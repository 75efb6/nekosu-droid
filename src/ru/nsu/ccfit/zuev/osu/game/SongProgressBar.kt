package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.RGBAColor
import ru.nsu.ccfit.zuev.osu.Utils

class SongProgressBar : GameObject {

    private val progressRect: Rectangle
    private val bgRect: Rectangle
    private var time = 0f
    private var startTime = 0f
    private var passedTime = 0f

    constructor(
        listener: GameObjectListener?,
        scene: Scene,
        time: Float,
        startTime: Float,
        pos: PointF
    ) : this(listener, scene, time, startTime, pos, Utils.toRes(300).toFloat(), Utils.toRes(7).toFloat())

    constructor(
        listener: GameObjectListener?,
        scene: Scene,
        time: Float,
        startTime: Float,
        pos: PointF,
        width: Float,
        height: Float
    ) {
        this.time = time
        this.startTime = startTime
        if (listener != null) listener.addPassiveObject(this)

        bgRect = Rectangle(pos.x, pos.y, width, height)
        bgRect.setColor(0f, 0f, 0f, 0.3f)
        scene.attachChild(bgRect)

        progressRect = Rectangle(bgRect.getX(), bgRect.getY(), 0f, bgRect.getHeight())
        progressRect.setColor(153f / 255f, 204f / 255f, 51f / 255f)
        scene.attachChild(progressRect)
    }

    override fun update(dt: Float) {
        if (passedTime >= startTime) {
            passedTime = minOf(time, passedTime + dt)
            progressRect.setWidth(bgRect.getWidth() * (passedTime - startTime) / (time - startTime))
        } else {
            passedTime = minOf(startTime, passedTime + dt)
            progressRect.setWidth(bgRect.getWidth() * passedTime / startTime)
            if (passedTime >= startTime) {
                progressRect.setColor(1f, 1f, 150f / 255f)
            }
        }
    }

    fun setTime(time: Float) {
        this.time = time
    }

    fun setStartTime(startTime: Float) {
        this.startTime = startTime
    }

    fun setPassedTime(passedTime: Float) {
        this.passedTime = passedTime
    }

    fun setProgressRectColor(color: RGBAColor) {
        progressRect.setColor(color.r(), color.g(), color.b(), color.a())
    }
}
