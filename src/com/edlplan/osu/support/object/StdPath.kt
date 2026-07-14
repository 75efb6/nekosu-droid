package com.edlplan.osu.support.`object`

import com.edlplan.framework.math.Vec2

class StdPath {
    var type: Type? = null

    var controlPoints: MutableList<Vec2> = ArrayList()

    fun addControlPoint(p: Vec2) {
        controlPoints.add(p)
    }

    fun addControlPoint(x: Float, y: Float) {
        addControlPoint(Vec2(x, y))
    }

    enum class Type(val tag: String) {
        Linear("L"),
        Perfect("P"),
        Bezier("B"),
        Catmull("C");

        fun getTag(): String = tag

        companion object {
            @JvmStatic
            fun forName(n: String): Type? = when (n) {
                "L" -> Linear
                "P" -> Perfect
                "B" -> Bezier
                "C" -> Catmull
                else -> null
            }
        }
    }
}
