package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper
import java.util.LinkedList

class HitErrorMeter(
    private val bgScene: Scene,
    private val barAnchor: PointF,
    difficulty: Float,
    private val barHeight: Float,
    private val difficultyHelper: DifficultyHelper
) : GameObject() {

    private val onDisplayIndicators: MutableList<Rectangle> = LinkedList()
    private val recycledIndicators: MutableList<Rectangle> = LinkedList()
    private val boundary: Float

    init {
        boundary = difficultyHelper.hitWindowFor50(difficulty)

        val totalLen = boundary * 1500
        val hitMeter = Rectangle(barAnchor.x - totalLen / 2, barAnchor.y - barHeight, totalLen, barHeight * 2)
        hitMeter.setColor(0f, 0f, 0f, 0.8f)
        bgScene.attachChild(hitMeter)

        val hit50 = Rectangle(barAnchor.x - totalLen / 2, barAnchor.y - barHeight / 2, totalLen, barHeight)
        hit50.setColor(200f / 255f, 180f / 255f, 110f / 255f, 0.8f)
        bgScene.attachChild(hit50)

        val hit100Len = difficultyHelper.hitWindowFor100(difficulty) * 1500
        val hit100 = Rectangle(barAnchor.x - hit100Len / 2, barAnchor.y - barHeight / 2, hit100Len, barHeight)
        hit100.setColor(100f / 255f, 220f / 255f, 40f / 255f, 0.8f)
        bgScene.attachChild(hit100)

        val hit300Len = difficultyHelper.hitWindowFor300(difficulty) * 1500
        val hit300 = Rectangle(barAnchor.x - hit300Len / 2, barAnchor.y - barHeight / 2, hit300Len, barHeight)
        hit300.setColor(70f / 255f, 180f / 255f, 220f / 255f, 0.8f)
        bgScene.attachChild(hit300)

        val hitIndicator = Rectangle(barAnchor.x - 2, barAnchor.y - barHeight, 4f, barHeight * 2)
        hitIndicator.setColor(1f, 1f, 1f, 0.8f)
        hitIndicator.setZIndex(15)
        bgScene.attachChild(hitIndicator)
    }

    override fun update(dt: Float) {
        while (onDisplayIndicators.isNotEmpty()) {
            if (onDisplayIndicators[0].getAlpha() <= 0) {
                val removed = onDisplayIndicators.removeAt(0)
                removed.setVisible(false)
                removed.setIgnoreUpdate(true)
                removed.detachSelf()
                recycledIndicators.add(removed)
            } else {
                break
            }
        }
        for (i in onDisplayIndicators.indices) {
            val result = onDisplayIndicators[i]
            val currentAlpha = result.getAlpha() - 0.002f
            result.setAlpha(currentAlpha)
        }
    }

    fun putErrorResult(errorResult: Float) {
        if (Math.abs(errorResult) > boundary) return
        val scaledError = errorResult * 750

        if (recycledIndicators.isEmpty()) {
            val indicator = Rectangle(barAnchor.x - 2, barAnchor.y - barHeight, 4f, barHeight * 2)
            val posX = indicator.getX() + scaledError
            val posY = indicator.getY()
            indicator.setPosition(posX, posY)
            indicator.setColor(70f / 255f, 180f / 255f, 220f / 255f, 0.6f)
            indicator.setZIndex(10)
            bgScene.attachChild(indicator)
            onDisplayIndicators.add(indicator)
        } else {
            val indicator = recycledIndicators.removeAt(0)
            val posX = barAnchor.x - 2 + scaledError
            val posY = indicator.getY()
            indicator.setPosition(posX, posY)
            indicator.setColor(70f / 255f, 180f / 255f, 220f / 255f, 0.6f)
            indicator.setZIndex(10)
            indicator.setVisible(true)
            indicator.setIgnoreUpdate(false)
            bgScene.attachChild(indicator)
            onDisplayIndicators.add(indicator)
        }
    }
}
