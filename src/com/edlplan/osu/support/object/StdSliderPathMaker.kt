package com.edlplan.osu.support.`object`

import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.LinePath
import com.edlplan.framework.math.line.approximator.BezierApproximator
import com.edlplan.framework.math.line.approximator.CatmullApproximator
import com.edlplan.framework.math.line.approximator.CircleApproximator

class StdSliderPathMaker(private val slider: StdPath) {

    private val path = LinePath()

    fun getControlPoint(): List<Vec2> = slider.controlPoints

    fun calculateSubPath(subPoints: List<Vec2>): List<Vec2> {
        return when (slider.type) {
            StdPath.Type.Linear -> subPoints
            StdPath.Type.Perfect -> {
                if (getControlPoint().size != 3 || subPoints.size != 3) {
                    BezierApproximator(subPoints).createBezier()
                } else {
                    val sub = CircleApproximator(subPoints[0], subPoints[1], subPoints[2]).createArc()
                    if (sub.isNotEmpty()) sub else BezierApproximator(subPoints).createBezier()
                }
            }
            StdPath.Type.Catmull -> CatmullApproximator(subPoints).createCatmull()
            else -> BezierApproximator(subPoints).createBezier()
        }
    }

    fun calculatePath(): LinePath {
        path.clear()
        val subControlPoints = ArrayList<Vec2>()
        for (i in getControlPoint().indices) {
            subControlPoints.add(getControlPoint()[i])
            if (i == getControlPoint().size - 1 || getControlPoint()[i] == getControlPoint()[i + 1]) {
                val subPath = calculateSubPath(subControlPoints)
                for (v in subPath) {
                    if (path.size() == 0 || path.last() != v) {
                        path.add(v)
                    }
                }
                subControlPoints.clear()
            }
        }
        return path
    }
}
