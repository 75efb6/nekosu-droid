package com.edlplan.osu.support.slider

import com.edlplan.andengine.TriangleBuilder
import com.edlplan.framework.math.FMath
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.AbstractPath

class DrawLinePath {

    var alpha: Float = 0f
    var width: Float = 0f

    private val current = Vec2()

    private var segTheta: FloatArray? = null
    private var segNormX: FloatArray? = null
    private var segNormY: FloatArray? = null
    private var structureSize: Int = 0

    private var segQuadStartOffset: IntArray? = null

    private var triangles: TriangleBuilder? = null
    private var path: AbstractPath? = null

    private var cachedThetaPath: AbstractPath? = null
    private var cachedThetaPathSize: Int = 0
    private var cachedFirstX: Float = 0f
    private var cachedFirstY: Float = 0f
    private var cachedLastX: Float = 0f
    private var cachedLastY: Float = 0f

    constructor(p: AbstractPath, width: Float) {
        alpha = 1f
        prepareForPath(p)
        this.width = width
    }

    constructor() {
        alpha = 1f
    }

    fun prepareForPath(p: AbstractPath): DrawLinePath {
        this.path = p
        val n = p.size()

        if (n < 2) {
            structureSize = n
            cachedThetaPath = p
            cachedThetaPathSize = n
            return this
        }

        val first = p[0]
        val last = p[n - 1]
        if (cachedThetaPath === p && cachedThetaPathSize == n &&
            cachedFirstX == first.x && cachedFirstY == first.y &&
            cachedLastX == last.x && cachedLastY == last.y
        ) {
            return this
        }

        val segs = n - 1
        if (segTheta == null || segTheta!!.size < segs) {
            segTheta = FloatArray(segs)
            segNormX = FloatArray(segs)
            segNormY = FloatArray(segs)
            segQuadStartOffset = IntArray(segs)
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

        cachedThetaPath = p
        cachedThetaPathSize = n
        cachedFirstX = first.x
        cachedFirstY = first.y
        cachedLastX = last.x
        cachedLastY = last.y
        structureSize = n
        return this
    }

    fun buildForWidth(width: Float, builder: TriangleBuilder): TriangleBuilder {
        this.width = width
        builder.length = 0
        triangles = builder
        init()
        return builder
    }

    fun getSegmentQuadStartOffset(segIdx: Int): Int {
        val offsets = segQuadStartOffset
        return if (offsets != null && segIdx < offsets.size) offsets[segIdx] else 0
    }

    fun buildBoundarySuffix(
        width: Float, builder: TriangleBuilder,
        boundarySegIdx: Int, boundaryPoint: Vec2,
        segStartPoint: Vec2
    ) {
        this.width = width
        this.triangles = builder
        addLineQuads(boundarySegIdx, segStartPoint, boundaryPoint)
        val theta = segTheta!![boundarySegIdx]
        addLineCap(boundaryPoint, theta - FMath.PiHalf, FMath.Pi)
    }

    fun buildBoundaryPrefix(
        width: Float, builder: TriangleBuilder,
        boundarySegIdx: Int, boundaryPoint: Vec2,
        segEndPoint: Vec2
    ): Int {
        this.width = width
        this.triangles = builder

        val theta = segTheta!![boundarySegIdx]

        addLineCap(boundaryPoint, theta + FMath.PiHalf, FMath.Pi)

        if (Vec2.length(boundaryPoint, segEndPoint) > 1e-3f) {
            addLineQuads(boundarySegIdx, boundaryPoint, segEndPoint)
        }

        if (boundarySegIdx + 1 < structureSize - 1) {
            val nextTheta = segTheta!![boundarySegIdx + 1]
            addLineCap(segEndPoint, theta - FMath.PiHalf, nextTheta - theta)
        } else {
            addLineCap(segEndPoint, theta - FMath.PiHalf, FMath.Pi)
        }

        val offsets = segQuadStartOffset
        return if (boundarySegIdx + 1 < offsets!!.size) {
            offsets[boundarySegIdx + 1]
        } else {
            0
        }
    }

    fun reset(p: AbstractPath, width: Float): DrawLinePath {
        prepareForPath(p)
        this.width = width
        triangles?.length = 0
        return this
    }

    fun reset(p: AbstractPath): DrawLinePath {
        prepareForPath(p)
        triangles?.length = 0
        return this
    }

    fun getTriangles(): TriangleBuilder {
        val t = triangles
        return if (t == null) {
            val builder = TriangleBuilder(path!!.size() * 6)
            triangles = builder
            init()
            builder
        } else {
            t.length = 0
            init()
            t
        }
    }

    fun getTriangles(builder: TriangleBuilder): TriangleBuilder {
        builder.length = 0
        triangles = builder
        init()
        return triangles!!
    }

    private fun addLineCap(org: Vec2, theta: Float, thetaDiff: Float) {
        val dir = Math.signum(thetaDiff)
        if (dir == 0f) return
        var thetaVar = theta
        var thetaDiffVar = thetaDiff * dir
        val amountPoints = Math.ceil((thetaDiffVar / CAP_STEP).toDouble()).toInt()
        if (amountPoints == 0) return

        if (dir < 0) thetaVar += FMath.Pi

        var ux = Math.cos(thetaVar.toDouble()).toFloat()
        var uy = Math.sin(thetaVar.toDouble()).toFloat()
        current.x = ux * width + org.x
        current.y = uy * width + org.y

        val cs = CAP_STEP_COS
        val ss = dir * CAP_STEP_SIN

        var prevX = current.x
        var prevY = current.y

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

            triangles!!.add(org.x, org.y, prevX, prevY, x2, y2)

            prevX = x2
            prevY = y2
            ux = ux2
            uy = uy2
        }
    }

    private fun addLineQuads(segIdx: Int, ps: Vec2, pe: Vec2) {
        val dx = pe.x - ps.x
        val dy = pe.y - ps.y
        if (dx * dx + dy * dy < DEGENERATE_LENGTH_SQ) {
            return
        }
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

        triangles!!.add(ps.x, ps.y, pe.x, pe.y, elx, ely)
        triangles!!.add(ps.x, ps.y, elx, ely, slx, sly)
        triangles!!.add(ps.x, ps.y, erx, ery, pe.x, pe.y)
        triangles!!.add(ps.x, ps.y, srx, sry, erx, ery)
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
        segQuadStartOffset!![0] = triangles!!.length
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
            segQuadStartOffset!![i] = triangles!!.length
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
        private const val DEGENERATE_LENGTH_SQ = 1e-6f
    }
}
