package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.math.MathUtils
import com.rian.difficultycalculator.utils.PathApproximator
import com.rian.difficultycalculator.math.Precision
import com.rian.difficultycalculator.math.Vector2

class SliderPath {
    @JvmField
    val pathType: SliderPathType

    @JvmField
    val controlPoints: ArrayList<Vector2>

    @JvmField
    val expectedDistance: Double

    @JvmField
    val calculatedPath: ArrayList<Vector2> = ArrayList()

    @JvmField
    val cumulativeLength: ArrayList<Double> = ArrayList()

    constructor(type: SliderPathType, controlPoints: ArrayList<Vector2>, expectedDistance: Double) {
        this.pathType = type
        this.controlPoints = controlPoints
        this.expectedDistance = expectedDistance

        calculatePath()
        calculateCumulativeLength()
    }

    private constructor(source: SliderPath) {
        pathType = source.pathType
        expectedDistance = source.expectedDistance
        controlPoints = ArrayList()

        for (point in source.controlPoints) {
            controlPoints.add(Vector2(point.x, point.y))
        }

        for (point in source.calculatedPath) {
            calculatedPath.add(Vector2(point.x, point.y))
        }

        cumulativeLength.addAll(source.cumulativeLength)
    }

    fun positionAt(progress: Double): Vector2 {
        val d = progressToDistance(progress)

        return interpolateVertices(indexOfDistance(d), d)
    }

    fun deepClone(): SliderPath {
        return SliderPath(this)
    }

    private fun calculatePath() {
        calculatedPath.clear()

        if (controlPoints.isEmpty()) {
            return
        }

        calculatedPath.add(controlPoints[0])

        var spanStart = 0

        for (i in controlPoints.indices) {
            if (i == controlPoints.size - 1 || controlPoints[i] == controlPoints[i + 1]) {
                val spanEnd = i + 1
                val subPath = calculateSubPath(controlPoints.subList(spanStart, spanEnd))

                val skipFirst = if (calculatedPath.isNotEmpty() && subPath.isNotEmpty() && calculatedPath.last() == subPath[0]) 1 else 0

                for (t in subPath.subList(skipFirst, subPath.size)) {
                    if (calculatedPath.isEmpty() || calculatedPath.last() != t) {
                        calculatedPath.add(t)
                    }
                }

                spanStart = spanEnd
            }
        }
    }

    private fun calculateCumulativeLength() {
        cumulativeLength.clear()
        cumulativeLength.add(0.0)

        var calculatedLength = 0.0

        for (i in 0 until calculatedPath.size - 1) {
            val diff = calculatedPath[i + 1].subtract(calculatedPath[i])
            calculatedLength += diff.getLength().toDouble()
            cumulativeLength.add(calculatedLength)
        }

        if (calculatedLength != expectedDistance) {
            if (calculatedPath.size >= 2 &&
                calculatedPath[calculatedPath.size - 1] == calculatedPath[calculatedPath.size - 2] &&
                expectedDistance > calculatedLength
            ) {
                return
            }

            cumulativeLength.removeAt(cumulativeLength.size - 1)
            var pathEndIndex = calculatedPath.size - 1

            if (calculatedLength > expectedDistance) {
                while (cumulativeLength.isNotEmpty() && cumulativeLength.last() >= expectedDistance) {
                    cumulativeLength.removeAt(cumulativeLength.size - 1)
                    calculatedPath.removeAt(pathEndIndex--)
                }
            }

            if (pathEndIndex <= 0) {
                cumulativeLength.add(0.0)
                return
            }

            val dir = calculatedPath[pathEndIndex].subtract(calculatedPath[pathEndIndex - 1])
            dir.normalize()

            calculatedPath[pathEndIndex] =
                calculatedPath[pathEndIndex - 1].add(dir.scale((expectedDistance - cumulativeLength.last()).toFloat()))

            cumulativeLength.add(expectedDistance)
        }
    }

    private fun calculateSubPath(subControlPoints: List<Vector2>): List<Vector2> {
        return when (pathType) {
            SliderPathType.Linear -> PathApproximator.approximateLinear(subControlPoints)
            SliderPathType.PerfectCurve -> {
                if (subControlPoints.size != 3) {
                    return PathApproximator.approximateBezier(subControlPoints)
                }

                val subPath = PathApproximator.approximateCircularArc(subControlPoints)

                if (subPath.isEmpty()) {
                    return PathApproximator.approximateBezier(subControlPoints)
                }

                subPath
            }
            SliderPathType.Catmull -> PathApproximator.approximateCatmull(subControlPoints)
            else -> PathApproximator.approximateBezier(subControlPoints)
        }
    }

    private fun progressToDistance(progress: Double): Double {
        return MathUtils.clamp(progress, 0.0, 1.0) * expectedDistance
    }

    private fun interpolateVertices(i: Int, d: Double): Vector2 {
        if (calculatedPath.isEmpty()) {
            return Vector2(0f)
        }

        if (i <= 0) {
            return calculatedPath[0]
        }
        if (i >= calculatedPath.size) {
            return calculatedPath[calculatedPath.size - 1]
        }

        val p0 = calculatedPath[i - 1]
        val p1 = calculatedPath[i]

        val d0 = cumulativeLength[i - 1]
        val d1 = cumulativeLength[i]

        if (Precision.almostEqualsNumber(d0, d1)) {
            return p0
        }

        val w = (d - d0) / (d1 - d0)
        return p0.add(p1.subtract(p0).scale(w.toFloat()))
    }

    private fun indexOfDistance(d: Double): Int {
        if (cumulativeLength.isEmpty() || d < cumulativeLength[0]) {
            return 0
        }

        if (d >= cumulativeLength[cumulativeLength.size - 1]) {
            return cumulativeLength.size
        }

        var l = 0
        var r = cumulativeLength.size - 2

        while (l <= r) {
            val pivot = l + ((r - l) shr 1)
            val length = cumulativeLength[pivot]

            if (length < d) {
                l = pivot + 1
            } else if (length > d) {
                r = pivot - 1
            } else {
                return pivot
            }
        }

        return l
    }
}
