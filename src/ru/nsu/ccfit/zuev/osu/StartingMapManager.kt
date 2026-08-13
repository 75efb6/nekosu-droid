package ru.nsu.ccfit.zuev.osu

import android.app.Activity
import android.content.res.AssetManager
import androidx.preference.PreferenceManager
import org.anddev.andengine.util.Debug
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class StartingMapManager(private val activity: Activity) {

    fun checkStartingMaps(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getBoolean("initialized", false)
    }

    fun copyStartingMaps() {
        if (checkStartingMaps()) {
            return
        }
        ToastLogger.showText("Preparing for the first launch", false)
        val dirList: Array<String> = try {
            activity.assets.list("Songs") ?: emptyArray()
        } catch (e: IOException) {
            Debug.e("StartingMapManager: ${e.message}", e)
            return
        }

        for (dir in dirList) {
            copyAllFiles(dir)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = prefs.edit()
        editor.putBoolean("initialized", true)
        editor.commit()
    }

    private fun copyAllFiles(dirname: String) {
        val dir = File(Config.getBeatmapPath() + "/" + dirname)
        if (!dir.exists() && !dir.mkdirs()) {
            ToastLogger.showText("Cannot create ${dir.path}", false)
            return
        }
        val fileList: Array<String> = try {
            activity.assets.list("Songs/$dirname") ?: emptyArray()
        } catch (e: IOException) {
            Debug.e("StartingMapManager: ${e.message}", e)
            return
        }

        val mgr: AssetManager = activity.assets
        for (file in fileList) {
            val fullname = "Songs/$dirname/$file"
            try {
                val istream: InputStream = mgr.open(fullname)
                copyFile("$dirname/$file", istream)
            } catch (e: IOException) {
                Debug.e("StartingMapManager: ${e.message}", e)
            }
        }
    }

    private fun copyFile(filename: String, istream: InputStream) {
        val out: OutputStream = try {
            FileOutputStream(Config.getBeatmapPath() + "/" + filename)
        } catch (e: FileNotFoundException) {
            ToastLogger.showText(
                StringTable.format(R.string.message_error, e.message),
                false
            )
            Debug.e("StartingMapManager: ${e.message}", e)
            return
        }

        try {
            val buffer = ByteArray(4096)
            var read: Int
            while (istream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            istream.close()
            out.flush()
            out.close()
        } catch (e: IOException) {
            ToastLogger.showText(e.message ?: "Unknown error", false)
            Debug.e("StartingMapManager: ${e.message}", e)
        }
    }
}
