package ru.nsu.ccfit.zuev.osu.menu

import com.reco1l.framework.lang.Execution
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.ColorBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.util.HorizontalAlign
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

class SplashScene : IUpdateHandler {

    private val scene: Scene
    private val infoText: ChangeableText
    private val progressText: ChangeableText
    private val mLoading: Sprite
    private var mStarting = true

    init {
        scene = Scene()
        scene.setBackground(ColorBackground(15 / 255f, 15 / 255f, 26 / 255f))
        val accentTop = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 3f)
        accentTop.setColor(0.90f, 0.24f, 0.55f, 1.0f)
        scene.attachChild(accentTop)
        val accentBottom = Rectangle(0f, Config.getRES_HEIGHT() - 3f,
            Config.getRES_WIDTH().toFloat(), 3f)
        accentBottom.setColor(0.90f, 0.24f, 0.55f, 1.0f)
        scene.attachChild(accentBottom)
        mLoading = initializeLoading()
        progressText = initializeProgress()
        infoText = initializeInfo()
        scene.registerUpdateHandler(this)
    }

    private fun initializeLoading(): Sprite {
        val loadTex = ResourceManager.getInstance().getTexture("loading_start")
        val loading = Sprite(0f, 0f, loadTex)
        loading.setPosition((Config.getRES_WIDTH() - loading.width) / 2f, (Config.getRES_HEIGHT() - loading.height) / 2f)
        loading.setRotationCenter(loading.width / 2f, loading.height / 2f)
        loading.setScale(0.4f)
        loading.setAlpha(0f)
        loading.registerEntityModifier(LoopEntityModifier(RotationByModifier(2f, 360f)))
        scene.attachChild(loading)
        return loading
    }

    private fun initializeInfo(): ChangeableText {
        val info = ChangeableText(0f, 0f, ResourceManager.getInstance().getFont("font"), "", HorizontalAlign.CENTER, 1024)
        info.setPosition((Config.getRES_WIDTH() - info.width) / 2, Config.getRES_HEIGHT() - info.height - 20)
        info.setAlpha(0f)
        info.setScale(0.6f)
        scene.attachChild(info)
        return info
    }

    fun getScene(): Scene = scene

    fun playWelcomeAnimation() {
        mStarting = false
        mLoading.registerEntityModifier(FadeOutModifier(0.2f))
        Execution.updateThread {
            infoText.detachSelf()
            progressText.detachSelf()
        }
        try {
            Thread.sleep(220)
        } catch (ignored: InterruptedException) {
        }
        val welcomeTex = ResourceManager.getInstance().getTexture("welcome")!!
        val welcomeSprite = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("welcome"))
        val welcomeSound = ResourceManager.getInstance().getSound("welcome")
        val welcomePiano = ResourceManager.getInstance().getSound("welcome_piano")
        welcomeSprite.setPosition((Config.getRES_WIDTH() - welcomeTex.width) / 2f, (Config.getRES_HEIGHT() - welcomeTex.height) / 2f)
        welcomeSprite.setAlpha(0f)
        welcomeSprite.setScaleY(0f)
        scene.attachChild(welcomeSprite)
        welcomeSound.play()
        welcomePiano.play()
        welcomeSprite.registerEntityModifier(ParallelEntityModifier(
            FadeInModifier(2.5f),
            SequenceEntityModifier(
                ScaleModifier(0.25f, 1f, 1f, 0f, 1f),
                ScaleModifier(2.25f, 1f, 1.1f)
            )
        ))
    }

    private fun initializeProgress(): ChangeableText {
        val progress = ChangeableText(0f, 0f, ResourceManager.getInstance().getFont("font"), "0 %", HorizontalAlign.CENTER, 10)
        progress.setPosition((Config.getRES_WIDTH() - progress.width) / 2f, (Config.getRES_HEIGHT() + mLoading.height) / 2f - mLoading.height / 4f)
        progress.setAlpha(0f)
        progress.setScale(0.5f)
        scene.attachChild(progress)
        return progress
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        val progress = GlobalManager.getInstance().loadingProgress
        if (mStarting) {
            mLoading.setAlpha(mLoading.alpha + 0.1f)
        }
        progressText.setText(String.format("%.0f %%", progress))
        progressText.setPosition((Config.getRES_WIDTH() - progressText.width) / 2f, (Config.getRES_HEIGHT() + mLoading.height) / 2f - mLoading.height / 4f)
        if (GlobalManager.getInstance().info != null) {
            infoText.setText(GlobalManager.getInstance().info)
            infoText.setPosition((Config.getRES_WIDTH() - infoText.width) / 2, Config.getRES_HEIGHT() - infoText.height - 20)
        }
    }

    override fun reset() {}

    companion object {
        @JvmField
        val INSTANCE = SplashScene()
    }
}
