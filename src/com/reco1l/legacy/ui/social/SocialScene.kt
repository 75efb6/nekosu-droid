package com.reco1l.legacy.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.legacy.ui.social.FriendsListFragment
import com.reco1l.legacy.ui.social.ConversationsFragment
import com.reco1l.legacy.ui.social.GlobalChatFragment
import ru.nsu.ccfit.zuev.osuplus.R

class SocialScene : BaseFragment() {

    override val layoutID = R.layout.social_scene

    private var currentTab = 0
    private var friendsFragment: FriendsListFragment? = null
    private var conversationsFragment: ConversationsFragment? = null
    private var globalChatFragment: GlobalChatFragment? = null

    override fun onLoadView() {
        val tabFriends = findViewById<TextView>(R.id.tabFriends)
        val tabMessages = findViewById<TextView>(R.id.tabMessages)
        val tabGlobal = findViewById<TextView>(R.id.tabGlobal)
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)

        findViewById<View>(R.id.backButton)?.setOnClickListener { dismiss() }
        findViewById<View>(R.id.searchButton)?.setOnClickListener {
            UserSearch().show(childFragmentManager, "user_search")
        }

        tabFriends?.setOnClickListener { switchTab(0) }
        tabMessages?.setOnClickListener { switchTab(1) }
        tabGlobal?.setOnClickListener { switchTab(2) }

        switchTab(0)
        playOnLoadAnim()
    }

    private fun switchTab(index: Int) {
        currentTab = index

        val tabFriends = findViewById<TextView>(R.id.tabFriends)
        val tabMessages = findViewById<TextView>(R.id.tabMessages)
        val tabGlobal = findViewById<TextView>(R.id.tabGlobal)
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame) ?: return

        // Reset tab colors
        tabFriends?.setTextColor(0xFF9999BB.toInt())
        tabMessages?.setTextColor(0xFF9999BB.toInt())
        tabGlobal?.setTextColor(0xFF9999BB.toInt())

        // Remove current fragment
        val ft = childFragmentManager.beginTransaction()
        friendsFragment?.let { ft.remove(it) }
        conversationsFragment?.let { ft.remove(it) }
        globalChatFragment?.let { ft.remove(it) }

        when (index) {
            0 -> {
                tabFriends?.setTextColor(0xFFE63E8C.toInt())
                if (friendsFragment == null) friendsFragment = FriendsListFragment()
                ft.add(R.id.contentFrame, friendsFragment!!)
            }
            1 -> {
                tabMessages?.setTextColor(0xFFE63E8C.toInt())
                conversationsFragment = ConversationsFragment()
                ft.add(R.id.contentFrame, conversationsFragment!!)
            }
            2 -> {
                tabGlobal?.setTextColor(0xFFE63E8C.toInt())
                if (globalChatFragment == null) globalChatFragment = GlobalChatFragment()
                ft.add(R.id.contentFrame, globalChatFragment!!)
            }
        }
        ft.commitAllowingStateLoss()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.alpha = 0f
        body.translationY = 200f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(com.edlplan.ui.EasingHelper.asInterpolator(com.edlplan.framework.easing.Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    override fun dismiss() {
        val body = findViewById<View>(R.id.fullLayout)
        if (body == null) {
            super@SocialScene.dismiss()
            return
        }
        body.animate().cancel()
        body.animate()
            .translationY(200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(com.edlplan.ui.EasingHelper.asInterpolator(com.edlplan.framework.easing.Easing.InOutQuad))
            .setListener(object : com.edlplan.ui.BaseAnimationListener() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isAdded) super@SocialScene.dismiss()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    companion object {
        @JvmStatic
        fun open() {
            SocialScene().show()
        }
    }
}
