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

    private static float getFloatPref(SharedPreferences prefs, String key, float defValue) {
        try {
            return prefs.getFloat(key, defValue);
        } catch (ClassCastException e) {
            try {
                return Float.parseFloat(prefs.getString(key, String.valueOf(defValue)));
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

    public static void setCursorX(float cursorX) {
        KeyboardConfig.cursorX = cursorX;
    }

    public static float getCursorY() {
        return cursorY;
    }

    public static void setCursorY(float cursorY) {
        KeyboardConfig.cursorY = cursorY;
    }

    /**
     * Returns the cursor index (0-2) that a keycode is bound to, or -1 if unbound.
     */
    public static int getCursorForKey(int keyCode) {
        if (keyCode == keyCursor0) return 0;
        if (keyCode == keyCursor1) return 1;
        return -1;
    }
}
