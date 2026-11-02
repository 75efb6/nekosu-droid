package com.reco1l.legacy.ui.social

import android.animation.Animator
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.server.SocialAPI
import com.reco1l.api.server.ServerAPI
import com.reco1l.framework.lang.async
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class ChatScene : BaseFragment() {

    override val layoutID = R.layout.chat_scene

    private var partnerId = 0L
    private var partnerName = ""
    private val messages = mutableListOf<JSONObject>()

    private var chatText: TextView? = null
    private var scrollView: ScrollView? = null
    private var chatField: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        partnerId = arguments?.getLong("partnerId", 0) ?: 0
        partnerName = arguments?.getString("partnerName", "") ?: ""
    }

    override fun onLoadView() {
        chatText = findViewById(R.id.chatText)
        scrollView = findViewById(R.id.scrollView)
        chatField = findViewById(R.id.chatField)

        chatText?.movementMethod = ScrollingMovementMethod()

        findViewById<TextView>(R.id.chatPartnerName)?.text = partnerName
        findViewById<View>(R.id.backButton)?.setOnClickListener { dismiss() }

        val sendButton = findViewById<TextView>(R.id.sendButton)
        sendButton?.setOnClickListener { sendMessage() }

        chatField?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        // Listen for incoming messages
        ServerAPI.on("message_received") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val senderId = data.optLong("senderId", 0)
            if (senderId == partnerId) {
                activity?.runOnUiThread {
                    appendMessage(data)
                    scrollToBottom()
                }
            }
        }

        loadMessages()
        playOnLoadAnim()
    }

    private fun loadMessages() {
        val userId = OnlineManager.getInstance().userId
        async {
            val result = SocialAPI.getMessages(userId, partnerId) ?: return@async
            if (result.optString("status") != "success") return@async

            messages.clear()
            val arr = result.optJSONArray("messages")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    messages.add(arr.getJSONObject(i))
                }
            }

            // Mark as read
            SocialAPI.markRead(userId, partnerId)

            activity?.runOnUiThread {
                renderMessages()
                scrollToBottom()
            }
        }
    }

    private fun renderMessages() {
        val userId = OnlineManager.getInstance().userId
        val sb = StringBuilder()
        for (msg in messages) {
            val senderId = msg.optLong("senderId", 0)
            val senderName = msg.optString("senderName", "Unknown")
            val content = msg.optString("content", "")
            val color = if (senderId == userId) "#E63E8C" else "#FFFFFF"
            sb.append("<font color=$color><b>$senderName:</b></font> ")
            sb.append("<font color=#FFFFFF>$content</font>\n")
        }
        chatText?.text = android.text.Html.fromHtml(sb.toString(), android.text.Html.FROM_HTML_MODE_COMPACT)
    }

    private fun appendMessage(msg: JSONObject) {
        val senderId = msg.optLong("senderId", 0)
        val senderName = msg.optString("senderName", "Unknown")
        val content = msg.optString("content", "")
        val userId = OnlineManager.getInstance().userId
        val color = if (senderId == userId) "#E63E8C" else "#FFFFFF"

        val newPart = "<font color=$color><b>$senderName:</b></font> <font color=#FFFFFF>$content</font>\n"
        val current = chatText?.text?.toString() ?: ""
        chatText?.text = android.text.Html.fromHtml(current + newPart, android.text.Html.FROM_HTML_MODE_COMPACT)
    }

    private fun sendMessage() {
        val content = chatField?.text?.toString()?.trim() ?: return
        if (content.isEmpty()) return

        val userId = OnlineManager.getInstance().userId
        async {
            val result = SocialAPI.sendMessage(userId, partnerId, content)
            activity?.runOnUiThread {
                if (result?.optString("status") == "success") {
                    chatField?.text?.clear()
                    // Add message locally
                    val localMsg = JSONObject().apply {
                        put("senderId", userId)
                        put("senderName", OnlineManager.getInstance().username)
                        put("content", content)
                    }
                    messages.add(localMsg)
                    appendMessage(localMsg)
                    scrollToBottom()
                } else {
                    Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scrollToBottom() {
        scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.alpha = 0f
        body.translationY = 200f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    override fun dismiss() {
        val body = findViewById<View>(R.id.fullLayout)
        if (body == null) {
            super@ChatScene.dismiss()
            return
        }
        body.animate().cancel()
        body.animate()
            .translationY(200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isAdded) super@ChatScene.dismiss()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    companion object {
        fun newInstance(partnerId: Long, partnerName: String): ChatScene {
            return ChatScene().apply {
                arguments = Bundle().apply {
                    putLong("partnerId", partnerId)
                    putString("partnerName", partnerName)
                }
            }
        }
    }
}
