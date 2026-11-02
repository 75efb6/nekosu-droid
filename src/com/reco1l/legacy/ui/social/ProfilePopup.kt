package com.reco1l.legacy.ui.social

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.server.SocialAPI
import com.reco1l.api.server.SpectateAPI
import com.reco1l.framework.lang.async
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class ProfilePopup : BaseFragment() {

    override val layoutID = R.layout.profile_popup

    private var targetId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetId = arguments?.getLong("targetId", 0) ?: 0
    }

    override fun onLoadView() {
        val fullLayout = findViewById<View>(R.id.fullLayout)
        val metrics = resources.displayMetrics
        val maxH = (metrics.heightPixels * 0.85).toInt()
        fullLayout?.let {
            it.post {
                if (it.height > maxH) {
                    val lp = it.layoutParams
                    lp.height = maxH
                    it.layoutParams = lp
                }
            }
        }

        findViewById<View>(R.id.closeButton)?.setOnClickListener { dismiss() }

        loadProfile()
        playOnLoadAnim()
    }

    private fun loadProfile() {
        val userId = OnlineManager.getInstance().userId

        async {
            val result = SocialAPI.getProfile(userId, targetId) ?: return@async
            if (result.optString("status") != "success") {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                return@async
            }

            val user = result.optJSONObject("user")
            val stats = result.optJSONObject("stats")
            val friendStatus = result.optString("friendStatus", "none")
            val note = result.optString("note", "")

            activity?.runOnUiThread {
                // User info
                findViewById<TextView>(R.id.profileName)?.text = user?.optString("username", "Unknown")
                findViewById<TextView>(R.id.profileCountry)?.text = user?.optString("country", "") ?: ""

                val online = user?.optBoolean("online", false) ?: false
                val onlineStatus = findViewById<TextView>(R.id.onlineStatus)
                if (online) {
                    onlineStatus?.text = user?.optString("onlineStatus", "Online")?.replaceFirstChar { it.uppercase() }
                    onlineStatus?.setTextColor(0xFF4CAF50.toInt())
                } else {
                    onlineStatus?.text = "Offline"
                    onlineStatus?.setTextColor(0xFF9999BB.toInt())
                }

                // Stats
                if (stats != null) {
                    findViewById<TextView>(R.id.statPP)?.text = "${stats.optInt("pp", 0)}"
                    findViewById<TextView>(R.id.statRank)?.text = "#${stats.optInt("rank", 0)}"
                    val acc = stats.optDouble("acc", 100.0)
                    findViewById<TextView>(R.id.statAcc)?.text = String.format("%.2f%%", acc)
                }

                // Note
                val noteField = findViewById<EditText>(R.id.noteField)
                noteField?.setText(note)

                // Friend button
                val friendButton = findViewById<TextView>(R.id.friendButton)
                when (friendStatus) {
                    "none" -> friendButton?.text = "Add Friend"
                    "pending" -> friendButton?.text = "Request Sent"
                    "accepted" -> friendButton?.text = "Remove Friend"
                    "incoming" -> friendButton?.text = "Accept Request"
                }
                friendButton?.setOnClickListener { handleFriendAction(friendStatus) }

                // Message button
                findViewById<View>(R.id.messageButton)?.setOnClickListener {
                    val name = user?.optString("username", "Unknown") ?: "Unknown"
                    val chatScene = ChatScene.newInstance(targetId, name)
                    chatScene.show()
                }

                // Spectate button (visible if target is online)
                val spectateButton = findViewById<TextView>(R.id.spectateButton)
                val isOnline = user?.optString("onlineStatus", "") in listOf("online", "playing")
                if (isOnline && targetId != OnlineManager.getInstance().userId && ru.nsu.ccfit.zuev.osu.Config.isAllowSpectating) {
                    spectateButton?.visibility = View.VISIBLE
                    spectateButton?.setOnClickListener {
                        val name = user?.optString("username", "Unknown") ?: "Unknown"
                        SpectateAPI.startSpectating(targetId)
                        val overlay = com.reco1l.legacy.ui.spectate.SpectateScene.newInstance(targetId, name)
                        overlay.show()
                        dismiss()
                    }
                } else {
                    spectateButton?.visibility = View.GONE
                }

                // Block button
                findViewById<View>(R.id.blockButton)?.setOnClickListener {
                    confirmBlock()
                }

                // Save note
                noteField?.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val newNote = noteField?.text?.toString()?.trim() ?: ""
                        saveNote(newNote)
                    }
                }

                // Load avatar
                loadAvatar(user?.optString("avatar", "") ?: "")
            }
        }
    }

    private fun loadAvatar(url: String) {
        if (url.isEmpty()) return
        async {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: return@async
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                activity?.runOnUiThread {
                    findViewById<ImageView>(R.id.avatarImage)?.setImageBitmap(bitmap)
                }
            } catch (_: Exception) {}
        }
    }

    private fun handleFriendAction(currentStatus: String) {
        val userId = OnlineManager.getInstance().userId
        async {
            when (currentStatus) {
                "none" -> SocialAPI.addFriend(userId, targetId)
                "pending" -> {} // Already sent
                "accepted" -> SocialAPI.removeFriend(userId, targetId)
                "incoming" -> SocialAPI.acceptFriend(userId, targetId)
            }
            loadProfile()
        }
    }

    private fun confirmBlock() {
        com.edlplan.ui.fragment.ConfirmDialogFragment()
            .setMessage("Block this user?\nThey won't be able to message you.")
            .showForResult {
                val userId = OnlineManager.getInstance().userId
                async {
                    SocialAPI.blockUser(userId, targetId)
                    activity?.runOnUiThread { dismiss() }
                }
            }
    }

    private fun saveNote(note: String) {
        val userId = OnlineManager.getInstance().userId
        async {
            SocialAPI.setProfileNote(userId, targetId, note)
        }
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
            super@ProfilePopup.dismiss()
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
                    if (isAdded) super@ProfilePopup.dismiss()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    companion object {
        fun newInstance(targetId: Long): ProfilePopup {
            return ProfilePopup().apply {
                arguments = Bundle().apply {
                    putLong("targetId", targetId)
                }
            }
        }
    }
}
