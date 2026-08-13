package com.edlplan.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.drawable.Drawable
import com.edlplan.framework.math.FMath
import ru.nsu.ccfit.zuev.osu.Config
import java.util.LinkedList
import java.util.Random

class TriangleDrawable @JvmOverloads constructor(preSpawnTriangles: Boolean = true) : Drawable() {

    private val m = (Math.sqrt(3.0) / 2).toFloat()
    private var spawnClock = 0
    private val spawnCost = 120
    private val triangles = LinkedList<Triangle>()
    private var spawnNewTriangles = true
    private val paint = Paint()
    private val random = Random()
    private val path = Path()
    private val colors = intArrayOf(
        0xFFF7E67A.toInt(),
        0xFFEE8100.toInt(),
        0xFF74C684.toInt(),
        0xFFF8558C.toInt(),
        0xFF5245F7.toInt()
    )
    private var width: Float = 0f
    private var height: Float = 0f
    private var time: Long = -1
    private val preSpawnTriangles: Boolean
    private var xDistribution: PosXDistribution? = null
    private var defaultXDistributionScale = 10f
    private var baseSpeed = 15f
    private var edgeClampRate = 0.2f
    private var freeze = false

    init {
        this.preSpawnTriangles = preSpawnTriangles
    }

    override fun draw(canvas: Canvas) {
        onDraw(canvas)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    fun setFreeze(freeze: Boolean) {
        this.freeze = freeze
    }

    fun setXDistribution(xDistribution: PosXDistribution?) {
        this.xDistribution = xDistribution
        this.time = -1
        this.triangles.clear()
    }

    fun setDefaultXDistributionScale(defaultXDistributionScale: Float) {
        this.defaultXDistributionScale = defaultXDistributionScale
    }

    fun setEdgeClampRate(edgeClampRate: Float) {
        this.edgeClampRate = edgeClampRate
    }

    fun getEdgeClampRate(): Float = edgeClampRate

    protected fun onDraw(canvas: Canvas) {
        if (!Config.isTrianglesAnimation()) {
            return
        }

        width = bounds.width().toFloat()
        height = bounds.height().toFloat()
        paint.color = 0xFFFFFFFF.toInt()
        if (time == -1L) {
            time = System.currentTimeMillis()
            if (this.preSpawnTriangles) {
                for (i in 0 until 200) {
                    update(36)
                }
            }
            return
        }
        val dt = (System.currentTimeMillis() - time).toInt()
        time += dt
        if (!freeze) {
            update(dt * 2)
        }

        for (triangle in triangles) {
            path.rewind()
            path.moveTo(triangle.center!!.x, triangle.center!!.y - triangle.size)
            path.rLineTo(triangle.size * m, triangle.size * 1.5f)
            path.rLineTo(-triangle.size * m * 2, 0f)
            path.close()
            canvas.drawPath(path, triangle.paint!!)
        }
    }

    private fun nextAlpha(): Float {
        return Math.max(1 - 1.5 * Math.abs(random.nextGaussian()), 0.1).toFloat()
    }

    private fun defaultX(): Float {
        return (2f / (1 + Math.exp((Math.random() * 2 - 1) * defaultXDistributionScale.toDouble())) - 1).toFloat()
    }

    private fun nextPos(): PointF {
        return PointF(
            FMath.clamp(width / 2 * (1 +
                    (xDistribution?.generate() ?: defaultX())), 0f, width),
            height
        )
    }

    private fun nextSize(): Float {
        return (Math.pow(Math.random(), 2.0) * 200).toFloat()
    }

    private fun spawnOneTriangle() {
        val triangle = Triangle()
        triangle.color = colors[random.nextInt(colors.size)]
        triangle.alpha = nextAlpha()
        triangle.center = nextPos()

        triangle.speed = (15 * (Math.abs(random.nextGaussian()) * 0.4 + 0.6)).toFloat()
        triangle.size = nextSize()
        triangle.center!!.y += triangle.size
        triangle.lifeTime = 8000
        triangle.center!!.x = FMath.clamp(triangle.center!!.x, triangle.size * edgeClampRate, width - triangle.size * edgeClampRate)
        triangle.fixBound()
        if (triangle.size < 20 || triangle.lifeTime < 100) {
            return
        }
        triangle.paint = Paint()
        triangle.paint!!.color = triangle.color
        triangles.add(triangle)
    }

    private fun doSpawnNewTriangles(dt: Int) {
        spawnClock += dt
        while (spawnClock > spawnCost) {
            spawnOneTriangle()
            spawnClock -= spawnCost
        }
    }

    private fun update(dt: Int) {
        if (spawnNewTriangles) {
            doSpawnNewTriangles(dt)
        }

        val iterator = triangles.iterator()
        while (iterator.hasNext()) {
            val triangle = iterator.next()
            triangle.update(dt)
            if (triangle.updateAlpha < 0.005) {
                iterator.remove()
            }
        }
    }

    interface PosXDistribution {
        fun generate(): Float
    }

    inner class Triangle {
        var paint: Paint? = null
        var center: PointF? = null
        var size: Float = 0f
        var alpha: Float = 0f
        var updateAlpha: Float = 0f
        var speed: Float = 0f
        var color: Int = 0
        var lifeTime: Int = 0
        var passTime: Int = 0

        fun fixBound() {}

        fun update(dt: Int) {
            passTime += dt
            updateAlpha = center!!.y / height * alpha
            center!!.y -= dt * speed / 1000
            paint!!.alpha = Math.min(255, (updateAlpha * 255).toInt())
        }
    }
}
