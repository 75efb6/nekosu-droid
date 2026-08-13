package com.edlplan.ui.fragment

import android.animation.Animator
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import club.andnext.markdown.MarkdownWebView
import ru.nsu.ccfit.zuev.osuplus.R

class MarkdownFragment : BaseFragment() {
    private var markdown: String? = null

    @StringRes
    private var title: Int = 0

    init {
        isDismissOnBackgroundClick = false
        isDismissOnBackPress = true
    }

    override val layoutID: Int
        get() = R.layout.fragment_markdown

    override fun onLoadView() {
        if (markdown != null) {
            findViewById<MarkdownWebView>(R.id.markdown_view)?.setText(markdown)
        }
        if (title != 0) {
            findViewById<TextView>(R.id.title)?.setText(title)
        }
        findViewById<View>(R.id.frg_close)?.setOnClickListener { dismiss() }
        playOnLoadAnim()
    }

    override fun dismiss() {
        playOnDismissAnim { super.dismiss() }
    }

    fun setMarkdown(markdown: String?): MarkdownFragment {
        this.markdown = markdown
        findViewById<MarkdownWebView>(R.id.markdown_view)?.setText(markdown)
        return this
    }

    fun setTitle(@StringRes title: Int): MarkdownFragment {
        this.title = title
        findViewById<TextView>(R.id.title)?.setText(title)
        return this
    }

    protected fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.translationY = 500f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.OutQuad))
            .start()
        playBackgroundHideInAnim(200)
    }

    protected fun playOnDismissAnim(runnable: Runnable?) {
        val body = findViewById<View>(R.id.frg_body) ?: return
        body.animate().cancel()
        body.animate()
            .translationY(500f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    runnable?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }
}
