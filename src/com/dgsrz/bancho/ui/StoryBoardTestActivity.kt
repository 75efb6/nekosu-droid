package com.dgsrz.bancho.ui

import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.engine.options.EngineOptions
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.ColorBackground
import org.anddev.andengine.entity.sprite.BaseSprite
import org.anddev.andengine.entity.util.FPSLogger
import org.anddev.andengine.ui.activity.BaseGameActivity
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger
import ru.nsu.ccfit.zuev.audio.BassAudioPlayer
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.storyboard.OsbParser
import ru.nsu.ccfit.zuev.osu.storyboard.OsuSprite

class StoryBoardTestActivity : BaseGameActivity(), IUpdateHandler {

    var mBackground: String? = null
    var mAudioFileName: String? = null
    lateinit var background: Entity
    lateinit var fail: Entity
    lateinit var pass: Entity
    lateinit var foreground: Entity
    var onScreenDrawCalls = AtomicInteger(0)
    private var osuSprites: LinkedList<OsuSprite>? = null
    private var nextSprite: OsuSprite? = null

    private lateinit var scene: Scene

    private var totalElapsed = 0f

    init {
        activity = this
    }

    override fun onLoadEngine(): Engine {
        val camera = Camera(0f, 0f, CAMERA_WIDTH.toFloat(), CAMERA_HEIGHT.toFloat())
        return Engine(EngineOptions(true,
            EngineOptions.ScreenOrientation.LANDSCAPE, RatioResolutionPolicy(CAMERA_WIDTH.toFloat(), CAMERA_HEIGHT.toFloat()),
            camera))
    }

    override fun onLoadResources() {
    }

    override fun onLoadScene(): Scene {
        mEngine.registerUpdateHandler(FPSLogger(1f))
        scene = Scene()
        scene.background = ColorBackground(0f, 0f, 0f)
        scene.registerUpdateHandler(this)

        ResourceManager.getInstance().Init(mEngine, this)
        ResourceManager.getInstance().loadHighQualityAsset("cursor", "gfx/cursor.png")
        ResourceManager.getInstance().loadHighQualityFileUnderFolder(File(FOLDER))

        BassAudioPlayer.initDevice()

        try {
            System.gc()
            OsbParser.instance.parse(FOLDER + PATH)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        background = Entity(0f, 0f)
        pass = Entity(0f, 0f)
        foreground = Entity(0f, 0f)
        scene.attachChild(background)
        scene.attachChild(pass)
        scene.attachChild(foreground)

        osuSprites = OsbParser.instance.getSprites()
        if (osuSprites != null && osuSprites!!.size > 0) {
            nextSprite = osuSprites!!.removeAt(0)
        }
        return scene
    }

    override fun onLoadComplete() {
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        totalElapsed += pSecondsElapsed
        if (nextSprite != null) {
            checkSpriteTime(totalElapsed * 1000)
        }
    }

    private fun checkSpriteTime(pSecondsElapsed: Float) {
        if (pSecondsElapsed >= nextSprite!!.spriteStartTime) {
            nextSprite!!.play()
            if (osuSprites!!.size > 0) {
                nextSprite = osuSprites!!.removeAt(0)
                checkSpriteTime(pSecondsElapsed)
            } else {
                nextSprite = null
            }
        }
    }

    override fun reset() {
    }

    fun attachBackground(sprite: BaseSprite) {
        background.attachChild(sprite)
        background.sortChildren()
    }

    fun attachPass(sprite: BaseSprite) {
        pass.attachChild(sprite)
        pass.sortChildren()
    }

    fun attachForeground(sprite: BaseSprite) {
        foreground.attachChild(sprite)
        foreground.sortChildren()
    }

    companion object {
        const val FOLDER = "/sdcard/osu!player/EOS"
        const val PATH = "/1.osu"
        private const val INVALID_POINTER_ID = -1
        private const val CAMERA_WIDTH = 640
        private const val CAMERA_HEIGHT = 480
        var activity: StoryBoardTestActivity? = null
            private set
    }
}
