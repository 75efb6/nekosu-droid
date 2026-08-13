package com.rian.difficultycalculator.math

object Interpolation {
    @JvmStatic
    fun linear(start: Double, end: Double, amount: Double): Double {
        return start + (end - start) * amount
    }

    @JvmStatic
    fun linear(start: Float, end: Float, amount: Float): Float {
        return start + (end - start) * amount
    }
}
