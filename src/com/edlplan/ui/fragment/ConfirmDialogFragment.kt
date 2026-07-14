package com.edlplan.ui.fragment

import android.animation.Animator
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osuplus.R

class ConfirmDialogFragment : BaseFragment() {

    private var onResult: OnResult? = null

    @StringRes
    private var text: Int = 0

    private var messageString: String? = null

    init {
        isDismissOnBackgroundClick = true
    }

    override val layoutID: Int
        get() = R.layout.frgdialog_confirm

    override fun onLoadView() {
        findViewById<View>(R.id.okButton)!!.setOnClickListener {
            onResult?.onAccept(true)
            dismiss()
        }
        when {
            messageString != null -> {
                findViewById<TextView>(R.id.confirm_message)?.text = messageString
            }
            text != 0 -> {
                findViewById<TextView>(R.id.confirm_message)?.setText(text)
            }
        }
        playOnLoadAnim()
    }

    override fun dismiss() {
        playOnDismissAnim { super.dismiss() }
    }

    fun setMessage(@StringRes text: Int): ConfirmDialogFragment {
        this.text = text
        findViewById<TextView>(R.id.confirm_message)?.setText(text)
        return this
    }

    fun setMessage(text: String?): ConfirmDialogFragment {
        this.messageString = text
        findViewById<TextView>(R.id.confirm_message)?.text = text
        return this
    }

    fun showForResult(result: OnResult?) {
        this.onResult = result
        show()
    }

    protected fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.alpha = 0f
        body.translationY = 500f
        body.animate().cancel()
        body.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(280)
            .setInterpolator(EasingHelper.asInterpolator(Easing.OutCubic))
            .start()
        playBackgroundHideInAnim(200)
    }

    protected fun playOnDismissAnim(runnable: Runnable?) {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.animate().cancel()
        body.animate()
            .alpha(0f)
            .translationY(500f)
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

    fun interface OnResult {
        fun onAccept(isAccepted: Boolean)
    }
}
