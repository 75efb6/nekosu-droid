package ru.nsu.ccfit.zuev.osu

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object KeyboardConfig {
    private var enabled = false
    private var keyCursor0 = 0
    private var keyCursor1 = 0
    private var cursorX = 0f
    private var cursorY = 0f
    private var bindingSlot = -1

    @JvmStatic
    fun loadConfig(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        enabled = prefs.getBoolean("kbEnabled", false)
        keyCursor0 = getIntPref(prefs, "kbKey0", 54)
        keyCursor1 = getIntPref(prefs, "kbKey1", 53)
        cursorX = getIntPref(prefs, "kbCursorX", 640).toFloat()
        cursorY = getIntPref(prefs, "kbCursorY", 384).toFloat()

        cursorX = Math.max(0f, Math.min(cursorX, Config.getRES_WIDTH().toFloat()))
        cursorY = Math.max(0f, Math.min(cursorY, Config.getRES_HEIGHT().toFloat()))
    }

    private fun getIntPref(prefs: SharedPreferences, key: String, defValue: Int): Int {
        return try {
            prefs.getInt(key, defValue)
        } catch (e: ClassCastException) {
            try {
                prefs.getString(key, defValue.toString())?.toInt() ?: defValue
            } catch (ex: Exception) {
                defValue
            }
        }
    }

    @JvmStatic
    fun isEnabled(): Boolean = enabled

    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        KeyboardConfig.enabled = enabled
    }

    @JvmStatic
    fun getKeyCursor0(): Int = keyCursor0

    @JvmStatic
    fun setKeyCursor0(key: Int) {
        keyCursor0 = key
    }

    @JvmStatic
    fun getKeyCursor1(): Int = keyCursor1

    @JvmStatic
    fun setKeyCursor1(key: Int) {
        keyCursor1 = key
    }

    @JvmStatic
    fun getCursorX(): Float = cursorX

    @JvmStatic
    fun getCursorX(slot: Int): Float {
        return if (slot == 0) cursorX - 40 else cursorX + 40
    }

    @JvmStatic
    fun setCursorX(cursorX: Float) {
        KeyboardConfig.cursorX = cursorX
    }

    @JvmStatic
    fun getCursorY(): Float = cursorY

    @JvmStatic
    fun getCursorY(slot: Int): Float = cursorY

    @JvmStatic
    fun setCursorY(cursorY: Float) {
        KeyboardConfig.cursorY = cursorY
    }

    @JvmStatic
    fun getBindingSlot(): Int = bindingSlot

    @JvmStatic
    fun setBindingSlot(slot: Int) {
        bindingSlot = slot
    }

    @JvmStatic
    fun isBinding(): Boolean = bindingSlot >= 0

    @JvmStatic
    fun tryBind(slot: Int, keyCode: Int): Boolean {
        if (slot == 0) {
            if (keyCode == keyCursor1) return false
            keyCursor0 = keyCode
        } else if (slot == 1) {
            if (keyCode == keyCursor0) return false
            keyCursor1 = keyCode
        }
        return true
    }

    @JvmStatic
    fun clearBinding(slot: Int) {
        if (slot == 0) {
            keyCursor0 = 0
        } else if (slot == 1) {
            keyCursor1 = 0
        }
    }

    @JvmStatic
    fun getCursorForKey(keyCode: Int): Int {
        if (keyCursor0 != 0 && keyCode == keyCursor0) return 1
        if (keyCursor1 != 0 && keyCode == keyCursor1) return 2
        return -1
    }

    @JvmStatic
    fun saveToPrefs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putBoolean("kbEnabled", enabled)
            .putInt("kbKey0", keyCursor0)
            .putInt("kbKey1", keyCursor1)
            .putInt("kbCursorX", cursorX.toInt())
            .putInt("kbCursorY", cursorY.toInt())
            .apply()
    }
}
