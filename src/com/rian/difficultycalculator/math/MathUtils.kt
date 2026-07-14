package com.rian.difficultycalculator.math

object MathUtils {
    @JvmStatic
    fun clamp(num: Double, min: Double, max: Double): Double {
        return Math.max(min, Math.min(num, max))
    }

    @JvmStatic
    fun clamp(num: Float, min: Float, max: Float): Float {
        return Math.max(min, Math.min(num, max))
    }

    @JvmStatic
    fun clamp(num: Int, min: Int, max: Int): Int {
        return Math.max(min, Math.min(num, max))
    }
}
