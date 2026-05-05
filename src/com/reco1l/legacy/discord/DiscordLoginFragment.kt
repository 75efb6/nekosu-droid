package com.reco1l.legacy.discord

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.preference.PreferenceManager
import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osuplus.R

class DiscordLoginFragment : BaseFragment() {

    override val layoutID = R.layout.fragment_discord_login

    @SuppressLint("SetJavaScriptEnabled")
    override fun onLoadView() {
        findViewById<android.view.View>(R.id.discord_close)?.setOnClickListener { dismiss() }

        val webView = findViewById<WebView>(R.id.discord_webview) ?: return

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Spoof a desktop UA so Discord serves the full web client
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val auth = request.requestHeaders["Authorization"]
                if (!auth.isNullOrBlank() && !auth.startsWith("Bot ") && auth.length > 30) {
                    saveTokenAndDismiss(auth)
                }
                return null
            }
        }

        webView.loadUrl("https://discord.com/login")
    }

    private fun saveTokenAndDismiss(token: String) {
        val ctx = context ?: return
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .edit()
            .putString("discordToken", token)
            .apply()
        Config.setDiscordToken(token)
        activity?.runOnUiThread { dismiss() }
    }
}
