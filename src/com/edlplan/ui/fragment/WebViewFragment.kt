package com.edlplan.ui.fragment

import android.animation.Animator
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class WebViewFragment : BaseFragment() {

    private var webview: WebView? = null
    private var url: String? = null
    private var loadingFragment: LoadingFragment? = null

    fun setURL(url: String?): WebViewFragment {
        this.url = url
        return this
    }

    override val layoutID: Int
        get() = R.layout.fragment_webview

    override fun onLoadView() {
        webview = findViewById(R.id.web)
        val webSettings: WebSettings = webview!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.userAgentString = "osudroid"

        findViewById<ImageButton>(R.id.close_button)!!.setOnClickListener {
            dismiss()
        }

        webview!!.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                if (loadingFragment == null) {
                    loadingFragment = LoadingFragment()
                    loadingFragment!!.show()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                loadingFragment?.dismiss()
                loadingFragment = null
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
        }
        webview!!.loadUrl(url ?: "")
        playOnLoadAnim()
    }

    override fun dismiss() {
        playOnDismissAnim { super.dismiss() }
    }

    override fun callDismissOnBackPress() {
        if (webview?.canGoBack() == true) {
            webview?.goBack()
        } else {
            dismiss()
        }
    }

    protected fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.translationY = 100f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .start()
        playBackgroundHideInAnim(200)
    }

    protected fun playOnDismissAnim(runnable: Runnable?) {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.animate().cancel()
        body.animate()
            .translationY(100f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    runnable?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    companion object {
        @JvmField
        val PROFILE_URL: String = "https://" + OnlineManager.HOSTNAME + "/user/profile?id="
    }
}
