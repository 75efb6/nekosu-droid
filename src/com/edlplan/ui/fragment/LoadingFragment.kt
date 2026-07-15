package com.edlplan.ui.fragment

import android.animation.Animator
import android.view.View
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osuplus.R

open class LoadingFragment : BaseFragment() {

    override val layoutID: Int
        get() = R.layout.fragment_loading

    override fun onLoadView() {
        playOnLoadAnim()
    }

    override fun dismiss() {
        playOnDismissAnim { super.dismiss() }
    }

    protected fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.alpha = 0f
        body.scaleX = 0.8f
        body.scaleY = 0.8f
        body.animate().cancel()
        body.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(EasingHelper.asInterpolator(Easing.OutBack))
            .start()
        playBackgroundHideInAnim(200)
    }

    protected fun playOnDismissAnim(runnable: Runnable?) {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.animate().cancel()
        body.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(180)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    runnable?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(180)
    }
}
