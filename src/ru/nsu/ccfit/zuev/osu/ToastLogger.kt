package ru.nsu.ccfit.zuev.osu

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import java.util.ArrayList
import ru.nsu.ccfit.zuev.osu.helper.StringTable

class ToastLogger private constructor(private val activity: Activity) {
    private var message = ""
    private var showlong = false
    private val debugLog = ArrayList<String>()
    @JvmField
    var percentage: Float = 0f

    companion object {
        private var instance: ToastLogger? = null

        @JvmStatic
        fun init(activity: Activity) {
            instance = ToastLogger(activity)
        }

        @JvmStatic
        fun showText(message: String, showlong: Boolean) {
            instance?.activity?.runOnUiThread {
                Toast.makeText(
                    instance!!.activity, message,
                    if (showlong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
        }

        @JvmStatic
        fun showTextId(@StringRes resID: Int, showlong: Boolean) {
            showText(StringTable.get(resID), showlong)
        }

        @JvmStatic
        fun addToLog(str: String) {
            // no-op
        }

        @JvmStatic
        fun getLog(): ArrayList<String>? {
            return instance?.debugLog
        }

        @JvmStatic
        fun getPercentage(): Float {
            return instance?.percentage ?: -1f
        }

        @JvmStatic
        fun setPercentage(perc: Float) {
            instance?.percentage = perc
        }
    }
}
