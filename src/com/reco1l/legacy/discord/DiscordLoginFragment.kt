package com.reco1l.legacy.discord

import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osuplus.R

class DiscordLoginFragment : BaseFragment() {

    companion object {
        private const val TAG = "DiscordLogin"
    }

    override val layoutID = R.layout.fragment_discord_login

    override fun onLoadView() {
        val activity = requireActivity()
        DiscordRPC.connect(activity)

        DiscordRPC.setConnectionStateListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        DiscordRPC.setConnectionStateListener(null)
    }
}
