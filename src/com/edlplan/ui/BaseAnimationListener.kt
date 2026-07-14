package com.edlplan.ui

import android.animation.Animator

open class BaseAnimationListener : Animator.AnimatorListener {

    override fun onAnimationStart(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator) {}

    override fun onAnimationCancel(animation: Animator) {}

    override fun onAnimationRepeat(animation: Animator) {}
}
