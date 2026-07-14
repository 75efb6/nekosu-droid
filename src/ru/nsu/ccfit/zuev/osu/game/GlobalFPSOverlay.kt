package ru.nsu.ccfit.zuev.osu.game

import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.engine.camera.hud.HUD
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.util.FPSCounter
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

class GlobalFPSOverlay : HUD() {
    private val fpsCounter: FPSCounter
    private val fpsText: ChangeableText
    private val background: Rectangle
    private val sb = StringBuilder(32)
    private var cachedFrameLimit = 60

    init {
        val font: Font = ResourceManager.getInstance().getFont("smallFont")

        fpsText = ChangeableText(0f, 0f, font, "0 FPS (0 ms)", 32)
        fpsText.setAlpha(0.85f)

        background = Rectangle(0f, 0f, 10f, 10f)
        background.setColor(0f, 0f, 0f, 0.5f)
        attachChild(background)
        attachChild(fpsText)

        fpsCounter = object : FPSCounter() {
            override fun onUpdate(pSecondsElapsed: Float) {
                super.onUpdate(pSecondsElapsed)

                if (cachedFrameLimit == 60) {
                    val activity = GlobalManager.getInstance().getMainActivity()
                    if (activity != null) {
                        cachedFrameLimit = Math.round(activity.getRefreshRate())
                    }
                }
                val frameLimit = cachedFrameLimit
                val currentFps = minOf(Math.round(this.getFPS()), frameLimit)
                val frameTime = if (this.mFrames > 0) (this.mSecondsElapsed / this.mFrames) * 1000f else 0f

                sb.setLength(0)
                sb.append(currentFps).append(" FPS (").append(frameTime.toInt()).append(" ms)")
                fpsText.setText(sb.toString())

                val ratio = currentFps.toFloat() / frameLimit
                when {
                    ratio >= 0.8f -> fpsText.setColor(0f, 1f, 0f)
                    ratio >= 0.65f -> fpsText.setColor(1f, 1f, 0f)
                    else -> fpsText.setColor(1f, 0f, 0f)
                }

                val w = Config.getRES_WIDTH()
                val h = Config.getRES_HEIGHT()
                if (w > 0 && h > 0) {
                    val padX = 5f
                    val padY = 3f
                    val textW = fpsText.getWidth()
                    val textH = fpsText.getHeight()
                    val bgW = textW + padX * 2
                    val bgH = textH + padY * 2
                    val bgX = w - bgW - 5
                    val bgY = h - bgH - 10
                    background.setPosition(bgX, bgY)
                    background.setSize(bgW, bgH)
                    fpsText.setPosition(bgX + padX, bgY + padY)
                }

                if (this.mSecondsElapsed >= 1f) {
                    this.reset()
                }
            }
        }

        registerUpdateHandler(fpsCounter)
    }

    fun attachToCamera(camera: Camera) {
        camera.setHUD(this)
    }
}
