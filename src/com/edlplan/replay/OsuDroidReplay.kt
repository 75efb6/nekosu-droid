package com.edlplan.replay

import org.json.JSONException
import org.json.JSONObject
import java.io.File

class OsuDroidReplay {

    var fileName: String? = null
    var playerName: String? = null
    var replayFile: String? = null
    var mode: String? = null
    var score: Int = 0
    var combo: Int = 0
    var mark: String? = null
    var h300k: Int = 0
    var h300: Int = 0
    var h100k: Int = 0
    var h100: Int = 0
    var h50: Int = 0
    var misses: Int = 0
    var accuracy: Float = 0f
    var time: Long = 0
    var perfect: Int = 0

    val isAbsoluteReplay: Boolean
        get() = replayFile?.contains("/") ?: false

    val replayFileName: String?
        get() = if (isAbsoluteReplay) File(replayFile).name else replayFile

    fun toJSON(): JSONObject {
        val replayData = JSONObject()
        try {
            replayData.put("filename", fileName)
            replayData.put("playername", playerName)
            replayData.put("replayfile", replayFileName)
            replayData.put("mod", mode)
            replayData.put("score", score)
            replayData.put("combo", combo)
            replayData.put("mark", mark)
            replayData.put("h300k", h300k)
            replayData.put("h300", h300)
            replayData.put("h100k", h100k)
            replayData.put("h100", h100)
            replayData.put("h50", h50)
            replayData.put("misses", misses)
            replayData.put("accuracy", accuracy.toDouble())
            replayData.put("time", time)
            replayData.put("perfect", perfect)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return replayData
    }

    companion object {
        @Throws(JSONException::class)
        private fun getString(cursor: JSONObject, c: String): String = cursor.getString(c)

        @Throws(JSONException::class)
        private fun getInt(cursor: JSONObject, c: String): Int = cursor.getInt(c)

        @Throws(JSONException::class)
        private fun getLong(cursor: JSONObject, c: String): Long = cursor.getLong(c)

        @Throws(JSONException::class)
        private fun getFloat(cursor: JSONObject, c: String): Float = cursor.getDouble(c).toFloat()

        @JvmStatic
        @Throws(JSONException::class)
        fun parseJSON(cursor: JSONObject): OsuDroidReplay {
            return OsuDroidReplay().apply {
                fileName = getString(cursor, "filename")
                playerName = getString(cursor, "playername")
                replayFile = getString(cursor, "replayfile")
                mode = getString(cursor, "mod")
                score = getInt(cursor, "score")
                combo = getInt(cursor, "combo")
                mark = getString(cursor, "mark")
                h300k = getInt(cursor, "h300k")
                h300 = getInt(cursor, "h300")
                h100k = getInt(cursor, "h100k")
                h100 = getInt(cursor, "h100")
                h50 = getInt(cursor, "h50")
                misses = getInt(cursor, "misses")
                accuracy = getFloat(cursor, "accuracy")
                time = getLong(cursor, "time")
                perfect = getInt(cursor, "perfect")
            }
        }
    }
}
