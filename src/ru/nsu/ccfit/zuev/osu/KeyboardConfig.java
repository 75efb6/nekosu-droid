package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class KeyboardConfig {

    private static boolean enabled;
    private static int keyCursor0;
    private static int keyCursor1;
    private static float cursorX;
    private static float cursorY;

    private static int bindingSlot = -1;

    public static void loadConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        enabled = prefs.getBoolean("kbEnabled", false);
        keyCursor0 = getIntPref(prefs, "kbKey0", 54);
        keyCursor1 = getIntPref(prefs, "kbKey1", 53);
        cursorX = getIntPref(prefs, "kbCursorX", 640);
        cursorY = getIntPref(prefs, "kbCursorY", 384);

        cursorX = Math.max(0, Math.min(cursorX, Config.getRES_WIDTH()));
        cursorY = Math.max(0, Math.min(cursorY, Config.getRES_HEIGHT()));
    }

    private static int getIntPref(SharedPreferences prefs, String key, int defValue) {
        try {
            return prefs.getInt(key, defValue);
        } catch (ClassCastException e) {
            try {
                return Integer.parseInt(prefs.getString(key, String.valueOf(defValue)));
            } catch (Exception ex) {
                return defValue;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        KeyboardConfig.enabled = enabled;
    }

    public static int getKeyCursor0() {
        return keyCursor0;
    }

    public static void setKeyCursor0(int key) {
        keyCursor0 = key;
    }

    public static int getKeyCursor1() {
        return keyCursor1;
    }

    public static void setKeyCursor1(int key) {
        keyCursor1 = key;
    }

    public static float getCursorX() {
        return cursorX;
    }

    public static float getCursorX(int slot) {
        // Offset cursor 1 to the left of cursor 0 so they're visually distinct
        return slot == 0 ? cursorX - 40 : cursorX + 40;
    }

    public static void setCursorX(float cursorX) {
        KeyboardConfig.cursorX = cursorX;
    }

    public static float getCursorY() {
        return cursorY;
    }

    public static float getCursorY(int slot) {
        return cursorY;
    }

    public static void setCursorY(float cursorY) {
        KeyboardConfig.cursorY = cursorY;
    }

    public static int getBindingSlot() {
        return bindingSlot;
    }

    public static void setBindingSlot(int slot) {
        bindingSlot = slot;
    }

    public static boolean isBinding() {
        return bindingSlot >= 0;
    }

    /**
     * Try to bind a key to the given slot.
     * Returns true if the binding was accepted, false if the key is already used by the other slot.
     */
    public static boolean tryBind(int slot, int keyCode) {
        if (slot == 0) {
            if (keyCode == keyCursor1) return false;
            keyCursor0 = keyCode;
        } else if (slot == 1) {
            if (keyCode == keyCursor0) return false;
            keyCursor1 = keyCode;
        }
        return true;
    }

    /**
     * Clear the binding for the given slot.
     */
    public static void clearBinding(int slot) {
        if (slot == 0) {
            keyCursor0 = 0;
        } else if (slot == 1) {
            keyCursor1 = 0;
        }
    }

    /**
     * Returns the cursor index (0-1) that a keycode is bound to, or -1 if unbound.
     */
    public static int getCursorForKey(int keyCode) {
        if (keyCursor0 != 0 && keyCode == keyCursor0) return 1;
        if (keyCursor1 != 0 && keyCode == keyCursor1) return 2;
        return -1;
    }

    public static void saveToPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putBoolean("kbEnabled", enabled)
            .putInt("kbKey0", keyCursor0)
            .putInt("kbKey1", keyCursor1)
            .putInt("kbCursorX", (int) cursorX)
            .putInt("kbCursorY", (int) cursorY)
            .apply();
    }
}
