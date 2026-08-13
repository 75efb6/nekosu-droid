package ru.nsu.ccfit.zuev.osu.helper

import android.content.Context
import androidx.annotation.StringRes
import java.util.Formatter

object StringTable {
    @JvmField
    var context: Context? = null

    private val sb = StringBuilder()
    private val f: Formatter = Formatter(sb)

    @JvmStatic
    fun setContext(context: Context) {
        StringTable.context = context.applicationContext
    }

    @JvmStatic
    fun get(@StringRes resid: Int): String {
        return try {
            context!!.getString(resid)
        } catch (e: NullPointerException) {
            "<error>"
        }
    }

    @JvmStatic
    fun format(resid: Int, vararg objects: Any?): String {
        sb.setLength(0)
        f.format(get(resid), *objects)
        return sb.toString()
    }
}
