package com.rian.difficultycalculator.math

object Precision {
    const val FLOAT_EPSILON = 1e-3f
    const val DOUBLE_EPSILON = 1e-7

    @JvmStatic
    fun almostEqualsNumber(value1: Float, value2: Float): Boolean {
        return almostEqualsNumber(value1, value2, FLOAT_EPSILON)
    }

    @JvmStatic
    fun almostEqualsNumber(value1: Float, value2: Float, acceptableDifference: Float): Boolean {
        return Math.abs(value1 - value2) <= acceptableDifference
    }

    @JvmStatic
    fun almostEqualsNumber(value1: Double, value2: Double): Boolean {
        return almostEqualsNumber(value1, value2, DOUBLE_EPSILON)
    }

    @JvmStatic
    fun almostEqualsNumber(value1: Double, value2: Double, acceptableDifference: Double): Boolean {
        return Math.abs(value1 - value2) <= acceptableDifference
    }
}
