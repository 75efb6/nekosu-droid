package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.modifier.FadeOutModifier
import org.anddev.andengine.entity.modifier.LoopEntityModifier
import org.anddev.andengine.entity.modifier.RotationByModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.ColorBackground
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite

class LoadingScreen : IUpdateHandler {
    internal val scene: LoadingScene
    private val logText: ChangeableText
    private var percentage: Float

    init {
        val toastLoggerLog = ToastLogger.getLog()
        toastLoggerLog?.clear()
        scene = LoadingScene()
        scene.registerEntityModifier(FadeOutModifier(0.4f))
        val tex = ResourceManager.getInstance().getTexture("menu-background")
        if (tex != null) {
            var height = tex.height.toFloat()
            height *= Config.getRES_WIDTH() / tex.width.toFloat()
            val menuBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2,
                Config.getRES_WIDTH().toFloat(), height, tex)
            scene.setBackground(SpriteBackground(menuBg))
        } else {
            scene.setBackground(ColorBackground(15 / 255f, 15 / 255f, 26 / 255f))
        }
        val dimOverlay = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        dimOverlay.setColor(0.04f, 0.04f, 0.10f, 0.78f)
        scene.attachChild(dimOverlay)
        val accentTop = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Utils.toRes(3).toFloat())
        accentTop.setColor(0.90f, 0.24f, 0.55f, 1.0f)
        scene.attachChild(accentTop)
        val accentBottom = Rectangle(0f, Config.getRES_HEIGHT() - Utils.toRes(3).toFloat(),
            Config.getRES_WIDTH().toFloat(), Utils.toRes(3).toFloat())
        accentBottom.setColor(0.90f, 0.24f, 0.55f, 1.0f)
        scene.attachChild(accentBottom)
        val loadingTexture = ResourceManager.getInstance().getTexture("loading-title")!!
        val loadingTitle = Sprite(0f, 0f,
            Config.getRES_WIDTH().toFloat(), loadingTexture.height.toFloat(), loadingTexture)
        scene.attachChild(loadingTitle)
        logText = ChangeableText(0f, 0f, ResourceManager.getInstance()
            .getFont("logFont"), "", 5)
        scene.attachChild(logText)
        ToastLogger.setPercentage(-1f)
        percentage = -1f
        val ltexture = ResourceManager.getInstance().getTexture("loading")!!
        val circle = CentredSprite(Config.getRES_WIDTH() / 2f,
            Config.getRES_HEIGHT() / 2f, ltexture)
        circle.registerEntityModifier(LoopEntityModifier(
            RotationByModifier(2.0f, 360f)))
        scene.attachChild(circle)
        scene.registerUpdateHandler(this)
    }

    fun getScene(): Scene = scene

    fun show() {
        GlobalManager.getInstance().engine?.setScene(scene)
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        if (ToastLogger.getPercentage() != percentage) {
            percentage = ToastLogger.getPercentage()
            logText.setText(String.format("%d%%", percentage.toInt()))
            logText.setPosition(Config.getRES_WIDTH() / 2f - logText.width / 2,
                Config.getRES_HEIGHT() - Utils.toRes(100).toFloat())
        }
    }

    override fun reset() {}

    class LoadingScene : Scene()
}
