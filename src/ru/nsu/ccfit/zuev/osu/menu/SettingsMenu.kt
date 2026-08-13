package ru.nsu.ccfit.zuev.osu.menu

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.AnimRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.SkinPathPreference
import com.edlplan.ui.fragment.AudioCalibratorFragment
import com.edlplan.ui.fragment.BaseFragment
import com.edlplan.ui.fragment.LoadingFragment
import com.edlplan.ui.fragment.SettingsFragment
import com.edlplan.ui.EasingHelper
import com.google.android.material.snackbar.Snackbar
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.UpdateManager
import com.reco1l.legacy.discord.DiscordLoginFragment
import com.reco1l.legacy.discord.DiscordRPC
import com.reco1l.legacy.ui.StyledInputDialog
import com.reco1l.legacy.ui.StyledKeybindDialog
import com.reco1l.legacy.ui.StyledSelectionDialog
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.KeyboardConfig
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.io.File

class SettingsMenu : SettingsFragment() {

    private var mParentScreen: PreferenceScreen? = null
    private var parentScreen: PreferenceScreen? = null
    private var isOnNestedScreen = false
    private var mActivity: Activity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = GlobalManager.getInstance().getMainActivity()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.options, rootKey)
        val skinPath = findPreference<SkinPathPreference>("skinPath")
        skinPath?.reloadSkinList()
        skinPath?.setOnPreferenceChangeListener { _, newValue ->
            if (GlobalManager.getInstance().skinNow != newValue.toString()) {
                val loading = LoadingFragment()
                loading.show()
                Execution.async {
                    GlobalManager.getInstance().skinNow = Config.getSkinPath()
                    SkinManager.getInstance().clearSkin()
                    ResourceManager.getInstance().loadSkin(newValue.toString())
                    GlobalManager.getInstance().engine?.textureManager?.reloadTextures()
                    mActivity?.runOnUiThread {
                        loading.dismiss()
                        mActivity?.startActivity(Intent(mActivity, MainActivity::class.java))
                        Snackbar.make(mActivity!!.findViewById(android.R.id.content), StringTable.get(R.string.message_loaded_skin), 1500).show()
                    }
                }
            }
            true
        }
        mParentScreen = preferenceScreen
        parentScreen = preferenceScreen
        findPreference<Preference>("onlineOption")?.setOnPreferenceClickListener {
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("general")?.setOnPreferenceClickListener {
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("color")?.setOnPreferenceClickListener {
            parentScreen = findPreference("general")
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("sound")?.setOnPreferenceClickListener {
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("calibrator")?.setOnPreferenceClickListener {
            AudioCalibratorFragment().show()
            true
        }
        val offsetPref = findPreference<androidx.preference.SeekBarPreference>("offset")
        offsetPref?.setOnPreferenceClickListener {
            StyledInputDialog.show(mActivity!!, it.title.toString(),
                Config.getOffset().toString(),
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            ) { value ->
                try {
                    var newOffset = value.trim().toInt()
                    newOffset = newOffset.coerceIn(-250, 250)
                    offsetPref.value = newOffset
                    Config.setOffset(newOffset.toFloat())
                    mActivity?.let { activity ->
                        androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity)
                            .edit().putInt("offset", newOffset).apply()
                    }
                } catch (ignored: NumberFormatException) {
                }
            }
            true
        }
        findPreference<Preference>("beatmaps")?.setOnPreferenceClickListener {
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("advancedopts")?.setOnPreferenceClickListener {
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("keyboard")?.setOnPreferenceClickListener {
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("kbKeybinds")?.setOnPreferenceClickListener {
            parentScreen = findPreference("keyboard")
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        findPreference<Preference>("kbCursorPos")?.setOnPreferenceClickListener {
            parentScreen = findPreference("keyboard")
            setPreferenceScreen(it as PreferenceScreen)
            true
        }
        setupKeybindPreference("kbKey0", 0)
        setupKeybindPreference("kbKey1", 1)
        val onlinePassword = findPreference<EditTextPreference>("onlinePassword")
        onlinePassword?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val skinToppref = findPreference<EditTextPreference>("skinTopPath")
        skinToppref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().trim().isEmpty()) {
                skinToppref.text = Config.getCorePath() + "Skin/"
                Config.loadConfig(mActivity!!)
                skinPath?.reloadSkinList()
                return@setOnPreferenceChangeListener false
            }
            val file = File(newValue.toString())
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true)
                    return@setOnPreferenceChangeListener false
                }
            }
            skinToppref.text = newValue.toString()
            Config.loadConfig(mActivity!!)
            skinPath?.reloadSkinList()
            false
        }
        findPreference<Preference>("clear")?.setOnPreferenceClickListener {
            LibraryManager.INSTANCE.clearCache()
            true
        }
        findPreference<Preference>("clear_properties")?.setOnPreferenceClickListener {
            PropertiesLibrary.instance.clear(mActivity!!)
            true
        }
        findPreference<Preference>("registerAcc")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_URL))
            startActivity(intent)
            true
        }
        findPreference<Preference>("update")?.setOnPreferenceClickListener {
            UpdateManager.checkNewUpdates(false)
            true
        }
        findPreference<Preference>("dither")?.setOnPreferenceChangeListener { _, newValue ->
            if (Config.isUseDither() != newValue as Boolean) {
                GlobalManager.getInstance().mainScene?.restart()
            }
            true
        }
        findPreference<Preference>("seasonalBg")?.setOnPreferenceChangeListener { _, newValue ->
            if (Config.isSeasonalBg() != newValue as Boolean) {
                Config.setSeasonalBg(newValue)
                GlobalManager.getInstance().mainScene?.reloadSeasonalBackground()
            }
            true
        }
        val discordLogin = findPreference<Preference>("discordLogin")
        discordLogin?.summary = if (DiscordRPC.isConnected) getString(R.string.opt_discord_login_summary_logged) else getString(R.string.opt_discord_login_summary_not_logged)
        discordLogin?.setOnPreferenceClickListener {
            val fragment = DiscordLoginFragment()
            fragment.onDismissListener = BaseFragment.OnDismissListener {
                discordLogin.summary = if (DiscordRPC.isConnected) getString(R.string.opt_discord_login_summary_logged) else getString(R.string.opt_discord_login_summary_not_logged)
            }
            fragment.show()
            true
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        if (preferenceScreen.key != null) {
            if (!isOnNestedScreen) {
                isOnNestedScreen = true
                animateBackButton(R.drawable.back_black)
            }
            setTitle(preferenceScreen.title.toString())
            for (v in intArrayOf(android.R.id.list_container, R.id.title)) {
                animateView(v, R.anim.slide_in_right)
            }
        }
    }

    private fun animateBackButton(@DrawableRes newDrawable: Int) {
        val animation = AnimationUtils.loadAnimation(mActivity, R.anim.rotate_360)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation) {
                val backButton = findViewById<ImageButton>(R.id.back_button)
                backButton?.setImageDrawable(mActivity!!.resources.getDrawable(newDrawable))
            }
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationStart(animation: Animation) {}
        })
        findViewById<ImageButton>(R.id.back_button)?.startAnimation(animation)
    }

    private fun animateView(@IdRes viewId: Int, @AnimRes anim: Int) {
        findViewById<View>(viewId)?.startAnimation(AnimationUtils.loadAnimation(mActivity, anim))
    }

    private fun setTitle(title: String) {
        (findViewById<TextView>(R.id.title))?.text = title
    }

    override fun callDismissOnBackPress() {
        navigateBack()
    }

    private fun navigateBack() {
        for (v in intArrayOf(android.R.id.list_container, R.id.title)) {
            animateView(v, R.anim.slide_in_left)
        }
        if (parentScreen?.key != null) {
            setPreferenceScreen(parentScreen)
            setTitle(parentScreen!!.title.toString())
            parentScreen = mParentScreen
            return
        }
        if (isOnNestedScreen) {
            isOnNestedScreen = false
            animateBackButton(R.drawable.close_black)
            setPreferenceScreen(mParentScreen)
            setTitle(StringTable.get(R.string.menu_settings_title))
        } else {
            dismiss()
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when {
            preference is ListPreference -> StyledSelectionDialog.show(requireContext(), preference)
            preference is EditTextPreference -> {
                val inputType = if ("onlinePassword" == preference.key)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT
                StyledInputDialog.show(requireContext(), preference, inputType)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onLoadView() {
        findViewById<ImageButton>(R.id.back_button)?.setOnClickListener { navigateBack() }
        val background = findViewById<View>(R.id.frg_background)
        background?.setOnClickListener { callDismissOnBackPress() }
        playOnLoadAnim()
    }

    protected open fun playOnLoadAnim() {
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

    protected open fun playOnDismissAnim(action: Runnable?) {
        val body = findViewById<View>(R.id.body) ?: return
        body.animate().cancel()
        body.animate()
            .translationXBy(400f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    action?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    override fun show() {
        super.show()
    }

    override fun dismiss() {
        playOnDismissAnim(Runnable {
            Config.loadConfig(mActivity!!)
            GlobalManager.getInstance().mainScene?.reloadOnlinePanel()
            GlobalManager.getInstance().mainScene?.loadTimingPoints(false)
            val songService = GlobalManager.getInstance().songService
            songService?.setVolume(Config.getBgmVolume())
            songService?.isGaming = false
            if (songService != null) applyDiscordRpc(songService)
            super@SettingsMenu.dismiss()
        })
    }

    private fun applyDiscordRpc(songService: SongService) {
        if (!Config.isDiscordRichPresence() || !DiscordRPC.isConnected) {
            DiscordRPC.disconnect()
            return
        }
        val current = GlobalManager.getInstance().engine?.scene
        if (current === GlobalManager.getInstance().songMenu?.scene) {
            DiscordRPC.updateForSongSelection()
        } else {
            DiscordRPC.updateForMainMenu()
        }
    }

    private fun setupKeybindPreference(key: String, slot: Int) {
        val pref = findPreference<Preference>(key) ?: return
        updateKeybindSummary(pref, slot)
        pref.setOnPreferenceClickListener {
            KeyboardConfig.setBindingSlot(slot)
            StyledKeybindDialog.show(
                mActivity!!,
                "Bind key for ${if (slot == 0) "Cursor 1" else "Cursor 2"}",
                slot,
                { keyCode ->
                    KeyboardConfig.setBindingSlot(-1)
                    if (KeyboardConfig.tryBind(slot, keyCode)) {
                        KeyboardConfig.setEnabled(true)
                        KeyboardConfig.saveToPrefs(mActivity!!)
                        updateKeybindSummary(it, slot)
                    }
                }
            ) { KeyboardConfig.setBindingSlot(-1) }
            true
        }
    }

    private fun updateKeybindSummary(pref: Preference, slot: Int) {
        val keyCode = if (slot == 0) KeyboardConfig.getKeyCursor0() else KeyboardConfig.getKeyCursor1()
        if (keyCode == 0) {
            pref.summary = "Not bound"
        } else {
            pref.summary = StyledKeybindDialog.getKeyName(keyCode)
        }
    }

    companion object {
        val REGISTER_URL = "https://${OnlineManager.HOSTNAME}/user/register"
    }
}
