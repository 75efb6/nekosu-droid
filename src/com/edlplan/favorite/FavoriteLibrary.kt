package com.edlplan.favorite

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.HashMap
import java.util.HashSet
import ru.nsu.ccfit.zuev.osu.Config

class FavoriteLibrary private constructor() {

    private var json: File? = null
    private val favorites: HashMap<String, HashSet<String>> = HashMap()

    fun load() {
        val jsonPath = Config.getCorePath() + "json/favorite.json"
        json = File(jsonPath)
        try {
            ensureFile(json!!)
            val favorite: JSONObject
            val jsonTxt = readFull(json!!)
            if (jsonTxt.isEmpty()) {
                favorite = JSONObject()
            } else {
                try {
                    favorite = JSONObject(jsonTxt)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    favorite = JSONObject()
                }
            }
            val iterator = favorite.keys()
            while (iterator.hasNext()) {
                val floder = iterator.next()
                val array = favorite.optJSONArray(floder)
                if (!favorites.containsKey(floder)) {
                    favorites[floder] = HashSet()
                }
                for (i in 0 until array.length()) {
                    favorites[floder]!!.add(array.optString(i))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getMaps(floder: String): HashSet<String>? {
        return favorites[floder]
    }

    fun getFolders(): Set<String> {
        return favorites.keys
    }

    fun addFolder(name: String) {
        if (!favorites.containsKey(name)) {
            favorites[name] = HashSet()
            save()
        }
    }

    fun add(folder: String, path: String) {
        if (!favorites.containsKey(folder)) {
            favorites[folder] = HashSet()
        }
        favorites[folder]!!.add(path)
        save()
    }

    fun remove(folder: String) {
        if (favorites.containsKey(folder)) {
            favorites.remove(folder)
            save()
        }
    }

    fun remove(folder: String, path: String) {
        if (favorites.containsKey(folder)) {
            if (favorites[folder]!!.contains(path)) {
                favorites[folder]!!.remove(path)
                save()
            }
        }
    }

    fun `in`(folder: String, path: String): Boolean {
        return favorites.containsKey(folder) && favorites[folder]!!.contains(path)
    }

    fun save() {
        try {
            val obj = JSONObject()
            for ((key, value) in favorites) {
                val array = JSONArray()
                for (path in value) {
                    array.put(path)
                }
                obj.put(key, array)
            }
            cover(obj.toString(2), json!!)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val library = FavoriteLibrary()

        @JvmStatic
        fun get(): FavoriteLibrary {
            return library
        }

        @Throws(IOException::class)
        private fun ensureFile(file: File) {
            checkExistDir(file.parentFile!!)
            if (!file.exists()) {
                file.createNewFile()
            }
        }

        private fun checkExistDir(dir: File) {
            if (!dir.exists()) dir.mkdirs()
        }

        @Throws(IOException::class)
        private fun readFull(file: File): String {
            val inputStream = FileInputStream(file)
            val bytes = ByteArray(inputStream.available())
            inputStream.read(bytes)
            inputStream.close()
            return String(bytes, Charset.forName("UTF-8"))
        }

        @Throws(IOException::class)
        fun cover(string: String, file: File) {
            ensureFile(file)
            val outputStream = FileOutputStream(file)
            outputStream.write(string.toByteArray())
            outputStream.close()
        }
    }
}
