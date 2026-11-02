package com.reco1l.legacy.ui.social

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
import androidx.fragment.app.Fragment
import com.reco1l.api.server.GlobalChatAPI
import com.reco1l.api.server.ServerAPI
import com.reco1l.framework.lang.async
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class GlobalChatFragment : Fragment() {

    private var chatText: TextView? = null
    private var scrollView: ScrollView? = null
    private var chatField: EditText? = null
    private var connected = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.global_chat_scene, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatText = view.findViewById(R.id.chatText)
        scrollView = view.findViewById(R.id.scrollView)
        chatField = view.findViewById(R.id.chatField)

        chatText?.movementMethod = ScrollingMovementMethod()

        val sendButton = view.findViewById<TextView>(R.id.sendButton)
        sendButton?.setOnClickListener { sendMessage() }

        chatField?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        connectToGlobalChat()
    }

    private fun connectToGlobalChat() {
        if (connected) return

        val userId = OnlineManager.getInstance().userId
        val username = OnlineManager.getInstance().username

        // Register listeners BEFORE connecting so we don't miss the history event
        GlobalChatAPI.onHistory { args ->
            activity?.runOnUiThread {
                val arr = args.getOrNull(0) as? org.json.JSONArray ?: return@runOnUiThread
                val sb = StringBuilder()
                for (i in 0 until arr.length()) {
                    val msg = arr.optJSONObject(i) ?: continue
                    sb.append(formatMessage(msg))
                }
                chatText?.text = android.text.Html.fromHtml(sb.toString(), android.text.Html.FROM_HTML_MODE_COMPACT)
                scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }
            }
        }

        GlobalChatAPI.onMessage { msg ->
            activity?.runOnUiThread {
                val current = chatText?.text?.toString() ?: ""
                chatText?.text = android.text.Html.fromHtml(current + formatMessage(msg), android.text.Html.FROM_HTML_MODE_COMPACT)
                scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }

                // Mention detection
                val content = msg.optString("content", "")
                val senderName = msg.optString("senderName", "")
                val myUsername = OnlineManager.getInstance().username
                if (myUsername.isNotEmpty() && content.contains(myUsername, ignoreCase = true)) {
                    ru.nsu.ccfit.zuev.osu.ToastLogger.showText("Someone mentioned you in the global chat", false)
                }
            }
        }

        async {
            GlobalChatAPI.connect(userId, username)
            connected = GlobalChatAPI.isConnected
        }
    }

    private fun formatMessage(msg: JSONObject): String {
        val senderName = msg.optString("senderName", "Unknown")
        val content = msg.optString("content", "")
        return "<font color=#E63E8C><b>$senderName:</b></font> <font color=#FFFFFF>$content</font>\n"
    }

    private fun sendMessage() {
        val content = chatField?.text?.toString()?.trim() ?: return
        if (content.isEmpty()) return

        val userId = OnlineManager.getInstance().userId
        GlobalChatAPI.sendMessage(userId, content)
        chatField?.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        GlobalChatAPI.disconnect()
        connected = false
    }
}
