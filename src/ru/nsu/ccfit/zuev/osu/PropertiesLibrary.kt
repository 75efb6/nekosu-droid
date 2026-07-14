package ru.nsu.ccfit.zuev.osu

import android.content.Context
import org.anddev.andengine.util.Debug
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.HashMap
import ru.nsu.ccfit.zuev.osu.async.AsyncTask
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class PropertiesLibrary private constructor() {
    private val version = "properties1"
    private var props: MutableMap<String, BeatmapProperties> = HashMap()
    private var context: Context? = null

    @Suppress("UNCHECKED_CAST")
    fun load(context: Context) {
        this.context = context
        val lib = File(context.filesDir, "properties")
        if (!lib.exists()) {
            return
        }

        try {
            val istream = ObjectInputStream(FileInputStream(lib))
            var obj = istream.readObject()
            if (obj is String) {
                if (obj != version) {
                    istream.close()
                    return
                }
            } else {
                istream.close()
                return
            }
            obj = istream.readObject()
            if (obj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                props = obj as MutableMap<String, BeatmapProperties>
                istream.close()
                Debug.i("Properties loaded")
            }
            istream.close()
        } catch (e: FileNotFoundException) {
            Debug.e("PropertiesLibrary: ${e.message}", e)
        } catch (e: IOException) {
            Debug.e("PropertiesLibrary: ${e.message}", e)
        } catch (e: ClassNotFoundException) {
            Debug.e("PropertiesLibrary: ${e.message}", e)
        }
        ToastLogger.addToLog("Cannot load properties!")
    }

    @Synchronized
    fun save(activity: Context) {
        val lib = File(activity.filesDir, "properties")
        try {
            val ostream = ObjectOutputStream(FileOutputStream(lib))
            ostream.writeObject(version)
            ostream.writeObject(props)
            ostream.close()
        } catch (e: FileNotFoundException) {
            ToastLogger.showText(
                StringTable.format(R.string.message_error, e.message),
                false
            )
            Debug.e("PropertiesLibrary: ${e.message}", e)
        } catch (e: IOException) {
            ToastLogger.showText(
                StringTable.format(R.string.message_error, e.message),
                false
            )
            Debug.e("PropertiesLibrary: ${e.message}", e)
        }
    }

    @Synchronized
    fun clear(activity: Context) {
        val lib = File(activity.filesDir, "properties")
        lib.delete()
        props.clear()
    }

    fun save() {
        val ctx = context ?: return
        save(ctx)
    }

    fun saveAsync() {
        val ctx = context ?: return
        object : AsyncTask() {
            override fun run() {
                save(ctx)
            }
        }.execute()
    }

    fun getProperties(path: String): BeatmapProperties? {
        return if (props.containsKey(path)) props[path] else null
    }

    fun setProperties(path: String, properties: BeatmapProperties) {
        this.load(context!!)
        props[path] = properties
        if (!properties.favorite && properties.getOffset() == 0) {
            props.remove(path)
        }
    }

    companion object {
        @JvmStatic
        val instance = PropertiesLibrary()
    }
}
