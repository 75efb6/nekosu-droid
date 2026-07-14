package ru.nsu.ccfit.zuev.osu.polygon

import android.graphics.PointF
import java.util.ArrayList

class Spline {
    private var mCtrlPts: ArrayList<PointF>
    private var mCurveType: CurveTypes
    private var mPath: ArrayList<Line>
    private var mPoints: ArrayList<PointF>
    private var mCtrlPtsCopy: ArrayList<PointF>? = null
    private var mPathCopy: ArrayList<Line>? = null
    private var mPointsCopy: ArrayList<PointF>? = null
    private var mLengths: ArrayList<Float>? = null

    constructor() {
        mCtrlPts = ArrayList()
        mCurveType = CurveTypes.Linear
        mPath = ArrayList()
        mPoints = ArrayList()
    }

    constructor(theControlPoints: ArrayList<PointF>, theCurveType: CurveTypes) {
        mCtrlPts = ArrayList(theControlPoints)
        mCurveType = theCurveType
        mPath = ArrayList()
        mPoints = ArrayList()
        sliderthing(mCurveType, mCtrlPts, mPath, mPoints)
    }

    fun getControlPoints(): ArrayList<PointF> {
        if (mCtrlPtsCopy == null) mCtrlPtsCopy = ArrayList(mCtrlPts)
        return mCtrlPtsCopy!!
    }

    fun setControlPoints(theControlPoints: ArrayList<PointF>) {
        mCtrlPts.clear()
        mCtrlPts.addAll(theControlPoints)
    }

    fun getType(): CurveTypes = mCurveType

    fun setType(type: CurveTypes) {
        mCurveType = type
    }

    fun getPath(): ArrayList<Line> {
        if (mPathCopy == null) mPathCopy = ArrayList(mPath)
        return mPathCopy!!
    }

    fun getPoints(): ArrayList<PointF> {
        if (mPointsCopy == null) mPointsCopy = ArrayList(mPoints)
        return mPointsCopy!!
    }

    fun getLengths(): ArrayList<Float> {
        if (mLengths == null) {
            mLengths = ArrayList(mPath.size + 1)
            var lengthSoFar = 0f
            for (x in mPath.indices) {
                mLengths!!.add(lengthSoFar)
                lengthSoFar += Rho(mPath[x])
            }
            mLengths!!.add(lengthSoFar)
        }
        return mLengths!!
    }

    private fun validateRange(which: Int, paramName: String, allowEnd: Boolean) {
        if (which >= mCtrlPts.size + (if (allowEnd) 0 else -1) || which < 0)
            throw ArrayIndexOutOfBoundsException(paramName)
    }

    fun refresh() {
        mPath.clear()
        mPoints.clear()
        sliderthing(mCurveType, mCtrlPts, mPath, mPoints)
        mCtrlPtsCopy = null
        mPathCopy = null
        mPointsCopy = null
        mLengths = null
    }

    fun adjustPt(which: Int, where: PointF) {
        validateRange(which, "which", true)
        mCtrlPts.add(which, PointF(where.x, where.y))
        refresh()
    }

    fun addPt(after: Int) {
        validateRange(after, "after", false)
        val pt1 = mCtrlPts[after]
        val pt2 = mCtrlPts[after + 1]
        var target = Lerp(pt1, pt2, 0.5f)
        target = PointF(Math.round(target.x.toDouble()).toFloat(), Math.round(target.y.toDouble()).toFloat())
        mCtrlPts.add(after + 1, target)
        refresh()
    }

    fun delPt(where: Int) {
        validateRange(where, "where", true)
        mCtrlPts.removeAt(where)
        refresh()
    }

    private fun sliderthing(curveType: CurveTypes, sliderCurvePoints: ArrayList<PointF>, path: ArrayList<Line>, points: ArrayList<PointF>) {
        when (curveType) {
            CurveTypes.Catmull -> {
                for (j in 0 until sliderCurvePoints.size - 1) {
                    val v1 = if (j - 1 >= 0) sliderCurvePoints[j - 1] else sliderCurvePoints[j]
                    val v2 = sliderCurvePoints[j]
                    val v3 = if (j + 1 < sliderCurvePoints.size)
                        sliderCurvePoints[j + 1]
                    else
                        Add(v2, Subtract(v2, v1))
                    val v4 = if (j + 2 < sliderCurvePoints.size)
                        sliderCurvePoints[j + 2]
                    else
                        Add(v3, Subtract(v3, v2))

                    for (k in 0 until DETAIL_LEVEL) {
                        points.add(CatmullRom(v1, v2, v3, v4, k.toFloat() / DETAIL_LEVEL))
                    }
                }
            }
            CurveTypes.Bezier -> {
                var lastIndex = 0
                for (i in 1 until sliderCurvePoints.size) {
                    val isDuplicate = sliderCurvePoints[i].x == sliderCurvePoints[i - 1].x && sliderCurvePoints[i].y == sliderCurvePoints[i - 1].y
                    val isEnd = i == sliderCurvePoints.size - 1

                    if (isDuplicate || isEnd) {
                        val endIndex = if (isEnd && !isDuplicate) i + 1 else i
                        val thisLength = ArrayList<PointF>(sliderCurvePoints.subList(lastIndex, endIndex))
                        val points1 = CreateBezier(thisLength)
                        points.addAll(points1)
                        lastIndex = i
                    }
                }
                if (lastIndex == 0 && sliderCurvePoints.isNotEmpty()) {
                    val points1 = CreateBezier(sliderCurvePoints)
                    points.addAll(points1)
                }
            }
            CurveTypes.Linear -> {
                for (i in 1 until sliderCurvePoints.size) {
                    val l = Line(sliderCurvePoints[i - 1], sliderCurvePoints[i])
                    var segments = (Rho(l) / 10).toInt()
                    if (segments <= 3) segments = 5
                    for (j in 0 until segments) {
                        points.add(Add(l.p1, MultiplyPt(Subtract(l.p2, l.p1), j.toFloat() / segments)))
                    }
                }
            }
            CurveTypes.PerfectCurve -> {
                if (sliderCurvePoints.size < 3 ||
                    (sliderCurvePoints.size == 3 && (sliderCurvePoints[0].x - sliderCurvePoints[2].x) * (sliderCurvePoints[1].y - sliderCurvePoints[2].y)
                            == (sliderCurvePoints[1].x - sliderCurvePoints[2].x) * (sliderCurvePoints[0].y - sliderCurvePoints[2].y))
                ) {
                    sliderthing(CurveTypes.Linear, mCtrlPts, mPath, mPoints)
                    return
                }
                val point1 = sliderCurvePoints[0]
                val point2 = sliderCurvePoints[1]
                val point3 = sliderCurvePoints[2]
                val circleCenter = CircleCenterPoint(point1, point2, point3)
                val radius = CircleRadius(point1, point2, point3)
                var startAng = Math.atan2((point1.y - circleCenter.y).toDouble(), (point1.x - circleCenter.x).toDouble()).toFloat()
                val midAng = Math.atan2((point2.y - circleCenter.y).toDouble(), (point2.x - circleCenter.x).toDouble()).toFloat()
                var endAng = Math.atan2((point3.y - circleCenter.y).toDouble(), (point3.x - circleCenter.x).toDouble()).toFloat()
                if (!isIn(startAng, midAng, endAng)) {
                    if (Math.abs(startAng + TWO_PI - endAng) < TWO_PI && isIn(startAng + TWO_PI, midAng, endAng))
                        startAng += TWO_PI
                    else if (Math.abs(startAng - (endAng + TWO_PI)) < TWO_PI && isIn(startAng, midAng, endAng + TWO_PI))
                        endAng += TWO_PI
                    else if (Math.abs(startAng - TWO_PI - endAng) < TWO_PI && isIn(startAng - TWO_PI, midAng, endAng))
                        startAng -= TWO_PI
                    else if (Math.abs(startAng - (endAng - TWO_PI)) < TWO_PI && isIn(startAng, midAng, endAng - TWO_PI))
                        endAng -= TWO_PI
                }
                if (Math.abs(startAng - midAng) < 0.1 && Math.abs(midAng - endAng) < 0.1) {
                    sliderthing(CurveTypes.Bezier, mCtrlPts, mPath, mPoints)
                    return
                }
                for (k in 0 until DETAIL_LEVEL) {
                    points.add(CircularArc(startAng, endAng, circleCenter, radius, k.toFloat() / DETAIL_LEVEL))
                }
            }
        }
    }

    enum class CurveTypes {
        Linear, Bezier, Catmull, PerfectCurve
    }

    inner class Line(var p1: PointF, var p2: PointF)

    companion object {
        private val TWO_PI = (Math.PI * 2).toFloat()
        private var instance: Spline? = null
        private var DETAIL_LEVEL = 20

        @JvmStatic
        fun getInstance(): Spline {
            if (instance == null) {
                instance = Spline()
            }
            return instance!!
        }

        private fun DistToOrigin(point: PointF): Float {
            return Math.sqrt((point.x * point.x + point.y * point.y).toDouble()).toFloat()
        }

        private fun Rho(line: Line): Float {
            return DistToOrigin(Subtract(line.p2, line.p1))
        }

        private fun CreateBezier(input: ArrayList<PointF>): ArrayList<PointF> {
            val DetailLevel2 = DETAIL_LEVEL.toFloat()
            val working = arrayOfNulls<PointF>(input.size)
            val output = ArrayList<PointF>()

            for (iteration in 0..DETAIL_LEVEL) {
                for (i in input.indices)
                    working[i] = PointF(input[i].x, input[i].y)

                for (level in 0 until input.size - 1)
                    for (i in 0 until input.size - level - 1) {
                        val lll = Lerp(working[i]!!, working[i + 1]!!, iteration.toFloat() / DetailLevel2)
                        working[i] = lll
                    }

                output.add(working[0]!!)
            }

            return output
        }

        private fun Lerp(pt1: PointF, pt2: PointF, weight: Float): PointF {
            if (weight > 1 || weight < 0) throw ArrayIndexOutOfBoundsException("weight")
            return Lerp(pt1.x, pt2.x, pt1.y, pt2.y, weight)
        }

        private fun Lerp(x1: Float, x2: Float, y1: Float, y2: Float, weight: Float): PointF {
            if (weight > 1 || weight < 0) throw ArrayIndexOutOfBoundsException("weight")
            return PointF(x1 + (x2 - x1) * weight, y1 + (y2 - y1) * weight)
        }

        private fun CatmullRom(value1: PointF, value2: PointF, value3: PointF, value4: PointF, amount: Float): PointF {
            val vector = PointF()
            val num = amount * amount
            val num2 = amount * num
            vector.x = 0.5f * ((((2f * value2.x) + ((-value1.x + value3.x) * amount)) + (((((2f * value1.x) - (5f * value2.x)) + (4f * value3.x)) - value4.x) * num)) + ((((-value1.x + (3f * value2.x)) - (3f * value3.x)) + value4.x) * num2))
            vector.y = 0.5f * ((((2f * value2.y) + ((-value1.y + value3.y) * amount)) + (((((2f * value1.y) - (5f * value2.y)) + (4f * value3.y)) - value4.y) * num)) + ((((-value1.y + (3f * value2.y)) - (3f * value3.y)) + value4.y) * num2))
            return vector
        }

        private fun MultiplyPt(value: PointF, scalar: Float): PointF {
            return PointF(value.x * scalar, value.y * scalar)
        }

        private fun CircleCenterPoint(point1: PointF, point2: PointF, point3: PointF): PointF {
            val center = PointF()
            val a = Math.pow(point1.x.toDouble(), 2.0) + Math.pow(point1.y.toDouble(), 2.0)
            val b = Math.pow(point2.x.toDouble(), 2.0) + Math.pow(point2.y.toDouble(), 2.0)
            val c = Math.pow(point3.x.toDouble(), 2.0) + Math.pow(point3.y.toDouble(), 2.0)
            val g = (point3.y - point2.y) * point1.x + (point1.y - point3.y) * point2.x + (point2.y - point1.y) * point3.x
            center.x = (((b - c) * point1.y + (c - a) * point2.y + (a - b) * point3.y) / (2 * g)).toFloat()
            center.y = (((c - b) * point1.x + (a - c) * point2.x + (b - a) * point3.x) / (2 * g)).toFloat()
            return center
        }

        private fun TwoPointSide(point1: PointF, point2: PointF): Float {
            return Math.sqrt(Math.pow((point1.x - point2.x).toDouble(), 2.0) + Math.pow((point1.y - point2.y).toDouble(), 2.0)).toFloat()
        }

        private fun CircleRadius(point1: PointF, point2: PointF, point3: PointF): Float {
            val a = TwoPointSide(point1, point2)
            val b = TwoPointSide(point2, point3)
            val c = TwoPointSide(point1, point3)
            return ((a * b * c) / Math.sqrt(((a + b + c) * (a + b - c) * (a - b + c) * (-a + b + c)).toDouble())).toFloat()
        }

        private fun CircularArc(startAng: Float, endAng: Float, circleCenter: PointF, radius: Float, t: Float): PointF {
            val vector = PointF()
            val ang = lerp(startAng, endAng, t)
            vector.x = (Math.cos(ang.toDouble()) * radius + circleCenter.x).toFloat()
            vector.y = (Math.sin(ang.toDouble()) * radius + circleCenter.y).toFloat()
            return vector
        }

        private fun isIn(a: Float, b: Float, c: Float): Boolean {
            return (b > a && b < c) || (b < a && b > c)
        }

        private fun lerp(a: Float, b: Float, t: Float): Float {
            return a * (1 - t) + b * t
        }

        @JvmStatic
        fun Add(pt1: PointF, sz2: PointF): PointF {
            return PointF(pt1.x + sz2.x, pt1.y + sz2.y)
        }

        @JvmStatic
        fun Subtract(pt1: PointF, sz2: PointF): PointF {
            return PointF(pt1.x - sz2.x, pt1.y - sz2.y)
        }

        @JvmStatic
        fun getCurveType(c: Char): CurveTypes {
            return when (c) {
                'L' -> CurveTypes.Linear
                'C' -> CurveTypes.Catmull
                'P' -> CurveTypes.PerfectCurve
                'B' -> CurveTypes.Bezier
                else -> CurveTypes.Bezier
            }
        }
    }
}
