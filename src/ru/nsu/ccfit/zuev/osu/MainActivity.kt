package ru.nsu.ccfit.zuev.osu

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.preference.PreferenceManager
import com.edlplan.ui.ActivityOverlay
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.reco1l.api.ibancho.LobbyAPI
import com.reco1l.framework.lang.Execution
import android.text.InputType
import com.reco1l.legacy.AccessibilityDetector
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.UpdateManager
import com.reco1l.legacy.discord.DiscordRPC
import com.reco1l.legacy.ui.StyledInputDialog
import com.reco1l.legacy.ui.StyledKeybindDialog
import com.reco1l.legacy.ui.multiplayer.LobbyScene
import com.reco1l.legacy.ui.multiplayer.RoomScene
import net.lingala.zip4j.ZipFile
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.SmoothCamera
import org.anddev.andengine.engine.options.EngineOptions
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.extension.input.touch.controller.MultiTouch
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.view.RenderSurfaceView
import org.anddev.andengine.sensor.accelerometer.AccelerometerData
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener
import org.anddev.andengine.ui.activity.BaseGameActivity
import org.anddev.andengine.util.Debug
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import ru.nsu.ccfit.zuev.audio.BassAudioPlayer
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.osu.game.SpritePool
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.InputManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osu.menu.SplashScene
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import ru.nsu.ccfit.zuev.osuplus.R

class MainActivity : BaseGameActivity(), IAccelerometerListener {
    private var wakeLock: PowerManager.WakeLock? = null
    private var beatmapToAdd: String? = null
    private var saveServiceObject: SaveServiceObject? = null
    private val handler = Handler(Looper.getMainLooper())
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var willReplay = false
    private var roomInviteLink: Uri? = null
    var connection: ServiceConnection? = null
        private set

    override fun onLoadEngine(): Engine? {
        if (!checkPermissions()) {
            return null
        }
        analytics = FirebaseAnalytics.getInstance(this)
        crashlytics = FirebaseCrashlytics.getInstance()
        Config.loadConfig(this)
        initialGameDirectory()
        StringTable.setContext(this)
        ToastLogger.init(this)
        InputManager.setContext(this)
        crashlytics!!.setUserId(Config.getOnlineDeviceID())

        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val screenInches = Math.sqrt(Math.pow(dm.heightPixels.toDouble(), 2.0) + Math.pow(dm.widthPixels.toDouble(), 2.0)) / (dm.density * 160.0f)
        Debug.i("screen inches: $screenInches")
        Config.setScaleMultiplier(((11 - 5.2450170716245195) / 5).toFloat())

        val manager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "osudroid:osu")

        val mCamera = SmoothCamera(0f, 0f, Config.getRES_WIDTH().toFloat(),
            Config.getRES_HEIGHT().toFloat(), 0f, 1800f, 1f)
        val opt = EngineOptions(true,
            null, RatioResolutionPolicy(
                Config.getRES_WIDTH().toFloat() / Config.getRES_HEIGHT().toFloat()
            ),
            mCamera)
        opt.setNeedsMusic(true)
        opt.setNeedsSound(true)
        opt.renderOptions.disableExtensionVertexBufferObjects()
        opt.touchOptions.enableRunOnUpdateThread()
        opt.updateThreadPriority = android.os.Process.THREAD_PRIORITY_URGENT_AUDIO
        val engine = Engine(opt)
        try {
            if (MultiTouch.isSupported(this)) {
                engine.touchController = MultiTouchController()
            } else {
                ToastLogger.showText(
                    StringTable.get(R.string.message_error_multitouch),
                    false
                )
            }
        } catch (e: MultiTouchException) {
            ToastLogger.showText(
                StringTable.get(R.string.message_error_multitouch),
                false
            )
        }
        GlobalManager.getInstance().camera = mCamera
        GlobalManager.getInstance().engine = engine
        GlobalManager.getInstance().setMainActivity(this)
        return GlobalManager.getInstance().engine
    }

    private fun initialGameDirectory() {
        var dir = File(Config.getBeatmapPath())
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Config.setBeatmapPath(Config.getCorePath() + "Songs/")
                dir = File(Config.getBeatmapPath())
                if (!(dir.exists() || dir.mkdirs())) {
                    ToastLogger.showText(StringTable.format(
                        R.string.message_error_createdir, dir.path
                    ), true)
                } else {
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = prefs.edit()
                    editor.putString("directory", dir.path)
                    editor.commit()
                }
            }
            val nomedia = File(dir.parentFile, ".nomedia")
            try {
                nomedia.createNewFile()
            } catch (e: IOException) {
                Debug.e("LibraryManager: ${e.message}", e)
            }
        }

        val skinDir = File(Config.getCorePath() + "/Skin")
        if (!skinDir.exists()) {
            skinDir.mkdirs()
        }
    }

    private fun initPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (prefs.getString("playername", "") == "") {
            val editor = prefs.edit()
            editor.putString("playername", "Guest")
            editor.commit()

            Execution.mainThread {
                StyledInputDialog.show(
                    this,
                    StringTable.get(R.string.dialog_playername_title),
                    "Guest",
                    InputType.TYPE_CLASS_TEXT
                ) { value ->
                    editor.putString("playername", value)
                    editor.commit()
                }
            }
        }

        if (!prefs.getBoolean("qualitySet", false)) {
            val editor = prefs.edit()
            editor.putBoolean("qualitySet", true)
            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(dm)

            if (dm.densityDpi > DisplayMetrics.DENSITY_MEDIUM) {
                editor.putBoolean("lowtextures", false)
            } else {
                editor.putBoolean("lowtextures", false)
            }
            editor.commit()
        }

        if (!prefs.getBoolean("onlineSet", false)) {
            val editor = prefs.edit()
            editor.putBoolean("onlineSet", true)
            editor.commit()
        }
    }

    override fun onLoadResources() {
        ResourceManager.getInstance().Init(mEngine, this)
        ResourceManager.getInstance().loadHighQualityAsset("welcome", "gfx/welcome.png")
        ResourceManager.getInstance().loadHighQualityAsset("loading_start", "gfx/loading.png")
        ResourceManager.getInstance().loadSound("welcome", "sfx/welcome.ogg", false)
        ResourceManager.getInstance().loadSound("welcome_piano", "sfx/welcome_piano.ogg", false)

        engine.setScene(SplashScene.INSTANCE.getScene())

        ResourceManager.getInstance().loadHighQualityAsset("logo", "logo.png")
        ResourceManager.getInstance().loadHighQualityAsset("play", "play.png")
        ResourceManager.getInstance().loadHighQualityAsset("solo", "solo.png")
        ResourceManager.getInstance().loadHighQualityAsset("multi", "multi.png")
        ResourceManager.getInstance().loadHighQualityAsset("back", "back.png")
        ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png")
        ResourceManager.getInstance().loadHighQualityAsset("beatmap_downloader", "beatmap_downloader.png")
        ResourceManager.getInstance().loadHighQualityAsset("options", "options.png")
        ResourceManager.getInstance().loadHighQualityAsset("editor", "editor.png")
        ResourceManager.getInstance().loadHighQualityAsset("offline-avatar", "offline-avatar.png")
        ResourceManager.getInstance().loadHighQualityAsset("star", "gfx/star.png")
        ResourceManager.getInstance().loadHighQualityAsset("chat", "chat.png")
        ResourceManager.getInstance().loadHighQualityAsset("team_vs", "team_vs.png")
        ResourceManager.getInstance().loadHighQualityAsset("head_head", "head_head.png")
        ResourceManager.getInstance().loadHighQualityAsset("crown", "crown.png")
        ResourceManager.getInstance().loadHighQualityAsset("missing", "missing.png")
        ResourceManager.getInstance().loadHighQualityAsset("lock", "lock.png")
        ResourceManager.getInstance().loadHighQualityAsset("music_play", "music_play.png")
        ResourceManager.getInstance().loadHighQualityAsset("music_pause", "music_pause.png")
        ResourceManager.getInstance().loadHighQualityAsset("music_stop", "music_stop.png")
        ResourceManager.getInstance().loadHighQualityAsset("music_next", "music_next.png")
        ResourceManager.getInstance().loadHighQualityAsset("music_prev", "music_prev.png")
        ResourceManager.getInstance().loadHighQualityAsset("songselect-top", "songselect-top.png")

        var bg: File
        if ((File(Config.getSkinPath() + "menu-background.png").also { bg = it }.exists())
            || (File(Config.getSkinPath() + "menu-background.jpg").also { bg = it }.exists())
        ) {
            ResourceManager.getInstance().loadHighQualityFile("menu-background", bg)
        }
        ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE)
        ResourceManager.getInstance().loadFont("smallFont", null, 21, Color.WHITE)
        ResourceManager.getInstance().loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE)
        ResourceManager.getInstance().loadSound("heartbeat", "sfx/heartbeat.ogg", false)
    }

    override fun onLoadScene(): Scene = SplashScene.INSTANCE.getScene()

    override fun onLoadComplete() {
        LobbyScene.init()
        RoomScene.init()

        Execution.async {
            BassAudioPlayer.initDevice()
            GlobalManager.getInstance().init()
            analytics!!.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
            GlobalManager.getInstance().loadingProgress = 50

            val skinsFuture = CompletableFuture.runAsync { checkNewSkins() }
            val beatmapsFuture = CompletableFuture.runAsync { checkNewBeatmaps() }

            skinsFuture.join()
            Config.loadSkins()

            beatmapsFuture.join()

            if (!LibraryManager.INSTANCE.loadLibraryCache(true)) {
                LibraryManager.INSTANCE.scanLibrary()
            }

            SplashScene.INSTANCE.playWelcomeAnimation()

            Execution.delayed(2500) {
                UpdateManager.onActivityStart()
                GlobalManager.getInstance().info = ""
                GlobalManager.getInstance().loadingProgress = 100
                ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE)
                GlobalManager.getInstance().engine?.setScene(GlobalManager.getInstance().mainScene?.scene)
                GlobalManager.getInstance().mainScene?.loadBeatmap()
                DiscordRPC.restore(this@MainActivity)
                DiscordRPC.updateForMainMenu()
                initPreferences()
                availableInternalMemory()

                scheduledExecutor.scheduleAtFixedRate({
                    AccessibilityDetector.check(this@MainActivity)
                    BeatmapDifficultyCalculator.invalidateExpiredCache()
                }, 0, 1000, TimeUnit.MILLISECONDS)

                if (roomInviteLink != null) {
                    LobbyScene.connectFromLink(roomInviteLink!!)
                    return@delayed
                }

                if (willReplay) {
                    GlobalManager.getInstance().mainScene?.watchReplay(beatmapToAdd)
                    willReplay = false
                }
            }
        }
    }

    private fun availableInternalMemory() {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.HALF_EVEN

        val minMem = 1073741824.0 //1 GiB = 1073741824 bytes
        val internal = Environment.getDataDirectory()
        val stat = StatFs(internal.path)
        val availableMemory = stat.availableBytes.toDouble()
        val toastMessage = String.format(StringTable.get(R.string.message_low_storage_space), df.format(availableMemory / minMem))
        if (availableMemory < 0.5 * minMem) {
            Execution.mainThread { Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show() }
        }
        Debug.i("Free Space: ${df.format(availableMemory / minMem)}")
    }

    @SuppressLint("ResourceType")
    override fun onSetContentView() {
        mRenderSurfaceView = RenderSurfaceView(this)
        if (Config.isUseDither()) {
            mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0)
            mRenderSurfaceView.holder.setFormat(PixelFormat.RGBA_8888)
        } else {
            mRenderSurfaceView.setEGLConfigChooser(true)
        }
        mRenderSurfaceView.setRenderer(mEngine)

        val layout = RelativeLayout(this)
        layout.setBackgroundColor(Color.argb(255, 0, 0, 0))
        layout.addView(
            mRenderSurfaceView,
            object : RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ) {
                init {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
            })

        val frameLayout = FrameLayout(this)
        frameLayout.id = 0x28371
        layout.addView(frameLayout, RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val c = View(this)
        c.setBackgroundColor(Color.argb(0, 0, 0, 0))
        layout.addView(c, RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setContentView(
            layout,
            object : FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) {
                init {
                    gravity = Gravity.CENTER
                }
            })

        ActivityOverlay.initial(this, frameLayout.id)
    }

    fun checkNewBeatmaps() {
        GlobalManager.getInstance().info = "Checking for new maps..."
        val mainDir = File(Config.getCorePath())
        if (beatmapToAdd != null) {
            val file = File(beatmapToAdd!!)
            if (file.name.lowercase().endsWith(".osz")) {
                ToastLogger.showText(
                    StringTable.get(R.string.message_lib_importing),
                    false
                )
                FileUtils.extractZip(beatmapToAdd!!, Config.getBeatmapPath())
                LibraryManager.INSTANCE.saveToCache()
            } else if (file.name.endsWith(".odr")) {
                willReplay = true
            }
        } else if (mainDir.exists() && mainDir.isDirectory) {
            var filelist = FileUtils.listFiles(mainDir, ".osz") ?: emptyArray()
            val beatmaps = ArrayList<String>()
            for (file in filelist) {
                try {
                    val zip = ZipFile(file)
                    if (zip.isValidZipFile) {
                        beatmaps.add(file.path)
                    }
                } catch (ignored: IOException) {
                }
            }

            val beatmapDir = File(Config.getBeatmapPath())
            if (beatmapDir.exists() && beatmapDir.isDirectory) {
                filelist = FileUtils.listFiles(beatmapDir, ".osz") ?: emptyArray()
                for (file in filelist) {
                    try {
                        val zip = ZipFile(file)
                        if (zip.isValidZipFile) {
                            beatmaps.add(file.path)
                        }
                    } catch (ignored: IOException) {
                    }
                }
            }

            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (Config.isSCAN_DOWNLOAD() && downloadDir.exists() && downloadDir.isDirectory) {
                filelist = FileUtils.listFiles(downloadDir, ".osz") ?: emptyArray()
                for (file in filelist) {
                    try {
                        val zip = ZipFile(file)
                        if (zip.isValidZipFile) {
                            beatmaps.add(file.path)
                        }
                    } catch (ignored: IOException) {
                    }
                }
            }

            if (beatmaps.isNotEmpty()) {
                ToastLogger.showText(StringTable.format(
                    R.string.message_lib_importing_several,
                    beatmaps.size
                ), false)
                for (beatmap in beatmaps) {
                    FileUtils.extractZip(beatmap, Config.getBeatmapPath())
                }
                LibraryManager.INSTANCE.saveToCache()
            }
        }
    }

    fun checkNewSkins() {
        GlobalManager.getInstance().info = "Checking new skins..."
        val skins = ArrayList<String>()

        val skinDir = File(Config.getSkinTopPath())
        if (skinDir.exists() && skinDir.isDirectory) {
            val files = FileUtils.listFiles(skinDir, ".osk") ?: emptyArray()
            for (file in files) {
                try {
                    val zip = ZipFile(file)
                    if (zip.isValidZipFile) {
                        skins.add(file.path)
                    }
                } catch (ignored: IOException) {
                }
            }
        }

        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (Config.isSCAN_DOWNLOAD() && downloadDir.exists() && downloadDir.isDirectory) {
            val files = FileUtils.listFiles(downloadDir, ".osk") ?: emptyArray()
            for (file in files) {
                try {
                    val zip = ZipFile(file)
                    if (zip.isValidZipFile) {
                        skins.add(file.path)
                    }
                } catch (ignored: IOException) {
                }
            }
        }

        if (skins.isNotEmpty()) {
            ToastLogger.showText(StringTable.format(
                R.string.message_skin_importing_several,
                skins.size
            ), false)

            for (skin in skins) {
                if (FileUtils.extractZip(skin, Config.getSkinTopPath())) {
                    val folderName = skin.substring(0, skin.length - 4)
                    ToastLogger.showText(
                        StringTable.format(R.string.message_lib_imported, folderName),
                        true
                    )
                    Config.addSkin(folderName.substring(folderName.lastIndexOf("/") + 1), skin)
                }
            }
        }
    }

    fun getHandler(): Handler = handler

    fun getAnalytics(): FirebaseAnalytics? = analytics

    fun getWakeLock(): PowerManager.WakeLock? = wakeLock

    override fun onCreate(pSavedInstanceState: Bundle?) {
        super.onCreate(pSavedInstanceState)

        try {
            versionName = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).versionName
        } catch (ignored: Exception) {
        }

        if (mEngine == null) {
            return
        }

        if (BuildConfig.DEBUG) {
            try {
                val d = File(Environment.getExternalStorageDirectory(), "osu!droid/Log")
                if (!d.exists()) d.mkdirs()
                val f = File(d, "rawlog.txt")
                if (!f.exists()) f.createNewFile()
                Runtime.getRuntime().exec("logcat -f ${f.absolutePath}")
            } catch (ignored: IOException) {
            }
        }
        onBeginBindService()
    }

    fun onBeginBindService() {
        if (connection == null && songService == null) {
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    songService = (service as SongService.ReturnBindObject).getObject()
                    saveServiceObject = application as SaveServiceObject
                    saveServiceObject!!.setSongService(songService)
                    GlobalManager.getInstance().songService = songService
                }

                override fun onServiceDisconnected(name: ComponentName) {
                }
            }

            val conn = connection
            if (conn != null) {
                bindService(Intent(this@MainActivity, SongService::class.java), conn, BIND_AUTO_CREATE)
            }
        }
        GlobalManager.getInstance().songService = songService
        GlobalManager.getInstance().saveServiceObject = saveServiceObject
    }

    override fun onStart() {
        if (intent.action != null && intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null) {
                if (data.toString().startsWith(LobbyAPI.INVITE_HOST)) {
                    roomInviteLink = data
                }
                if (ContentResolver.SCHEME_FILE == intent.data?.scheme)
                    beatmapToAdd = intent.data?.path
            }
        }
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        DiscordRPC.onActivityResume()
        if (mEngine == null) {
            return
        }
        activityVisible = true
        if (GlobalManager.getInstance().engine != null && GlobalManager.getInstance().gameScene != null
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene
        ) {
            GlobalManager.getInstance().engine?.textureManager?.reloadTextures()
        }
        if (GlobalManager.getInstance().mainScene != null) {
            if (songService != null && Build.VERSION.SDK_INT > 10) {
                if (songService!!.hideNotification()) {
                    if (wakeLock != null && wakeLock!!.isHeld) wakeLock!!.release()
                    GlobalManager.getInstance().mainScene?.loadBeatmapInfo()
                    GlobalManager.getInstance().mainScene?.loadTimingPoints(false)
                    GlobalManager.getInstance().mainScene?.progressBar?.setTime(songService!!.length.toFloat())
                    GlobalManager.getInstance().mainScene?.progressBar?.setPassedTime(songService!!.position.toFloat())
                    GlobalManager.getInstance().mainScene?.musicControl(MainScene.MusicOption.SYNC)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        activityVisible = false
        if (mEngine == null) {
            return
        }
        if (GlobalManager.getInstance().engine != null && GlobalManager.getInstance().gameScene != null
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene
        ) {
            SpritePool.getInstance().purge()

            if (Multiplayer.isMultiplayer) {
                ToastLogger.showText("You've left the match.", true)
                GlobalManager.getInstance().gameScene?.quit()
                Multiplayer.log("Player left the match.")
            } else GlobalManager.getInstance().gameScene?.pause()
        }
        if (GlobalManager.getInstance().mainScene != null) {
            val beatmapInfo = GlobalManager.getInstance().mainScene?.beatmapInfo
            if (songService != null && beatmapInfo != null && !songService!!.isGaming) {
                songService!!.showNotification()

                if (wakeLock == null) {
                    val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "osudroid:MainActivity")
                }
                wakeLock?.acquire()
            } else {
                songService?.pause()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        activityVisible = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (mEngine == null) {
            return
        }

        if (engine != null && !hasFocus) {
            if (GlobalManager.getInstance().gameScene != null
                && engine.scene == GlobalManager.getInstance().gameScene?.scene
                && GlobalManager.getInstance().gameScene != null
            ) {
                if (!GlobalManager.getInstance().gameScene!!.isPaused() && !Multiplayer.isMultiplayer)
                    GlobalManager.getInstance().gameScene?.pause()
            }

            if (Multiplayer.isConnected
                && (engine.scene == RoomScene
                        || engine.scene == GlobalManager.getInstance().songMenu?.scene)
            ) {
                Execution.asyncIgnoreExceptions { RoomScene.invalidateStatus() }
            }
        }

        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Config.isHideNaviBar()) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onAccelerometerChanged(arg0: AccelerometerData) {
        if (mEngine == null) {
            return
        }
        if (GlobalManager.getInstance().camera?.rotation == 0f && arg0.y < -5) {
            GlobalManager.getInstance().camera?.setRotation(180f)
        } else if (GlobalManager.getInstance().camera?.rotation == 180f && arg0.y > 5) {
            GlobalManager.getInstance().camera?.setRotation(0f)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (mEngine == null) {
            return false
        }

        if (AccessibilityDetector.isIllegalServiceDetected)
            return false

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event)
        }
        if (GlobalManager.getInstance().engine == null) {
            return super.onKeyDown(keyCode, event)
        }

        if (event.action == TouchEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && ActivityOverlay.onBackPress()) {
            return true
        }

        if (KeyboardConfig.isBinding() && event.action == KeyEvent.ACTION_DOWN) {
            val overlay = ActivityOverlay.getTopOverlay()
            if (overlay is StyledKeybindDialog) {
                overlay.onKeyPress(keyCode, StyledKeybindDialog.getKeyName(keyCode))
                return true
            }
        }

        if (GlobalManager.getInstance().gameScene != null
            && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene
        ) {
            if (GlobalManager.getInstance().gameScene!!.isPaused()) {
                GlobalManager.getInstance().gameScene?.resume()
            } else {
                GlobalManager.getInstance().gameScene?.pause()
            }
            return true
        }

        if (GlobalManager.getInstance().gameScene != null
            && GlobalManager.getInstance().engine != null
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene
            && !GlobalManager.getInstance().gameScene!!.isPaused()
            && KeyboardConfig.isEnabled()
            && keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU
        ) {
            val kc = keyCode
            GlobalManager.getInstance().engine?.runOnUpdateThread {
                GlobalManager.getInstance().gameScene?.onKeyboardDown(kc)
            }
            return true
        }

        if (GlobalManager.getInstance().scoring != null && keyCode == KeyEvent.KEYCODE_BACK
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().scoring?.scene
        ) {
            GlobalManager.getInstance().scoring?.back()
            return true
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER)
            && GlobalManager.getInstance().engine != null
            && GlobalManager.getInstance().songMenu != null
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().songMenu?.scene
            && GlobalManager.getInstance().songMenu?.scene?.hasChildScene() == true
        ) {
            if (GlobalManager.getInstance().songMenu?.scene?.childScene ==
                GlobalManager.getInstance().songMenu?.filterMenu?.scene
            ) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    InputManager.getInstance().toggleKeyboard()
                }
                GlobalManager.getInstance().songMenu?.filterMenu?.hideMenu()
            }

            if (GlobalManager.getInstance().songMenu?.scene?.childScene == ModMenu.getInstance().scene) {
                ModMenu.getInstance().onBackPress()
            }

            return true
        }
        if (GlobalManager.getInstance().songMenu != null && GlobalManager.getInstance().engine != null
            && keyCode == KeyEvent.KEYCODE_MENU
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().songMenu?.scene
            && GlobalManager.getInstance().songMenu?.scene?.hasChildScene() == false
        ) {
            GlobalManager.getInstance().songMenu?.stopScroll(0f)
            GlobalManager.getInstance().songMenu?.showPropertiesMenu(null)
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (GlobalManager.getInstance().engine != null && GlobalManager.getInstance().songMenu != null &&
                GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().songMenu?.scene
            ) {
                GlobalManager.getInstance().songMenu?.back()
            } else {
                if (GlobalManager.getInstance().engine?.scene is LoadingScreen.LoadingScene) {
                    return true
                }

                if (GlobalManager.getInstance().editorScene != null &&
                    GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().editorScene?.getScene()
                ) {
                    GlobalManager.getInstance().editorScene?.back()
                    return true
                }

                if (Multiplayer.isMultiplayer) {
                    if (GlobalManager.getInstance().engine?.scene == LobbyScene) {
                        LobbyScene.back()
                        return true
                    }

                    if (GlobalManager.getInstance().engine?.scene == RoomScene) {
                        if (RoomScene.hasChildScene() && RoomScene.childScene == ModMenu.getInstance().scene) {
                            ModMenu.getInstance().onBackPress()
                            return true
                        }
                        runOnUiThread { RoomScene.leaveDialog.show() }
                        return true
                    }
                }

                GlobalManager.getInstance().mainScene?.showExitDialog()
            }
            return true
        }

        if (InputManager.getInstance().isStarted()) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                InputManager.getInstance().pop()
            } else if (keyCode != KeyEvent.KEYCODE_ENTER) {
                val c = event.unicodeChar.toChar()
                if (c != 0.toChar()) {
                    InputManager.getInstance().append(c)
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (mEngine == null) {
            return false
        }

        if (event.action != KeyEvent.ACTION_UP) {
            return super.onKeyUp(keyCode, event)
        }

        if (GlobalManager.getInstance().gameScene != null
            && GlobalManager.getInstance().engine != null
            && GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene
            && !GlobalManager.getInstance().gameScene!!.isPaused()
            && KeyboardConfig.isEnabled()
            && keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU
        ) {
            val kc = keyCode
            GlobalManager.getInstance().engine?.runOnUpdateThread {
                GlobalManager.getInstance().gameScene?.onKeyboardUp(kc)
            }
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    fun forcedExit() {
        if (GlobalManager.getInstance().engine?.scene == GlobalManager.getInstance().gameScene?.scene) {
            GlobalManager.getInstance().gameScene?.quit()
        }
        GlobalManager.getInstance().engine?.setScene(GlobalManager.getInstance().mainScene?.scene)
        GlobalManager.getInstance().mainScene?.exit()
    }

    fun getVersionCode(): Long {
        var versionCode: Long = 0
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Debug.e("PackageManager: ${e.message}", e)
        }
        return versionCode
    }

    fun getRefreshRate(): Float {
        return (getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
            .defaultDisplay
            .refreshRate
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            return true
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PermissionChecker.PERMISSION_GRANTED
        ) {
            return true
        } else {
            val grantPermission = Intent(this, PermissionActivity::class.java)
            startActivity(grantPermission)
            overridePendingTransition(R.anim.fast_activity_swap, R.anim.fast_activity_swap)
            finish()
            return false
        }
    }

    override fun onDestroy() {
        NotificationManagerCompat.from(applicationContext).cancelAll()
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        var versionName: String? = null

        @JvmStatic
        var songService: SongService? = null

        private var activityVisible = true
        private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

        @JvmStatic
        fun isActivityVisible(): Boolean = activityVisible
    }
}
