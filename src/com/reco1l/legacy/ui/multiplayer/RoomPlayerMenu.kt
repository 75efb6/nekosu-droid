package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import com.edlplan.ui.fragment.ConfirmDialogFragment
import android.view.View
import android.widget.TextView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.RoomPlayer
import com.reco1l.legacy.Multiplayer
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.GlobalManager
import com.reco1l.api.server.SocialAPI
import com.reco1l.api.server.SpectateAPI
import com.reco1l.legacy.ui.social.ChatScene
import com.reco1l.legacy.ui.social.ProfilePopup
import com.reco1l.legacy.ui.spectate.SpectateScene
import com.reco1l.framework.lang.async
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.ToastLogger

class RoomPlayerMenu : BaseFragment()
{

    override val layoutID = R.layout.multiplayer_room_player_menu


    var player: RoomPlayer? = null


    init
    {
        isDismissOnBackgroundClick = true
    }


    override fun onLoadView()
    {
        if (player == null)
        {
            dismiss()
            return
        }

        findViewById<TextView>(R.id.room_player)!!.text = player!!.name

        // View profile -> open in-app profile popup
        findViewById<View>(R.id.room_profile)!!.setOnClickListener {
            val profile = ProfilePopup.newInstance(player!!.id.toLong())
            profile.show()
            dismiss()
        }

        // Send message
        findViewById<View>(R.id.room_send_message)!!.setOnClickListener {
            val chat = ChatScene.newInstance(player!!.id.toLong(), player!!.name)
            chat.show()
            dismiss()
        }

        // Spectate
        val spectateBtn = findViewById<View>(R.id.room_spectate)!!
        spectateBtn.setOnClickListener {
            SpectateAPI.startSpectating(player!!.id.toLong())
            val overlay = SpectateScene.newInstance(player!!.id.toLong(), player!!.name)
            overlay.show()
            dismiss()
        }

        // Add friend
        val addFriendBtn = findViewById<View>(R.id.room_add_friend)!!
        val userId = OnlineManager.getInstance().userId
        async {
            val result = SocialAPI.getFriends(userId)
            val friends = result?.optJSONArray("friends") ?: org.json.JSONArray()
            var isFriend = false
            for (i in 0 until friends.length()) {
                if (friends.optJSONObject(i)?.optLong("id", 0) == player!!.id.toLong()) {
                    isFriend = true
                    break
                }
            }
            GlobalManager.getInstance().getMainActivity()?.runOnUiThread {
                if (isFriend) {
                    addFriendBtn.visibility = View.INVISIBLE
                }
            }
        }
        addFriendBtn.setOnClickListener {
            async {
                SocialAPI.addFriend(userId, player!!.id.toLong())
                GlobalManager.getInstance().getMainActivity()?.runOnUiThread {
                    ToastLogger.showText("Friend request sent!", false)
                    addFriendBtn.visibility = View.INVISIBLE
                }
            }
        }

        // Mute
        val muteText = findViewById<TextView>(R.id.mute_text)!!

        fun updateText()
        {
            if (player!!.isMuted)
                muteText.text = "Unmute"
            else
                muteText.text = "Mute"
        }

        updateText()

        findViewById<View>(R.id.room_mute)!!.setOnClickListener {
            player!!.isMuted = !player!!.isMuted
            updateText()
        }

        // Make host
        val kick = findViewById<View>(R.id.room_kick)!!
        val host = findViewById<View>(R.id.room_host)!!

        if (!Multiplayer.isRoomHost)
        {
            kick.visibility = View.INVISIBLE
            host.visibility = View.INVISIBLE
        }

        kick.setOnClickListener {
            ConfirmDialogFragment()
                .setMessage("Kick ${player!!.name}?\nAre you sure?")
                .showForResult {
                    dismiss()
                    if (Multiplayer.isConnected) RoomAPI.kickPlayer(player!!.id)
                }
        }

        host.setOnClickListener {
            ConfirmDialogFragment()
                .setMessage("Make ${player!!.name} room host?\nAre you sure?")
                .showForResult {
                    dismiss()
                    if (Multiplayer.isConnected) RoomAPI.setRoomHost(player!!.id)
                }
        }

        // Block user
        findViewById<View>(R.id.room_block)!!.setOnClickListener {
            ConfirmDialogFragment()
                .setMessage("Block ${player!!.name}?\nThey won't be able to message you.")
                .showForResult {
                    async {
                        SocialAPI.blockUser(userId, player!!.id.toLong())
                        GlobalManager.getInstance().getMainActivity()?.runOnUiThread {
                            ToastLogger.showText("User blocked", false)
                        }
                    }
                    dismiss()
                }
        }

        playOnLoadAnim()
    }

    // Code extracted from PropsMenuFragment class

    private fun playOnLoadAnim()
    {
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

    private fun playEndAnim(action: Runnable?)
    {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.animate().cancel()
        body.animate()
                .translationY(200f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setListener(object : BaseAnimationListener()
                             {
                                 override fun onAnimationEnd(animation: Animator)
                                 {
                                     action?.run()
                                 }
                             })
                .start()
        playBackgroundHideOutAnim(200)
    }

    override fun dismiss()
    {
        playEndAnim { super.dismiss() }
    }
}
