package com.edlplan.ui

import android.view.animation.Interpolator
import com.edlplan.framework.easing.Easing
import com.edlplan.framework.easing.EasingManager

object EasingHelper {

    @JvmStatic
    fun asInterpolator(easing: Easing): Interpolator {
        return Interpolator { f -> EasingManager.apply(easing, f.toDouble()).toFloat() }
    }
}
