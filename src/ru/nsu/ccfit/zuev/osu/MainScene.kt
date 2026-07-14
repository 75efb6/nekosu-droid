package ru.nsu.ccfit.zuev.osu

import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.edlplan.ui.fragment.ConfirmDialogFragment
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.discord.DiscordRPC
import com.reco1l.legacy.ui.MainMenu
import com.reco1l.legacy.ui.beatmapdownloader.BeatmapListing
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.entity.modifier.MoveXModifier
import org.anddev.andengine.entity.modifier.ParallelEntityModifier
import org.anddev.andengine.entity.modifier.RotationModifier
import org.anddev.andengine.entity.modifier.SequenceEntityModifier
import org.anddev.andengine.entity.particle.ParticleSystem
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter
import org.anddev.andengine.entity.particle.initializer.AccelerationInitializer
import org.anddev.andengine.entity.particle.initializer.RotationInitializer
import org.anddev.andengine.entity.particle.initializer.VelocityInitializer
import org.anddev.andengine.entity.modifier.AlphaModifier as EntityAlphaModifier
import org.anddev.andengine.entity.particle.modifier.AlphaModifier
import org.anddev.andengine.entity.particle.modifier.ExpireModifier
import org.anddev.andengine.entity.particle.modifier.ScaleModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.ColorBackground
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.HorizontalAlign
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.ease.EaseBounceOut
import org.anddev.andengine.util.modifier.ease.EaseCubicOut
import org.anddev.andengine.util.modifier.ease.EaseElasticOut
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut
import java.util.Arrays
import java.util.LinkedList
import java.util.Locale
import java.util.Random
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.opengles.GL10
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.game.SongProgressBar
import ru.nsu.ccfit.zuev.osu.game.TimingPoint
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring
import ru.nsu.ccfit.zuev.osu.online.SeasonalBackgroundManager
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import ru.nsu.ccfit.zuev.osuplus.R

class MainScene : IUpdateHandler {
    lateinit var progressBar: SongProgressBar
    var beatmapInfo: BeatmapInfo? = null
    private var context: android.content.Context? = null
    private var logo: Sprite? = null
    private var logoOverlay: Sprite? = null
    private var background: Sprite? = null
    private var lastBackground: Sprite? = null

    var scene: Scene? = null
        private set
    private var musicInfoText: ChangeableText? = null
    private val random = Random()
    private val spectrum = arrayOfNulls<Rectangle>(120)
    private val peakLevel = FloatArray(120)
    private val peakDownRate = FloatArray(120)
    private val peakAlpha = FloatArray(120)
    private var timingPoints: MutableList<TimingPoint>? = null
    private var currentTimingPoint: TimingPoint? = null
    private var lastTimingPoint: TimingPoint? = null
    private var firstTimingPoint: TimingPoint? = null

    private var particleBeginTime = 0
    private var particleEnabled = false
    private var isContinuousKiai = false

    private val particleSystem = arrayOfNulls<ParticleSystem>(2)

    private var musicStarted = false
    private var hitsound: BassSoundProvider? = null

    private var bpmLength = 1000.0
    private var lastBpmLength = 0.0
    private var offset = 0.0
    private var beatPassTime = 0f
    private var lastBeatPassTime = 0f
    private var doChange = false
    private var doStop = false
    private var lastHit: Long = 0
    var isOnExitAnim = false

    private var isMenuShowed = false
    private var doMenuShow = false
    private var showPassTime = 0f
    private var syncPassedTime = 0f
    private var menuBarX = 0f
    private var playY = 0f
    private var exitY = 0f

    private var menu: MainMenu? = null

    fun load(context: android.content.Context) {
        this.context = context
        Debug.i("Load: mainMenuLoaded()")
        scene = Scene()

        var tex: TextureRegion? = null

        if (SeasonalBackgroundManager.isSeasonalActive()) {
            SeasonalBackgroundManager.initAndLoadFirst()
            val seasonalFile = SeasonalBackgroundManager.getCurrentCacheFile()
            if (seasonalFile != null) {
                tex = ResourceManager.getInstance().loadBackground(seasonalFile.absolutePath)
            }
        }
        if (tex == null) {
            tex = ResourceManager.getInstance().getTexture("menu-background")
        }

        if (tex != null) {
            var height = tex.height.toFloat()
            height *= Config.getRES_WIDTH().toFloat() / tex.width.toFloat()
            val menuBg = Sprite(
                0f,
                (Config.getRES_HEIGHT() - height) / 2,
                Config.getRES_WIDTH().toFloat(),
                height, tex
            )
            scene!!.setBackground(SpriteBackground(menuBg))
        } else {
            scene!!.setBackground(ColorBackground(70f / 255f, 129f / 255f, 252f / 255f))
        }
        lastBackground = Sprite(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat(), ResourceManager.getInstance().getTexture("emptyavatar"))
        val logotex = ResourceManager.getInstance().getTexture("logo")!!
        logo = object : Sprite(
            Config.getRES_WIDTH() / 2f - logotex.width / 2f, Config.getRES_HEIGHT() / 2f - logotex.height / 2f, logotex
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    hitsound?.play()
                    Debug.i("logo down")
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    Debug.i("logo up")
                    Debug.i("doMenuShow $doMenuShow isMenuShowed $isMenuShowed showPassTime $showPassTime")
                    if (doMenuShow && isMenuShowed) {
                        showPassTime = 20000f
                    }
                    if (!doMenuShow && !isMenuShowed && logo!!.x == (Config.getRES_WIDTH() - logo!!.width) / 2) {
                        doMenuShow = true
                        showPassTime = 0f
                    }
                    Debug.i("doMenuShow $doMenuShow isMenuShowed $isMenuShowed showPassTime $showPassTime")
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        logoOverlay = Sprite(Config.getRES_WIDTH() / 2f - logotex.width / 2f, Config.getRES_HEIGHT() / 2f - logotex.height / 2f, logotex)
        logoOverlay!!.setScale(1.07f)
        logoOverlay!!.setAlpha(0.2f)

        menu = MainMenu(this)

        val author = object : Text(
            10f, 530f, ResourceManager.getInstance().getFont("font"),
            String.format(
                Locale.getDefault(),
                "nekosu!droid %s\nby Nekosu! Team\nosu!droid by osu!droid Team\nosu! is (c) peppy 2007-2026",
                BuildConfig.VERSION_NAME
            )
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    ConfirmDialogFragment().setMessage(R.string.dialog_visit_osu_website_message).showForResult(
                        { isAccepted ->
                            if (isAccepted) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://osu.ppy.sh"))
                                GlobalManager.getInstance().getMainActivity()?.startActivity(browserIntent)
                            }
                        }
                    )
                    return true
                }
                return false
            }
        }
        author.setPosition(10f, Config.getRES_HEIGHT().toFloat() - author.height - 10)

        val yasonline = object : Text(
            720f, 530f, ResourceManager.getInstance().getFont("font"),
            "            Global Ranking\n   Provided by Nekosu! Team"
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    ConfirmDialogFragment().setMessage(R.string.dialog_visit_osudroid_website_message).showForResult(
                        { isAccepted ->
                            if (isAccepted) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://" + OnlineManager.HOSTNAME))
                                GlobalManager.getInstance().getMainActivity()?.startActivity(browserIntent)
                            }
                        }
                    )
                    return true
                }
                return false
            }
        }
        yasonline.setPosition(Config.getRES_WIDTH().toFloat() - yasonline.width - 40, Config.getRES_HEIGHT().toFloat() - yasonline.height - 10)

        val music_prev = object : Sprite(
            (Config.getRES_WIDTH() - 50 * 6 + 35).toFloat(),
            47f, 40f, 40f, ResourceManager.getInstance().getTexture("music_prev")
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    doChange = true
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setColor(1f, 1f, 1f)
                    if (lastHit == 0L) {
                        lastHit = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastHit <= 1000 && !isOnExitAnim) {
                            return true
                        }
                    }
                    lastHit = System.currentTimeMillis()
                    musicControl(MusicOption.PREV)
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        val music_play = object : Sprite(
            (Config.getRES_WIDTH() - 50 * 5 + 35).toFloat(),
            47f, 40f, 40f, ResourceManager.getInstance().getTexture("music_play")
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setColor(1f, 1f, 1f)
                    musicControl(MusicOption.PLAY)
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        val music_pause = object : Sprite(
            (Config.getRES_WIDTH() - 50 * 4 + 35).toFloat(),
            47f, 40f, 40f, ResourceManager.getInstance().getTexture("music_pause")
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setColor(1f, 1f, 1f)
                    musicControl(MusicOption.PAUSE)
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        val music_stop = object : Sprite(
            (Config.getRES_WIDTH() - 50 * 3 + 35).toFloat(),
            47f, 40f, 40f, ResourceManager.getInstance().getTexture("music_stop")
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    doStop = true
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setColor(1f, 1f, 1f)
                    musicControl(MusicOption.STOP)
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        val music_next = object : Sprite(
            (Config.getRES_WIDTH() - 50 * 2 + 35).toFloat(),
            47f, 40f, 40f, ResourceManager.getInstance().getTexture("music_next")
        ) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    doChange = true
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setColor(1f, 1f, 1f)
                    if (lastHit == 0L) {
                        lastHit = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastHit <= 1000 && !isOnExitAnim) {
                            return true
                        }
                    }
                    lastHit = System.currentTimeMillis()
                    musicControl(MusicOption.NEXT)
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        musicInfoText = ChangeableText(0f, 0f, ResourceManager.getInstance().getFont("font"), "", HorizontalAlign.RIGHT, 35)

        val bgTopRect = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Utils.toRes(120).toFloat())
        bgTopRect.setColor(0.05f, 0.06f, 0.12f, 0.92f)

        val bgbottomRect = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(),
            Math.max(author.height, yasonline.height) + Utils.toRes(15).toFloat())
        bgbottomRect.setPosition(0f, Config.getRES_HEIGHT().toFloat() - bgbottomRect.height)
        bgbottomRect.setColor(0.05f, 0.06f, 0.12f, 0.92f)

        val topAccentLine = Rectangle(0f, Utils.toRes(120) - Utils.toRes(3).toFloat(), Config.getRES_WIDTH().toFloat(), Utils.toRes(3).toFloat())
        topAccentLine.setColor(0.90f, 0.24f, 0.55f, 1.0f)
        val bottomAccentLine = Rectangle(0f, bgbottomRect.y, Config.getRES_WIDTH().toFloat(), Utils.toRes(3).toFloat())
        bottomAccentLine.setColor(0.90f, 0.24f, 0.55f, 1.0f)

        for (i in 0 until 120) {
            val pX = Config.getRES_WIDTH() / 2f
            val pY = Config.getRES_HEIGHT() / 2f

            spectrum[i] = Rectangle(pX, pY, 260f, 10f)
            spectrum[i]!!.setRotationCenter(0f, 5f)
            spectrum[i]!!.setScaleCenter(0f, 5f)
            spectrum[i]!!.setRotation(-220 + i * 3f)
            spectrum[i]!!.setColor(0.90f, 0.24f, 0.55f)
            spectrum[i]!!.setAlpha(0.0f)

            scene!!.attachChild(spectrum[i])
        }

        LibraryManager.INSTANCE.loadLibraryCache(false)

        val starRegion = ResourceManager.getInstance().getTexture("star")!!

        particleSystem[0] = ParticleSystem(PointParticleEmitter(-40f, Config.getRES_HEIGHT() * 3f / 4), 32f, 48f, 128, starRegion)
        particleSystem[0]!!.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        particleSystem[0]!!.addParticleInitializer(VelocityInitializer(150f, 430f, -480f, -520f))
        particleSystem[0]!!.addParticleInitializer(AccelerationInitializer(10f, 30f))
        particleSystem[0]!!.addParticleInitializer(RotationInitializer(0.0f, 360.0f))
        particleSystem[0]!!.addParticleModifier(ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f))
        particleSystem[0]!!.addParticleModifier(AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f))
        particleSystem[0]!!.addParticleModifier(ExpireModifier(1.0f))
        particleSystem[0]!!.setParticlesSpawnEnabled(false)
        scene!!.attachChild(particleSystem[0])

        particleSystem[1] = ParticleSystem(PointParticleEmitter(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT() * 3f / 4), 32f, 48f, 128, starRegion)
        particleSystem[1]!!.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        particleSystem[1]!!.addParticleInitializer(VelocityInitializer(-150f, -430f, -480f, -520f))
        particleSystem[1]!!.addParticleInitializer(AccelerationInitializer(-10f, 30f))
        particleSystem[1]!!.addParticleInitializer(RotationInitializer(0.0f, 360.0f))
        particleSystem[1]!!.addParticleModifier(ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f))
        particleSystem[1]!!.addParticleModifier(AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f))
        particleSystem[1]!!.addParticleModifier(ExpireModifier(1.0f))
        particleSystem[1]!!.setParticlesSpawnEnabled(false)
        scene!!.attachChild(particleSystem[1])

        val beatmapDownloaderTex = ResourceManager.getInstance().getTexture("beatmap_downloader")!!
        val beatmapDownloader = object : Sprite(Config.getRES_WIDTH().toFloat() - beatmapDownloaderTex.width, (Config.getRES_HEIGHT() - beatmapDownloaderTex.height) / 2f, beatmapDownloaderTex) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    doStop = true
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setColor(1f, 1f, 1f)
                    BeatmapListing().show()
                    return true
                }
                return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY)
            }
        }

        menu!!.first.alpha = 0f
        menu!!.second.alpha = 0f
        menu!!.third.alpha = 0f

        logo!!.setPosition((Config.getRES_WIDTH() - logo!!.width) / 2, (Config.getRES_HEIGHT() - logo!!.height) / 2)
        logoOverlay!!.setPosition((Config.getRES_WIDTH() - logo!!.width) / 2, (Config.getRES_HEIGHT() - logo!!.height) / 2)

        menu!!.second.setScale(Config.getRES_WIDTH() / 1024f)
        menu!!.first.setScale(Config.getRES_WIDTH() / 1024f)
        menu!!.third.setScale(Config.getRES_WIDTH() / 1024f)

        menu!!.second.setPosition(logo!!.x + logo!!.width - Config.getRES_WIDTH() / 3f, (Config.getRES_HEIGHT() - menu!!.second.height) / 2)
        menu!!.first.setPosition(logo!!.x + logo!!.width - Config.getRES_WIDTH() / 3f, menu!!.second.y - menu!!.first.height - 40 * Config.getRES_WIDTH() / 1024f)
        menu!!.third.setPosition(logo!!.x + logo!!.width - Config.getRES_WIDTH() / 3f, menu!!.second.y + menu!!.second.height + 40 * Config.getRES_WIDTH() / 1024f)

        menuBarX = menu!!.first.x
        playY = menu!!.first.scaleY
        exitY = menu!!.third.scaleY

        scene!!.attachChild(lastBackground, 0)
        scene!!.attachChild(bgTopRect)
        scene!!.attachChild(bgbottomRect)
        scene!!.attachChild(topAccentLine)
        scene!!.attachChild(bottomAccentLine)
        scene!!.attachChild(author)
        scene!!.attachChild(yasonline)

        menu!!.attachButtons()

        scene!!.attachChild(logo)
        scene!!.attachChild(logoOverlay)
        scene!!.attachChild(musicInfoText)
        scene!!.attachChild(music_prev)
        scene!!.attachChild(music_play)
        scene!!.attachChild(music_pause)
        scene!!.attachChild(music_stop)
        scene!!.attachChild(music_next)
        scene!!.attachChild(beatmapDownloader)

        scene!!.registerTouchArea(logo)
        scene!!.registerTouchArea(author)
        scene!!.registerTouchArea(beatmapDownloader)
        scene!!.registerTouchArea(yasonline)
        scene!!.registerTouchArea(music_prev)
        scene!!.registerTouchArea(music_play)
        scene!!.registerTouchArea(music_pause)
        scene!!.registerTouchArea(music_stop)
        scene!!.registerTouchArea(music_next)
        scene!!.setTouchAreaBindingEnabled(true)

        progressBar = SongProgressBar(null, scene!!, 0f, 0f, PointF(Utils.toRes((Config.getRES_WIDTH() - 320).toFloat()), Utils.toRes(100f)))
        progressBar.setProgressRectColor(RGBAColor(0.9f, 0.9f, 0.9f, 0.8f))

        createOnlinePanel(scene!!)

        if (BuildConfig.DEBUG) {
            val devBuildText = Text(0f, 0f, ResourceManager.getInstance().getFont("font"), "DEVELOPMENT BUILD")
            devBuildText.setColor(1f, 1f, 0f)
            devBuildText.setPosition(
                (Config.getRES_WIDTH() - devBuildText.width) / 2f,
                Config.getRES_HEIGHT().toFloat() - devBuildText.height - 10
            )
            scene!!.attachChild(devBuildText)
        }

        scene!!.registerUpdateHandler(this)

        hitsound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false)

        if (SeasonalBackgroundManager.isSeasonalActive()) {
            SeasonalBackgroundManager.startPeriodicRefresh {
                val file = SeasonalBackgroundManager.getCurrentCacheFile()
                if (file != null) {
                    GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread {
                        try {
                            val seasonalTex = ResourceManager.getInstance().loadBackground(file.absolutePath)
                            if (seasonalTex != null) {
                                var height = seasonalTex.height.toFloat()
                                height *= Config.getRES_WIDTH().toFloat() / seasonalTex.width.toFloat()
                                val newBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, seasonalTex)
                                lastBackground!!.registerEntityModifier(EntityAlphaModifier(1.5f, 1f, 0f, object : IEntityModifier.IEntityModifierListener {
                                    override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                        scene!!.attachChild(newBg, 0)
                                    }
                                    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                        GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread { pItem.detachSelf() }
                                    }
                                }))
                                lastBackground = newBg
                            }
                        } catch (e: Exception) {
                            Debug.e(e.toString())
                        }
                    }
                }
            }
        }
    }

    private fun createOnlinePanel(scene: Scene) {
        Config.loadOnlineConfig(context!!)

        if (OnlineManager.getInstance().isStayOnline) {
            Debug.i("Stay online, creating panel")
            OnlineScoring.getInstance().createPanel()
            val panel: OnlinePanel? = OnlineScoring.getInstance().panel
            panel?.setPosition(5f, 5f)
            if (panel != null) {
                scene.attachChild(panel)
                scene.registerTouchArea(panel.rect)
            }
        }

        OnlineScoring.getInstance().login()
    }

    fun reloadOnlinePanel() {
        Execution.updateThread {
            scene!!.detachChild(OnlineScoring.getInstance().panel)
            createOnlinePanel(scene!!)
        }
    }

    fun musicControl(option: MusicOption) {
        if (GlobalManager.getInstance().songService == null || beatmapInfo == null) {
            return
        }
        when (option) {
            MusicOption.PREV -> {
                if (GlobalManager.getInstance().songService?.status == Status.PLAYING || GlobalManager.getInstance().songService?.status == Status.PAUSED) {
                    GlobalManager.getInstance().songService?.stop()
                }
                firstTimingPoint = null
                LibraryManager.INSTANCE.getPrevBeatmap()
                loadBeatmapInfo()
                loadTimingPoints(true)
                doChange = false
                doStop = false
            }
            MusicOption.PLAY -> {
                if (GlobalManager.getInstance().songService?.status == Status.PAUSED || GlobalManager.getInstance().songService?.status == Status.STOPPED) {
                    if (GlobalManager.getInstance().songService?.status == Status.STOPPED) {
                        loadTimingPoints(false)
                    GlobalManager.getInstance().songService?.preLoadPreview(beatmapInfo?.getMusic() ?: "")
                        if (firstTimingPoint != null) {
                            bpmLength = firstTimingPoint!!.beatLength * 1000f
                            if (lastTimingPoint != null) {
                                offset = (lastTimingPoint!!.time * 1000f) % bpmLength
                            }
                        }
                    }
                    if (GlobalManager.getInstance().songService?.status == Status.PAUSED) {
                        if (lastBpmLength > 0) {
                            bpmLength = lastBpmLength
                        }
                        if (lastTimingPoint != null) {
                            val position = GlobalManager.getInstance().songService!!.position
                            offset = (position - lastTimingPoint!!.time * 1000f) % bpmLength
                        }
                    }
                    Debug.i("BPM: ${60 / bpmLength * 1000} Offset: $offset")
                    GlobalManager.getInstance().songService?.play()
                    applyModMenuSpeed()
                    doStop = false
                }
            }
            MusicOption.PAUSE -> {
                if (GlobalManager.getInstance().songService?.status == Status.PLAYING) {
                    GlobalManager.getInstance().songService?.pause()
                    lastBpmLength = bpmLength
                    bpmLength = 1000.0
                }
            }
            MusicOption.STOP -> {
                if (GlobalManager.getInstance().songService?.status == Status.PLAYING || GlobalManager.getInstance().songService?.status == Status.PAUSED) {
                    GlobalManager.getInstance().songService?.stop()
                    lastBpmLength = bpmLength
                    bpmLength = 1000.0
                }
            }
            MusicOption.NEXT -> {
                if (GlobalManager.getInstance().songService?.status == Status.PLAYING || GlobalManager.getInstance().songService?.status == Status.PAUSED) {
                    GlobalManager.getInstance().songService?.stop()
                }
                LibraryManager.INSTANCE.getNextBeatmap()
                firstTimingPoint = null
                loadBeatmapInfo()
                loadTimingPoints(true)
                doChange = false
                doStop = false
            }
            MusicOption.SYNC -> {
                if (GlobalManager.getInstance().songService?.status == Status.PLAYING) {
                    if (lastTimingPoint != null) {
                        val position = GlobalManager.getInstance().songService!!.position
                        offset = (position - lastTimingPoint!!.time * 1000f) % bpmLength
                    }
                    Debug.i("BPM: ${60 / bpmLength * 1000} Offset: $offset")
                }
            }
        }
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        beatPassTime += pSecondsElapsed * 1000f
        if (isOnExitAnim) {
            for (specRectangle in spectrum) {
                specRectangle?.setWidth(0f)
                specRectangle?.setAlpha(0f)
            }
            return
        }

        if (GlobalManager.getInstance().songService == null || !musicStarted || GlobalManager.getInstance().songService?.status == Status.STOPPED) {
            bpmLength = 1000.0
            offset = 0.0
        }

        if (doMenuShow && !isMenuShowed) {
            logo!!.registerEntityModifier(MoveXModifier(0.3f, Config.getRES_WIDTH() / 2f - logo!!.width / 2, Config.getRES_WIDTH() / 3f - logo!!.width / 2, EaseExponentialOut.getInstance()))
            logoOverlay!!.registerEntityModifier(MoveXModifier(0.3f, Config.getRES_WIDTH() / 2f - logo!!.width / 2, Config.getRES_WIDTH() / 3f - logo!!.width / 2, EaseExponentialOut.getInstance()))
            for (rectangle in spectrum) {
                rectangle!!.registerEntityModifier(MoveXModifier(0.3f, Config.getRES_WIDTH() / 2f, Config.getRES_WIDTH() / 3f, EaseExponentialOut.getInstance()))
            }
            menu!!.first.registerEntityModifier(ParallelEntityModifier(
                MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                EntityAlphaModifier(0.5f, 0f, 0.9f, EaseCubicOut.getInstance())))
            menu!!.second.registerEntityModifier(ParallelEntityModifier(
                MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                EntityAlphaModifier(0.5f, 0f, 0.9f, EaseCubicOut.getInstance())))
            menu!!.third.registerEntityModifier(ParallelEntityModifier(
                MoveXModifier(0.5f, menuBarX - 100, menuBarX, EaseElasticOut.getInstance()),
                EntityAlphaModifier(0.5f, 0f, 0.9f, EaseCubicOut.getInstance())))
            scene!!.registerTouchArea(menu!!.first)
            scene!!.registerTouchArea(menu!!.second)
            scene!!.registerTouchArea(menu!!.third)
            isMenuShowed = true
        }

        if (doMenuShow) {
            if (showPassTime > 10000f) {
                menu!!.showFirstMenu()
                scene!!.unregisterTouchArea(menu!!.first)
                scene!!.unregisterTouchArea(menu!!.second)
                scene!!.unregisterTouchArea(menu!!.third)

                menu!!.first.registerEntityModifier(ParallelEntityModifier(
                    MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                    EntityAlphaModifier(1f, 0.9f, 0f, EaseExponentialOut.getInstance())))
                menu!!.second.registerEntityModifier(ParallelEntityModifier(
                    MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                    EntityAlphaModifier(1f, 0.9f, 0f, EaseExponentialOut.getInstance())))
                menu!!.third.registerEntityModifier(ParallelEntityModifier(
                    MoveXModifier(1f, menuBarX, menuBarX - 50, EaseExponentialOut.getInstance()),
                    EntityAlphaModifier(1f, 0.9f, 0f, EaseExponentialOut.getInstance())))

                logo!!.registerEntityModifier(MoveXModifier(1f, Config.getRES_WIDTH() / 3f - logo!!.width / 2, Config.getRES_WIDTH() / 2f - logo!!.width / 2, EaseBounceOut.getInstance()))
                logoOverlay!!.registerEntityModifier(MoveXModifier(1f, Config.getRES_WIDTH() / 3f - logo!!.width / 2, Config.getRES_WIDTH() / 2f - logo!!.width / 2, EaseBounceOut.getInstance()))

                for (rectangle in spectrum) {
                    rectangle!!.registerEntityModifier(MoveXModifier(1f, Config.getRES_WIDTH() / 3f, Config.getRES_WIDTH() / 2f, EaseBounceOut.getInstance()))
                }
                isMenuShowed = false
                doMenuShow = false
                showPassTime = 0f
            } else {
                showPassTime += pSecondsElapsed * 1000f
            }
        }

        if (beatPassTime - lastBeatPassTime >= bpmLength - offset) {
            lastBeatPassTime = beatPassTime
            offset = 0.0
            if (logo != null) {
                logo!!.registerEntityModifier(SequenceEntityModifier(
                    org.anddev.andengine.entity.modifier.ScaleModifier((bpmLength / 1000 * 0.9f).toFloat(), 1f, 1.07f),
                    org.anddev.andengine.entity.modifier.ScaleModifier((bpmLength / 1000 * 0.07f).toFloat(), 1.07f, 1f)))
            }
        }

        if (GlobalManager.getInstance().songService != null) {
            if (!musicStarted) {
                if (firstTimingPoint != null) {
                    bpmLength = firstTimingPoint!!.beatLength * 1000f
                } else {
                    return
                }
                progressBar.setStartTime(0f)
                GlobalManager.getInstance().songService?.play()
                applyModMenuSpeed()
                GlobalManager.getInstance().songService?.setVolume(Config.getBgmVolume())
                if (lastTimingPoint != null) {
                    offset = (lastTimingPoint!!.time * 1000f) % bpmLength
                }
                Debug.i("BPM: ${60 / bpmLength * 1000} Offset: $offset")
                musicStarted = true
            }

            if (GlobalManager.getInstance().songService?.status == Status.PLAYING) {
                val position = GlobalManager.getInstance().songService!!.position
                progressBar.setTime(GlobalManager.getInstance().songService!!.length.toFloat())
                progressBar.setPassedTime(position.toFloat())
                progressBar.update(pSecondsElapsed * 1000)

                if (currentTimingPoint != null && position > currentTimingPoint!!.time * 1000) {
                    if (!isContinuousKiai && currentTimingPoint!!.isKiai()) {
                        for (particleSpout in particleSystem) {
                            particleSpout?.setParticlesSpawnEnabled(true)
                        }
                        particleBeginTime = position
                        particleEnabled = true
                    }
                    isContinuousKiai = currentTimingPoint!!.isKiai()

                    if (timingPoints != null && timingPoints!!.size > 0) {
                        currentTimingPoint = timingPoints!!.removeAt(0)
                        if (!currentTimingPoint!!.wasInderited()) {
                            lastTimingPoint = currentTimingPoint
                            bpmLength = currentTimingPoint!!.beatLength * 1000
                            offset = (lastTimingPoint!!.time * 1000f) % bpmLength
                            Debug.i("BPM: ${60 / bpmLength * 1000} Offset: $offset")
                        }
                    } else {
                        currentTimingPoint = null
                    }
                }

                if (particleEnabled && (position - particleBeginTime > 2000)) {
                    for (particleSpout in particleSystem) {
                        particleSpout?.setParticlesSpawnEnabled(false)
                    }
                    particleEnabled = false
                }

                val windowSize = 240
                val spectrumWidth = 120
                val fft = GlobalManager.getInstance().songService?.spectrum ?: return
                var leftBound = 0
                for (i in 0 until spectrumWidth) {
                    var peak = 0f
                    var rightBound = Math.pow(2.0, i * 9.0 / (windowSize - 1)).toInt()
                    if (rightBound <= leftBound) rightBound = leftBound + 1
                    if (rightBound > 511) rightBound = 511

                    while (leftBound < rightBound) {
                        if (peak < fft[1 + leftBound]) peak = fft[1 + leftBound]
                        leftBound++
                    }

                    val initialAlpha = 0.4f
                    val gradient = 20f
                    val currPeakLevel = peak * 500

                    if (currPeakLevel > peakLevel[i]) {
                        peakLevel[i] = currPeakLevel
                        peakDownRate[i] = peakLevel[i] / gradient
                        peakAlpha[i] = initialAlpha
                    } else {
                        peakLevel[i] = Math.max(peakLevel[i] - peakDownRate[i], 0f)
                        peakAlpha[i] = Math.max(peakAlpha[i] - initialAlpha / gradient, 0f)
                    }

                    spectrum[i]!!.setWidth(250f + peakLevel[i])
                    spectrum[i]!!.setAlpha(peakAlpha[i])
                }
            } else {
                for (specRectangle in spectrum) {
                    specRectangle?.setWidth(0f)
                    specRectangle?.setAlpha(0f)
                }
                if (!doChange && !doStop && GlobalManager.getInstance().songService != null && GlobalManager.getInstance().songService!!.position >= GlobalManager.getInstance().songService!!.length) {
                    musicControl(MusicOption.NEXT)
                }
            }
        }
    }

    override fun reset() {}

    fun loadBeatmap() {
        LibraryManager.INSTANCE.shuffleLibrary()
        loadBeatmapInfo()
        loadTimingPoints(true)
    }

    fun loadBeatmapInfo() {
        if (LibraryManager.INSTANCE.getSizeOfBeatmaps() != 0) {
            beatmapInfo = LibraryManager.INSTANCE.getBeatmap()
            Log.w("MainMenuActivity", "Next song: ${beatmapInfo?.getMusic()}, Start at: ${beatmapInfo?.getPreviewTime()}")

            if (musicInfoText == null) {
                musicInfoText = ChangeableText(Utils.toRes((Config.getRES_WIDTH() - 500).toFloat()), Utils.toRes(3f),
                    ResourceManager.getInstance().getFont("font"), "None...", HorizontalAlign.RIGHT, 35)
            }
            if (beatmapInfo?.artistUnicode != null && beatmapInfo?.titleUnicode != null && !Config.isForceRomanized()) {
                musicInfoText!!.setText("${beatmapInfo?.artistUnicode} - ${beatmapInfo?.titleUnicode}", true)
            } else if (beatmapInfo?.artist != null && beatmapInfo?.title != null) {
                musicInfoText!!.setText("${beatmapInfo?.artist} - ${beatmapInfo?.title}", true)
            } else {
                musicInfoText!!.setText("Failure to load QAQ", true)
            }
            try {
                musicInfoText!!.setPosition(Utils.toRes((Config.getRES_WIDTH() - 500 + 470 - musicInfoText!!.width).toFloat()), musicInfoText!!.y)
            } catch (e: NullPointerException) {
                musicInfoText!!.setPosition(Utils.toRes((Config.getRES_WIDTH() - 500 + 470 - 200).toFloat()), 5f)
            }
        }
    }

    fun loadTimingPoints(reloadMusic: Boolean) {
        if (beatmapInfo == null) {
            return
        }

        for (particleSpout in particleSystem) {
            particleSpout?.setParticlesSpawnEnabled(false)
        }
        particleEnabled = false

        val trackInfos = beatmapInfo?.getTracks()
        if (trackInfos != null && trackInfos.size > 0) {
            val trackIndex = random.nextInt(trackInfos.size)
            val selectedTrack = trackInfos[trackIndex]
            GlobalManager.getInstance().selectedTrack = selectedTrack

            if (selectedTrack.background != null && !SeasonalBackgroundManager.isSeasonalActive()) {
                try {
                    val tex: TextureRegion? = if (Config.isSafeBeatmapBg())
                        ResourceManager.getInstance().getTexture("menu-background")
                    else
                        ResourceManager.getInstance().loadBackground(selectedTrack.background!!)

                    if (tex != null) {
                        var height = tex.height.toFloat()
                        height *= Config.getRES_WIDTH().toFloat() / tex.width.toFloat()
                        background = Sprite(0f,
                            (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, tex)
                        lastBackground!!.registerEntityModifier(EntityAlphaModifier(1.5f, 1f, 0f, object : IEntityModifier.IEntityModifierListener {
                            override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                scene!!.attachChild(background, 0)
                            }
                            override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread { pItem.detachSelf() }
                            }
                        }))
                        lastBackground = background
                    }
                } catch (e: Exception) {
                    Debug.e(e.toString())
                    lastBackground?.setAlpha(0f)
                }
            }

            if (reloadMusic) {
                if (GlobalManager.getInstance().songService != null) {
                    GlobalManager.getInstance().songService?.preLoadPreview(beatmapInfo?.getMusic() ?: "")
                    musicStarted = false
                } else {
                    Log.w("nullpoint", "GlobalManager.getInstance().getSongService() is null while reload music (MainScene.loadTimeingPoints)")
                }
            }

            Arrays.fill(peakLevel, 0f)
            Arrays.fill(peakDownRate, 1f)
            Arrays.fill(peakAlpha, 0f)

            val parser = BeatmapParser(selectedTrack.filename!!)
            val beatmapData = parser.parse(false)
            if (beatmapData != null) {
                timingPoints = LinkedList()
                for (s in beatmapData.rawTimingPoints) {
                    val tp = TimingPoint(s.split(",").toTypedArray(), currentTimingPoint)
                    timingPoints!!.add(tp)
                    if (!tp.wasInderited() || currentTimingPoint == null) {
                        currentTimingPoint = tp
                    }
                }
                firstTimingPoint = timingPoints!!.removeAt(0)
                currentTimingPoint = firstTimingPoint
                lastTimingPoint = currentTimingPoint
                bpmLength = firstTimingPoint!!.beatLength * 1000f
            }
        }
    }

    fun showExitDialog() {
        GlobalManager.getInstance().getMainActivity()?.runOnUiThread {
            ConfirmDialogFragment().setMessage(R.string.dialog_exit_message).showForResult(
                { isAccepted ->
                    if (isAccepted) {
                        exit()
                    }
                }
            )
        }
    }

    fun exit() {
        if (isOnExitAnim) {
            return
        }
        isOnExitAnim = true

        val wakeLock = GlobalManager.getInstance().getMainActivity()?.getWakeLock()
        if (wakeLock != null && wakeLock.isHeld) {
            wakeLock.release()
        }

        scene!!.unregisterTouchArea(menu!!.first)
        scene!!.unregisterTouchArea(menu!!.second)
        scene!!.unregisterTouchArea(menu!!.third)

        menu!!.first.alpha = 0f
        menu!!.second.alpha = 0f
        menu!!.third.alpha = 0f

        val exitsound: BassSoundProvider? = ResourceManager.getInstance().getSound("seeya")
        exitsound?.play()

        val bg = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        bg.setColor(0f, 0f, 0f, 1.0f)
        bg.registerEntityModifier(ModifierFactory.newAlphaModifier(3.0f, 0f, 1f))
        scene!!.attachChild(bg)
        logo!!.registerEntityModifier(ParallelEntityModifier(
            RotationModifier(3.0f, 0f, -15f),
            ModifierFactory.newScaleModifier(3.0f, 1f, 0.8f)
        ))
        logoOverlay!!.registerEntityModifier(ParallelEntityModifier(
            RotationModifier(3.0f, 0f, -15f),
            ModifierFactory.newScaleModifier(3.0f, 1f, 0.8f)
        ))

        GlobalManager.getInstance().songService?.stop()

        val taskPool = Executors.newScheduledThreadPool(1)
        taskPool.schedule(object : TimerTask() {
            override fun run() {
                GlobalManager.getInstance().getMainActivity()?.finish()
            }
        }, 3000, TimeUnit.MILLISECONDS)
    }

    fun restart() {
        val mActivity = GlobalManager.getInstance().getMainActivity() ?: return
        mActivity.runOnUiThread {
            ConfirmDialogFragment().setMessage(R.string.dialog_dither_confirm).showForResult(
                { isAccepted ->
                    if (isAccepted) {
                        val mIntent = Intent(mActivity, MainActivity::class.java)
                        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        mActivity.startActivity(mIntent)
                        DiscordRPC.updateForMainMenu()
                        System.exit(0)
                    }
                }
            )
        }
    }

    fun getBeatmapInfo(): BeatmapInfo? = beatmapInfo

    fun setBeatmap(info: Any?) {
        val beatmapInfoObj = when (info) {
            is BeatmapInfo -> info
            is TrackInfo -> info.beatmap
            else -> return
        }
        val playIndex = LibraryManager.INSTANCE.findBeatmap(beatmapInfoObj!!)
        Debug.i("index $playIndex")
        loadBeatmapInfo()
        loadTimingPoints(false)
        musicControl(MusicOption.SYNC)
    }

    fun watchReplay(replayFile: String?) {
        if (replayFile == null) return
        val replay = Replay()
        if (replay.loadInfo(replayFile)) {
            if (replay.replayVersion >= 3) {
                val scorescene = GlobalManager.getInstance().scoring
                val stat = replay.stat
                val track = LibraryManager.INSTANCE.findTrackByFileNameAndMD5(replay.mapFile, replay.md5)
                if (track != null) {
                    GlobalManager.getInstance().mainScene?.setBeatmap(track.beatmap)
                    GlobalManager.getInstance().songMenu?.select()
                    ResourceManager.getInstance().loadBackground(track.background ?: "")
                    GlobalManager.getInstance().songService?.preLoad(track.beatmap?.getMusic() ?: "")
                    GlobalManager.getInstance().songService?.play()
                    scorescene?.load(stat!!, null, GlobalManager.getInstance().songService, replayFile, null, track)
                    GlobalManager.getInstance().engine?.setScene(scorescene?.scene)
                }
            }
        }
    }

    private fun applyModMenuSpeed() {
        if (GlobalManager.getInstance().songService == null) return
        val speed = ModMenu.getInstance().speed
        val enableNC = ModMenu.getInstance().isEnableNCWhenSpeedChange || ModMenu.getInstance().mod.contains(GameMod.MOD_NIGHTCORE)
        GlobalManager.getInstance().songService?.applySpeed(speed, enableNC)
    }

    fun show() {
        GlobalManager.getInstance().songService?.setGaming(false)
        GlobalManager.getInstance().engine?.setScene(scene)
        DiscordRPC.updateForMainMenu()

        if (SeasonalBackgroundManager.isSeasonalActive()) {
            SeasonalBackgroundManager.startPeriodicRefresh {
                val file = SeasonalBackgroundManager.getCurrentCacheFile()
                if (file != null) {
                    GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread {
                        try {
                            val seasonalTex = ResourceManager.getInstance().loadBackground(file.absolutePath)
                            if (seasonalTex != null) {
                                var height = seasonalTex.height.toFloat()
                                height *= Config.getRES_WIDTH().toFloat() / seasonalTex.width.toFloat()
                                val newBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, seasonalTex)
                                lastBackground!!.registerEntityModifier(EntityAlphaModifier(1.5f, 1f, 0f, object : IEntityModifier.IEntityModifierListener {
                                    override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                        scene!!.attachChild(newBg, 0)
                                    }
                                    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                        GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread { pItem.detachSelf() }
                                    }
                                }))
                                lastBackground = newBg
                            }
                        } catch (e: Exception) {
                            Debug.e(e.toString())
                        }
                    }
                }
            }
        }

        if (SeasonalBackgroundManager.isSeasonalActive() && SeasonalBackgroundManager.shouldRefresh()) {
            Execution.async {
                SeasonalBackgroundManager.refreshSeasonalBg()
                val file = SeasonalBackgroundManager.getCurrentCacheFile()
                if (file != null) {
                    GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread {
                        try {
                            val seasonalTex = ResourceManager.getInstance().loadBackground(file.absolutePath)
                            if (seasonalTex != null) {
                                var height = seasonalTex.height.toFloat()
                                height *= Config.getRES_WIDTH().toFloat() / seasonalTex.width.toFloat()
                                val newBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, seasonalTex)
                                lastBackground!!.registerEntityModifier(EntityAlphaModifier(1.5f, 1f, 0f, object : IEntityModifier.IEntityModifierListener {
                                    override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                        scene!!.attachChild(newBg, 0)
                                    }
                                    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                        GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread { pItem.detachSelf() }
                                    }
                                }))
                                lastBackground = newBg
                            }
                        } catch (e: Exception) {
                            Debug.e(e.toString())
                        }
                    }
                }
            }
        }

        if (GlobalManager.getInstance().selectedTrack != null) {
            setBeatmap(GlobalManager.getInstance().selectedTrack?.beatmap)
        }
    }

    fun reloadSeasonalBackground() {
        if (!SeasonalBackgroundManager.isSeasonalActive()) {
            loadTimingPoints(false)
            return
        }
        Execution.async {
            SeasonalBackgroundManager.refreshSeasonalBg()
            val file = SeasonalBackgroundManager.getCurrentCacheFile()
            if (file != null) {
                GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread {
                    try {
                        val seasonalTex = ResourceManager.getInstance().loadBackground(file.absolutePath)
                        if (seasonalTex != null) {
                            var height = seasonalTex.height.toFloat()
                            height *= Config.getRES_WIDTH().toFloat() / seasonalTex.width.toFloat()
                            val newBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, seasonalTex)
                            lastBackground!!.registerEntityModifier(EntityAlphaModifier(1.5f, 1f, 0f, object : IEntityModifier.IEntityModifierListener {
                                override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                    scene!!.attachChild(newBg, 0)
                                }
                                override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                    GlobalManager.getInstance().getMainActivity()?.runOnUpdateThread { pItem.detachSelf() }
                                }
                            }))
                            lastBackground = newBg
                        }
                    } catch (e: Exception) {
                        Debug.e(e.toString())
                    }
                }
            }
        }
    }

    enum class MusicOption { PREV, PLAY, PAUSE, STOP, NEXT, SYNC }
}
