package com.edlplan.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.Config
import java.io.File
import java.util.Arrays

class SkinPathPreference : ListPreference {

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun reloadSkinList() {
        try {
            val skinMain = File(Config.getSkinTopPath())
            val skins = HashMap(Config.getSkins())
            val skinsSize = if (skins.size > 0) skins.size + 1 else 1
            Debug.i("Skins count:$skinsSize")
            val entries = arrayOfNulls<CharSequence>(skinsSize)
            val entryValues = arrayOfNulls<CharSequence>(skinsSize)
            entries[0] = "${skinMain.name} (Default)"
            entryValues[0] = skinMain.path

            if (skins.size > 0) {
                var index = 1
                for ((key, value) in skins) {
                    entries[index] = key
                    entryValues[index] = value
                    index++
                }

                Arrays.sort(entries, 1, entries.size)
                Arrays.sort(entryValues, 1, entryValues.size)
            }

            setEntries(entries)
            setEntryValues(entryValues)
        } catch (e: Exception) {
            Debug.e("SkinPathPreference.reloadSkinList: ", e)
            e.printStackTrace()
        }
    }
}
