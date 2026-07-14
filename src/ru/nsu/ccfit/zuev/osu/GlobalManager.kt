package ru.nsu.ccfit.zuev.osu

import android.util.DisplayMetrics
import java.lang.ref.WeakReference
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.osu.editor.EditorScene
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.game.GlobalFPSOverlay
import ru.nsu.ccfit.zuev.osu.menu.SongMenu
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene

class GlobalManager private constructor() {
    var engine: Engine? = null
    var camera: Camera? = null
    var gameScene: GameScene? = null
    var mainScene: MainScene? = null
    var scoring: ScoringScene? = null
    var songMenu: SongMenu? = null
    var editorScene: EditorScene? = null
    private var mainActivityRef: WeakReference<MainActivity>? = null
    var loadingProgress: Int = 0
    var info: String? = null
    var songService: SongService? = null
    var selectedTrack: TrackInfo? = null
    var saveServiceObject: SaveServiceObject? = null
    var skinNow: String? = null

    fun init() {
        val activity = getMainActivity() ?: return
        saveServiceObject = activity.application as SaveServiceObject
        songService = saveServiceObject?.getSongService()
        setLoadingProgress(10)
        mainScene = MainScene()
        mainScene?.load(activity)
        info = "Loading skin..."
        skinNow = Config.getSkinPath()
        ResourceManager.getInstance().loadSkin(skinNow!!)
        ScoreLibrary.getInstance().load(activity)
        setLoadingProgress(20)
        PropertiesLibrary.instance.load(activity)
        setLoadingProgress(30)
        gameScene = GameScene(engine!!)
        songMenu = SongMenu()
        setLoadingProgress(40)
        songMenu?.init(activity, engine!!, gameScene!!)
        songMenu?.load()
        scoring = ScoringScene(engine!!, gameScene!!, songMenu!!)
        gameScene?.setScoringScene(scoring!!)
        gameScene?.setOldScene(songMenu!!.scene!!)

        GlobalFPSOverlay().attachToCamera(camera!!)

        if (songService != null) {
            songService?.stop()
            songService?.hideNotification()
        }
    }

    fun getMainActivity(): MainActivity? {
        return mainActivityRef?.get()
    }

    fun setMainActivity(mainActivity: MainActivity?) {
        mainActivityRef = WeakReference(mainActivity)
    }

    fun setLoadingProgress(loadingProgress: Int) {
        this.loadingProgress = loadingProgress
    }

    fun getDisplayMetrics(): DisplayMetrics {
        val dm = DisplayMetrics()
        val activity = getMainActivity()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        return dm
    }

    companion object {
        @JvmStatic
        fun getInstance(): GlobalManager = Holder.INSTANCE
    }

    private object Holder {
        val INSTANCE = GlobalManager()
    }
}
