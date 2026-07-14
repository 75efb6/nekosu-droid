package com.edlplan.osu.support.slider

import com.edlplan.andengine.Triangle3DBuilder
import com.edlplan.framework.math.FMath
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.AbstractPath

class Draw3DLinePath {

    var alpha: Float = 0f
    var width: Float = 0f

    private var zEdge: Float = -1f
    private var zCenter: Float = 1f

    private var segTheta: FloatArray? = null
    private var segNormX: FloatArray? = null
    private var segNormY: FloatArray? = null
    private var structureSize: Int = 0

    internal var triangles: Triangle3DBuilder? = null
    private var path: AbstractPath? = null

    constructor(p: AbstractPath, width: Float, zCenter: Float, zEdge: Float) {
        this.zCenter = zCenter
        this.zEdge = zEdge
        alpha = 1f
        this.width = width
        prepareForPath(p)
    }

    constructor() {
        alpha = 1f
    }

    fun setZCenter(zCenter: Float) {
        this.zCenter = zCenter
    }

    fun setZEdge(zEdge: Float) {
        this.zEdge = zEdge
    }

    fun prepareForPath(p: AbstractPath): Draw3DLinePath {
        this.path = p
        val n = p.size()
        if (n >= 2) {
            val segs = n - 1
            if (segTheta == null || segTheta!!.size < segs) {
                segTheta = FloatArray(segs)
                segNormX = FloatArray(segs)
                segNormY = FloatArray(segs)
            }
            var a = p[0]
            for (i in 0 until segs) {
                val b = p[i + 1]
                segTheta!![i] = Vec2.calTheta(a, b)
                val dx = b.x - a.x
                val dy = b.y - a.y
                val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (len > 1e-6f) {
                    segNormX!![i] = -dy / len
                    segNormY!![i] = dx / len
                } else {
                    segNormX!![i] = 0f
                    segNormY!![i] = 0f
                }
                a = b
            }
        }
        structureSize = n
        return this
    }

    fun buildForWidth(
        width: Float, zCenter: Float, zEdge: Float,
        builder: Triangle3DBuilder
    ): Triangle3DBuilder {
        this.width = width
        this.zCenter = zCenter
        this.zEdge = zEdge
        builder.reset()
        triangles = builder
        init()
        return builder
    }

    fun getTriangles(): Triangle3DBuilder {
        val t = triangles
        return if (t == null) {
            val builder = Triangle3DBuilder()
            triangles = builder
            init()
            builder
        } else {
            t.reset()
            init()
            t
        }
    }

    private fun addLineCap(org: Vec2, theta: Float, thetaDiff: Float) {
        val dir = Math.signum(thetaDiff)
        if (dir == 0f) return
        var thetaVar = theta
        val thetaDiffVar = thetaDiff * dir
        val amountPoints = Math.ceil((thetaDiffVar / CAP_STEP).toDouble()).toInt()
        if (amountPoints == 0) return

        if (dir < 0) thetaVar += FMath.Pi

        var ux = Math.cos(thetaVar.toDouble()).toFloat()
        var uy = Math.sin(thetaVar.toDouble()).toFloat()
        var prevX = ux * width + org.x
        var prevY = uy * width + org.y

        val cs = CAP_STEP_COS
        val ss = dir * CAP_STEP_SIN

        val orgX = org.x
        val orgY = org.y

        for (i in 1..amountPoints) {
            var ux2: Float
            var uy2: Float
            if (i == amountPoints && i * CAP_STEP > thetaDiffVar) {
                val finalAngle = thetaVar + dir * thetaDiffVar
                ux2 = Math.cos(finalAngle.toDouble()).toFloat()
                uy2 = Math.sin(finalAngle.toDouble()).toFloat()
            } else {
                ux2 = ux * cs - uy * ss
                uy2 = uy * cs + ux * ss
            }
            val x2 = ux2 * width + org.x
            val y2 = uy2 * width + org.y

            triangles!!.add(
                orgX, orgY, zCenter,
                prevX, prevY, zEdge,
                x2, y2, zEdge
            )

            prevX = x2
            prevY = y2
            ux = ux2
            uy = uy2
        }
    }

    private fun addLineQuads(segIdx: Int, ps: Vec2, pe: Vec2) {
        val nx = segNormX!![segIdx] * width
        val ny = segNormY!![segIdx] * width

        val slx = ps.x + nx
        val sly = ps.y + ny
        val srx = ps.x - nx
        val sry = ps.y - ny
        val elx = pe.x + nx
        val ely = pe.y + ny
        val erx = pe.x - nx
        val ery = pe.y - ny
        val psx = ps.x
        val psy = ps.y
        val pex = pe.x
        val pey = pe.y

        triangles!!.add(psx, psy, zCenter, pex, pey, zCenter, elx, ely, zEdge)
        triangles!!.add(psx, psy, zCenter, elx, ely, zEdge, slx, sly, zEdge)
        triangles!!.add(psx, psy, zCenter, erx, ery, zEdge, pex, pey, zCenter)
        triangles!!.add(psx, psy, zCenter, srx, sry, zEdge, erx, ery, zEdge)
    }

    private fun init() {
        val n = structureSize
        if (n < 2) {
            if (n == 1) {
                val p0 = path!![0]
                addLineCap(p0, FMath.Pi, FMath.Pi)
                addLineCap(p0, 0f, FMath.Pi)
            }
            return
        }

        var prev = path!![0]
        var next = path!![1]
        val theta = segTheta!![0]
        addLineCap(prev, theta + FMath.PiHalf, FMath.Pi)
        addLineQuads(0, prev, next)

        if (n == 2) {
            addLineCap(next, theta - FMath.PiHalf, FMath.Pi)
            return
        }

        var preTheta = theta
        for (i in 1 until n - 1) {
            prev = next
            next = path!![i + 1]
            val nextTheta = segTheta!![i]
            addLineCap(prev, preTheta - FMath.PiHalf, nextTheta - preTheta)
            addLineQuads(i, prev, next)
            preTheta = nextTheta
        }
        addLineCap(next, preTheta - FMath.PiHalf, FMath.Pi)
    }

    companion object {
        private const val MAXRES = 24
        private val CAP_STEP = FMath.Pi / MAXRES
        private val CAP_STEP_COS = Math.cos(CAP_STEP.toDouble()).toFloat()
        private val CAP_STEP_SIN = Math.sin(CAP_STEP.toDouble()).toFloat()
    }
}
