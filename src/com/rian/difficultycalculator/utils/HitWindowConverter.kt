package com.rian.difficultycalculator.utils

import kotlin.UnsupportedOperationException

object HitWindowConverter {
    @JvmStatic
    fun hitWindow300ToOD(value: Double): Float {
        return ((80 - value) / 6).toFloat()
    }

    @JvmStatic
    fun hitWindow100ToOD(value: Double): Float {
        return ((140 - value) / 8).toFloat()
    }

    @JvmStatic
    fun hitWindow50ToOD(value: Double): Float {
        return ((200 - value) / 10).toFloat()
    }

    @JvmStatic
    fun odToHitWindow300(od: Float): Double {
        return 80.0 - 6 * od
    }

    @JvmStatic
    fun odToHitWindow100(od: Float): Double {
        return 140.0 - 8 * od
    }

    @JvmStatic
    fun odToHitWindow50(od: Float): Double {
        return 200.0 - 10 * od
    }
}
