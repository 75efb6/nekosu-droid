package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.SkinManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite

class FollowTrack : GameObject() {

    private val points = arrayOfNulls<Sprite>(MAX_POINTS)
    private var pointsCount = 0
    private val frameCount: Int
    private var listener: GameObjectListener? = null
    private var timeLeft = 0f
    private var time = 0f
    private var empty = false
    private var approach = 0f

    init {
        frameCount = SkinManager.getFrames("followpoint")
    }

    fun init(
        listener: GameObjectListener,
        scene: Scene,
        start: PointF,
        end: PointF,
        time: Float,
        approachtime: Float,
        scale: Float
    ) {
        this.listener = listener
        approach = approachtime
        timeLeft = time
        this.time = 0f

        val dist = Utils.distance(start, end)
        val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble()).toFloat()
        var region = ResourceManager.getInstance().getTexture(
            if (frameCount > 1) "followpoint-0" else "followpoint"
        )
        if (region == null) {
            region = ResourceManager.getInstance().getTexture("followpoint")
        }
        val pointSize = region!!.getWidth() * scale
        var count = ((dist - 64 * scale) / pointSize).toInt()
        if (count > 0) count--
        count = minOf(count, 30)
        if (count <= 0) {
            empty = true
            GameObjectPool.instance.putTrac(this)
            return
        }
        empty = false

        pointsCount = 0
        val pos = PointF()
        val rotDeg = (angle * 180 / Math.PI).toFloat()
        for (i in 0 until count) {
            val percent = 1 - (i + 1).toFloat() / (count + 1).toFloat()
            pos.x = start.x * percent + end.x * (1 - percent)
            pos.y = start.y * percent + end.y * (1 - percent)
            val point: Sprite
            if (frameCount == 1) {
                point = SpritePool.getInstance().getCenteredSprite("followpoint", pos)
            } else {
                point = SpritePool.getInstance().getAnimSprite("followpoint-", frameCount)
                point.setPosition(pos.x - pointSize * 0.5f, pos.y - pointSize * 0.5f)
            }
            point.setScale(scale)
            point.setAlpha(0f)
            point.setRotation(rotDeg)
            scene.attachChild(point, 0)
            points[pointsCount++] = point
        }

        listener.addPassiveObject(this)
    }

    override fun update(dt: Float) {
        if (empty) return
        time += dt

        val n = pointsCount
        if (timeLeft <= approach) {
            var percent = time / (approach * 0.5f)
            if (percent > 1) percent = 1f
            for (i in 0 until n) {
                points[i]!!.setAlpha(percent)
            }
        } else if (time < timeLeft - approach) {
            var percent = time / (timeLeft - approach)
            if (percent > 1) percent = 1f
            val visible = (percent * n).toInt()
            for (i in 0 until visible) {
                points[i]!!.setAlpha(1f)
            }
            if (percent < 1 && visible < n) {
                points[visible]!!.setAlpha(percent - percent.toInt())
            }
        } else {
            var percent = 1 - (timeLeft - time) / approach
            if (percent > 1) percent = 1f
            val faded = (percent * n).toInt()
            for (i in 0 until faded) {
                points[i]!!.setAlpha(0f)
            }
            if (percent in 0.0..1.0 && faded < n) {
                points[faded]!!.setAlpha(1 - percent + percent.toInt())
            }
        }

        if (time >= timeLeft) {
            empty = true
            for (i in 0 until n) {
                val sp = points[i]!!
                sp.detachSelf()
                if (sp is AnimSprite) {
                    SpritePool.getInstance().putAnimSprite("followpoint-", sp)
                } else {
                    SpritePool.getInstance().putSprite("followpoint", sp)
                }
                points[i] = null
            }
            listener!!.removePassiveObject(this)
            GameObjectPool.instance.putTrac(this)
        }
    }

    companion object {
        private const val MAX_POINTS = 30
    }
}
