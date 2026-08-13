package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.*
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.AnimRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import android.text.InputType
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.SkinPathPreference
import com.edlplan.ui.fragment.LoadingFragment
import com.edlplan.ui.fragment.SettingsFragment
import com.reco1l.legacy.ui.StyledInputDialog
import com.reco1l.legacy.ui.StyledSelectionDialog
import com.reco1l.api.ibancho.LobbyAPI
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.RoomTeam
import com.reco1l.api.ibancho.data.TeamMode
import com.reco1l.api.ibancho.data.WinCondition
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.mainThread
import com.reco1l.legacy.Multiplayer
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.SkinManager


/**
 * Based on [SettingsMenu]
 */
class RoomOptions : SettingsFragment()
{

    private var rootScreen: PreferenceScreen? = null

    private var parentScreen: PreferenceScreen? = null

    private var isOnNestedScreen = false


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.multiplayer_player_settings, rootKey)

        findPreference<ListPreference>("player_team")!!.apply {

            isEnabled = Multiplayer.room!!.teamMode == TeamMode.TEAM_VS_TEAM
            value = Multiplayer.player!!.team?.ordinal?.toString()

            setOnPreferenceChangeListener { _, newValue ->

                val team = RoomTeam.from((newValue as String).toInt())

                RoomAPI.setPlayerTeam(team)
                true
            }
        }

        findPreference<CheckBoxPreference>("player_nightcore")!!.apply {

            setOnPreferenceChangeListener { _, newValue ->
                Config.setUseNightcoreOnMultiplayer(newValue as Boolean)
                RoomScene.onRoomModsChange(Multiplayer.room!!.mods)
                true
            }
        }

        if (Multiplayer.isRoomHost)
        {
            addPreferencesFromResource(R.xml.multiplayer_room_settings)

            findPreference<Preference>("room_link")!!.onPreferenceClickListener = OnPreferenceClickListener {

                val clipboard = requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                val link = "${LobbyAPI.INVITE_HOST}/${Multiplayer.room!!.id}/"
                val data = ClipData.newPlainText("Invitation link", link)

                clipboard.setPrimaryClip(data)
                ToastLogger.showText("Link copied to clipboard. If the room has a password, you can write it at the end of the link.", false)
                true
            }

            findPreference<EditTextPreference>("room_name")!!.apply {

                text = Multiplayer.room!!.name

                setOnPreferenceChangeListener { _, newValue ->

                    val newName = newValue as String

                    if (newName.isEmpty())
                        return@setOnPreferenceChangeListener false

                    RoomAPI.setRoomName(newName)
                    true
                }
            }

            findPreference<EditTextPreference>("room_password")!!.apply {

                text = null

                setOnPreferenceChangeListener { _, newValue ->
                    val password = newValue as String

                    RoomAPI.setRoomPassword(password)
                    true
                }
            }

            findPreference<CheckBoxPreference>("room_free_mods")!!.apply {

                isChecked = Multiplayer.room!!.gameplaySettings.isFreeMod

                setOnPreferenceChangeListener { _, newValue ->
                    val value = newValue as Boolean

                    RoomAPI.setRoomFreeMods(value)
                    true
                }
            }

            findPreference<CheckBoxPreference>("room_allowForceDifficultyStatistics")!!.apply {

                isChecked = Multiplayer.room!!.gameplaySettings.allowForceDifficultyStatistics

                setOnPreferenceChangeListener { _, newValue ->
                    val value = newValue as Boolean

                    RoomAPI.setRoomAllowForceDifficultyStatistics(value)
                    true
                }
            }

            findPreference<ListPreference>("room_versus_mode")!!.apply {

                value = Multiplayer.room!!.teamMode.ordinal.toString()

                setOnPreferenceChangeListener { _, newValue ->

                    val teamMode = TeamMode.from((newValue as String).toInt())

                    RoomAPI.setRoomTeamMode(teamMode)
                    true
                }
            }

            findPreference<ListPreference>("room_win_condition")!!.apply {

                value = Multiplayer.room!!.winCondition.ordinal.toString()

                setOnPreferenceChangeListener { _, newValue ->
                    val winCondition = WinCondition.from((newValue as String).toInt())

                    RoomAPI.setRoomWinCondition(winCondition)
                    true
                }
            }
        }

        loadGameSettings()
    }

    private fun loadGameSettings()
    {
        addPreferencesFromResource(R.xml.multiplayer_game_settings)

        findPreference<SkinPathPreference>("skinPath")!!.apply {

            reloadSkinList()
            setOnPreferenceChangeListener { _, newValue ->

                if (GlobalManager.getInstance().skinNow !== newValue.toString())
                {
                    val loading = LoadingFragment()
                    loading.show()

                    async {
                        GlobalManager.getInstance().skinNow = Config.getSkinPath()
                        SkinManager.getInstance().clearSkin()
                        ResourceManager.getInstance().loadSkin(newValue.toString())
                        GlobalManager.getInstance().engine?.textureManager?.reloadTextures()
                        GlobalManager.getInstance().engine?.fontManager?.reloadFonts()

                        LobbyScene.load()
                        RoomScene.load()
                        RoomScene.onRoomConnect(Multiplayer.room!!)

                        mainThread {
                            loading.dismiss()
                            activity?.startActivity(Intent(requireActivity(), MainActivity::class.java))
                        }
                    }
                }
                true
            }
        }

        // screens
        parentScreen = preferenceScreen
        rootScreen = parentScreen

        findPreference<PreferenceScreen>("general")!!.onPreferenceClickListener = OnPreferenceClickListener {
            preferenceScreen = it as PreferenceScreen
            true
        }

        findPreference<PreferenceScreen>("color")!!.onPreferenceClickListener = OnPreferenceClickListener {
            parentScreen = findPreference<Preference>("general") as PreferenceScreen
            preferenceScreen = it as PreferenceScreen
            true
        }

        findPreference<PreferenceScreen>("sound")!!.onPreferenceClickListener = OnPreferenceClickListener {
            preferenceScreen = it as PreferenceScreen
            true
        }

        findPreference<PreferenceScreen>("advancedopts")!!.onPreferenceClickListener = OnPreferenceClickListener {
            preferenceScreen = it as PreferenceScreen
            true
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen)
    {
        if (preferenceScreen.key != null)
        {
            if (!isOnNestedScreen)
            {
                isOnNestedScreen = true
                animateBackButton(R.drawable.back_black)
            }
            setTitle(preferenceScreen.title.toString())
            for (v in intArrayOf(android.R.id.list_container, R.id.title))
            {
                animateView(v, R.anim.slide_in_right)
            }
        }
    }

    private fun animateBackButton(@DrawableRes newDrawable: Int)
    {
        val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_360)

        animation.setAnimationListener(
                object : Animation.AnimationListener
                {
                    override fun onAnimationStart(animation: Animation?) = Unit
                    override fun onAnimationRepeat(animation: Animation?) = Unit

                    override fun onAnimationEnd(animation: Animation)
                    {
                        val backButton = findViewById<ImageButton>(R.id.back_button)
                        backButton!!.setImageDrawable(activity?.resources?.getDrawable(newDrawable, activity?.theme))
                    }
                }
        )

        findViewById<ImageButton>(R.id.back_button)!!.startAnimation(animation)
    }

    private fun animateView(@IdRes viewId: Int, @AnimRes anim: Int)
    {
        findViewById<View>(viewId)!!.startAnimation(AnimationUtils.loadAnimation(activity, anim))
    }

    private fun setTitle(title: String)
    {
        findViewById<TextView>(R.id.title)!!.text = title
    }

    override fun callDismissOnBackPress() = navigateBack()

    private fun navigateBack()
    {
        animateView(android.R.id.list_container, R.anim.slide_in_left)
        animateView(R.id.title, R.anim.slide_in_left)

        if (parentScreen!!.key != null)
        {
            preferenceScreen = parentScreen
            setTitle(parentScreen!!.title.toString())
            parentScreen = rootScreen
            return
        }

        if (isOnNestedScreen)
        {
            isOnNestedScreen = false
            animateBackButton(R.drawable.close_black)
            preferenceScreen = rootScreen
            setTitle(StringTable.get(R.string.menu_settings_title))
        }
        else dismiss()
    }

    override fun onDisplayPreferenceDialog(preference: Preference)
    {
        when (preference) {
            is ListPreference -> StyledSelectionDialog.show(requireContext(), preference)
            is EditTextPreference -> {
                val inputType = if (preference.key == "room_password")
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT
                StyledInputDialog.show(requireContext(), preference, inputType)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onLoadView()
    {
        findViewById<ImageButton>(R.id.back_button)!!.setOnClickListener { navigateBack() }
        findViewById<View>(R.id.frg_background)!!.setOnClickListener { dismiss() }
        playOnLoadAnim()
    }

    private fun playOnLoadAnim()
    {
        val body = findViewById<View>(R.id.body) ?: return
        body.alpha = 0f
        body.translationX = 400f
        body.animate().cancel()
        body.animate()
            .translationX(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    private fun playOnDismissAnim(action: Runnable)
    {
        val body = findViewById<View>(R.id.body)
        body!!.animate().cancel()
        body.animate()
                .translationXBy(400f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setListener(
                        object : BaseAnimationListener()
                        {
                            override fun onAnimationEnd(animation: Animator) = action.run()
                        }
                ).start()

        playBackgroundHideOutAnim(200)
    }

    override fun dismiss()
    {
        if (!isAdded)
            return

        playOnDismissAnim {
            Config.loadConfig(GlobalManager.getInstance().getMainActivity()!!)
            GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume())
            super.dismiss()
        }
    }
}
