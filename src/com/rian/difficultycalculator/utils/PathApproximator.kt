package com.rian.difficultycalculator.utils

import com.rian.difficultycalculator.math.Precision
import com.rian.difficultycalculator.math.Vector2
import java.util.ArrayList
import java.util.Stack
import kotlin.UnsupportedOperationException

object PathApproximator {
    private const val catmullDetail = 50

    private const val bezierTolerance = 0.25f
    private const val circularArcTolerance = 0.1f

    @JvmStatic
    fun approximateBezier(controlPoints: List<Vector2>): ArrayList<Vector2> {
        val output = ArrayList<Vector2>()
        val count = controlPoints.size - 1

        if (count < 0) {
            return output
        }

        val toFlatten = Stack<Array<Vector2>>()
        val freeBuffers = Stack<Array<Vector2>>()

        toFlatten.push(controlPoints.toTypedArray())

        val subdivisionBuffer1 = arrayOfNulls<Vector2>(count + 1)
        val subdivisionBuffer2 = arrayOfNulls<Vector2>(count * 2 + 1)

        while (toFlatten.isNotEmpty()) {
            val parent = toFlatten.pop()

            if (bezierIsFlatEnough(parent)) {
                bezierApproximate(parent, output, subdivisionBuffer1, subdivisionBuffer2, count + 1)
                freeBuffers.push(parent)
                continue
            }

            val rightChild = if (freeBuffers.isNotEmpty()) freeBuffers.pop() else arrayOfNulls<Vector2>(count + 1)

            bezierSubdivide(parent, subdivisionBuffer2 as Array<Vector2?>, rightChild as Array<Vector2?>, subdivisionBuffer1 as Array<Vector2?>, count + 1)

            System.arraycopy(subdivisionBuffer2, 0, parent, 0, count + 1)

            toFlatten.push(rightChild as Array<Vector2>)
            toFlatten.push(parent)
        }

        output.add(controlPoints[count])
        return output
    }

    @JvmStatic
    fun approximateCatmull(controlPoints: List<Vector2>): ArrayList<Vector2> {
        val result = ArrayList<Vector2>()

        for (i in 0 until controlPoints.size - 1) {
            val v1 = if (i > 0) controlPoints[i - 1] else controlPoints[i]
            val v2 = controlPoints[i]
            val v3 = controlPoints[i + 1]
            val v4 = if (i < controlPoints.size - 2) controlPoints[i + 2] else v3.add(v3).subtract(v2)

            for (c in 0 until catmullDetail) {
                result.add(catmullFindPoint(v1, v2, v3, v4, c.toFloat() / catmullDetail))
                result.add(catmullFindPoint(v1, v2, v3, v4, (c + 1).toFloat() / catmullDetail))
            }
        }

        return result
    }

    @JvmStatic
    fun approximateCircularArc(controlPoints: List<Vector2>): ArrayList<Vector2> {
        val a = controlPoints[0]
        val b = controlPoints[1]
        val c = controlPoints[2]

        if (Precision.almostEqualsNumber(0.0, ((b.y - a.y) * (c.x - a.x) - (b.x - a.x) * (c.y - a.y)).toDouble())) {
            return approximateBezier(controlPoints)
        }

        val d = 2 * (a.x * b.subtract(c).y + b.x * c.subtract(a).y + c.x * a.subtract(b).y)
        val aSq = a.getLengthSquared()
        val bSq = b.getLengthSquared()
        val cSq = c.getLengthSquared()

        val center = Vector2(
            aSq * b.subtract(c).y + bSq * c.subtract(a).y + cSq * a.subtract(b).y,
            aSq * c.subtract(b).x + bSq * a.subtract(c).x + cSq * b.subtract(a).x
        ).divide(d)

        val dA = a.subtract(center)
        val dC = c.subtract(center)

        val radius = dA.getLength()

        var thetaStart = Math.atan2(dA.y.toDouble(), dA.x.toDouble())
        var thetaEnd = Math.atan2(dC.y.toDouble(), dC.x.toDouble())

        while (thetaEnd < thetaStart) {
            thetaEnd += 2 * Math.PI
        }

        var direction = 1.0
        var thetaRange = thetaEnd - thetaStart

        var orthoAtoC = c.subtract(a)
        orthoAtoC = Vector2(orthoAtoC.y, -orthoAtoC.x)
        if (orthoAtoC.dot(b.subtract(a)) < 0) {
            direction = -direction
            thetaRange = 2 * Math.PI - thetaRange
        }

        val amountPoints = if (2 * radius <= circularArcTolerance)
            2
        else
            Math.max(2, Math.ceil(thetaRange / (2 * Math.acos(1 - circularArcTolerance.toDouble() / radius.toDouble()))).toInt())

        val output = ArrayList<Vector2>()

        for (i in 0 until amountPoints) {
            val fraction = i.toDouble() / (amountPoints - 1)
            val theta = thetaStart + direction * fraction * thetaRange
            val o = Vector2(Math.cos(theta).toFloat(), Math.sin(theta).toFloat()).scale(radius)
            output.add(center.add(o))
        }

        return output
    }

    @JvmStatic
    fun approximateLinear(controlPoints: List<Vector2>): List<Vector2> {
        return controlPoints
    }

    private fun bezierIsFlatEnough(controlPoints: Array<Vector2>): Boolean {
        for (i in 1 until controlPoints.size - 1) {
            val prev = controlPoints[i - 1]
            val current = controlPoints[i]
            val next = controlPoints[i + 1]

            val finalVec = prev.subtract(current.scale(2f)).add(next)

            if (Math.pow(finalVec.getLength().toDouble(), 2.0) > Math.pow(bezierTolerance.toDouble(), 2.0) * 4) {
                return false
            }
        }

        return true
    }

    private fun bezierApproximate(
        controlPoints: Array<Vector2>, output: ArrayList<Vector2>,
        subdivisionBuffer1: Array<Vector2?>, subdivisionBuffer2: Array<Vector2?>,
        count: Int
    ) {
        bezierSubdivide(controlPoints, subdivisionBuffer2, subdivisionBuffer1, subdivisionBuffer1, count)

        System.arraycopy(subdivisionBuffer1, 1, subdivisionBuffer2, count, count - 1)

        output.add(controlPoints[0])

        for (i in 1 until count - 1) {
            val index = 2 * i
            val p = subdivisionBuffer2[index - 1]!!
                .add(subdivisionBuffer2[index]!!.scale(2f))
                .add(subdivisionBuffer2[index + 1]!!)
                .scale(0.25f)
            output.add(p)
        }
    }

    private fun bezierSubdivide(
        controlPoints: Array<Vector2>, l: Array<Vector2?>, r: Array<Vector2?>,
        subdivisionBuffer: Array<Vector2?>, count: Int
    ) {
        System.arraycopy(controlPoints, 0, subdivisionBuffer, 0, count)

        for (i in 0 until count) {
            l[i] = subdivisionBuffer[0]
            r[count - i - 1] = subdivisionBuffer[count - i - 1]

            for (j in 0 until count - i - 1) {
                subdivisionBuffer[j] = subdivisionBuffer[j]!!.add(subdivisionBuffer[j + 1]!!).divide(2f)
            }
        }
    }

    private fun catmullFindPoint(vec1: Vector2, vec2: Vector2, vec3: Vector2, vec4: Vector2, t: Float): Vector2 {
        val t2 = Math.pow(t.toDouble(), 2.0).toFloat()
        val t3 = Math.pow(t.toDouble(), 3.0).toFloat()

        return Vector2(
            0.5f *
                    (2 * vec2.x +
                            (-vec1.x + vec3.x) * t +
                            (2 * vec1.x - 5 * vec2.x + 4 * vec3.x - vec4.x) * t2 +
                            (-vec1.x + 3 * vec2.x - 3 * vec3.x + vec4.x) * t3),
            0.5f *
                    (2 * vec2.y +
                            (-vec1.y + vec3.y) * t +
                            (2 * vec1.y - 5 * vec2.y + 4 * vec3.y - vec4.y) * t2 +
                            (-vec1.y + 3 * vec2.y - 3 * vec3.y + vec4.y) * t3)
        )
    }
}
