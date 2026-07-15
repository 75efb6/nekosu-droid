package ru.nsu.ccfit.zuev.osu.scoring

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteCantOpenDatabaseException
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.helper.sql.DBOpenHelper
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring
import ru.nsu.ccfit.zuev.osu.online.SendingPanel
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Pattern

class ScoreLibrary private constructor() {
    private var db: android.database.sqlite.SQLiteDatabase? = null

    fun getDb(): android.database.sqlite.SQLiteDatabase? = db

    fun load(context: Context) {
        val helper = DBOpenHelper.getOrCreate(context)
        try {
            db = helper.writableDatabase
        } catch (e: SQLiteCantOpenDatabaseException) {
            ToastLogger.showText(StringTable.get(R.string.require_storage_permission), true)
            throw RuntimeException(e)
        }
        loadOld(context)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadOld(context: Context) {
        val prefs = context.getSharedPreferences("score_migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("migrated", false)) {
            return
        }

        val folder = File(Config.getCorePath() + "/Scores")
        if (!folder.exists()) {
            prefs.edit().putBoolean("migrated", true).apply()
            return
        }
        val f = File(folder, "scoreboard")
        if (!f.exists()) {
            prefs.edit().putBoolean("migrated", true).apply()
            return
        }
        Debug.i("Loading old scores...")
        try {
            val input = ObjectInputStream(FileInputStream(f))

            var obj = input.readObject()
            var versionStr = ""
            if (obj is String) {
                versionStr = obj
                if (versionStr != "scores1" && versionStr != "scores2") {
                    input.close()
                    return
                }
            } else {
                input.close()
                return
            }
            obj = input.readObject()
            var scores: Map<String, ArrayList<StatisticV2>>? = null
            if (obj is Map<*, *>) {
                if (versionStr == "scores1") {
                    val oldStat = obj as Map<String, ArrayList<Statistic>>
                    scores = HashMap()
                    for (str in oldStat.keys) {
                        val newStat = ArrayList<StatisticV2>()
                        for (s in oldStat[str]!!) {
                            newStat.add(StatisticV2(s))
                        }
                        val newPathMather = newPathPattern.matcher(str)
                        if (newPathMather.find()) {
                            scores[newPathMather.group()] = newStat
                        } else {
                            scores[str] = newStat
                        }
                    }
                } else if (versionStr == "scores2") {
                    scores = obj as Map<String, ArrayList<StatisticV2>>
                }
            }

            if (scores != null) {
                for (track in scores.keys) {
                    for (stat in scores[track]!!) {
                        addScore(track, stat, null)
                    }
                }
            }

            input.close()
        } catch (e: Exception) {
            Debug.e("ScoreLibrary.loadOld: ${e.message}")
            return
        }
        f.delete()
        prefs.edit().putBoolean("migrated", true).apply()
    }

    fun save() {}

    fun sendScoreOnline(stat: StatisticV2, replay: String, panel: SendingPanel) {
        Debug.i("Preparing for online!")
        if (stat.totalScoreWithMultiplier <= 0) return
        OnlineScoring.getInstance().sendRecord(stat, panel, replay)
    }

    fun addScore(trackPath: String, stat: StatisticV2, replay: String?) {
        if (stat.totalScoreWithMultiplier == 0 || stat.mod.contains(GameMod.MOD_AUTO)) {
            return
        }
        val track = getTrackPath(trackPath)

        if (db == null) return
        val values = ContentValues()
        values.put("filename", track)
        values.put("playername", stat.playerName)
        values.put("replayfile", replay)
        values.put("mode", stat.getModString())
        values.put("score", stat.totalScoreWithMultiplier)
        values.put("combo", stat.maxCombo)
        values.put("mark", stat.getMark())
        values.put("h300k", stat.hit300k)
        values.put("h300", stat.hit300)
        values.put("h100k", stat.hit100k)
        values.put("h100", stat.hit100)
        values.put("h50", stat.hit50)
        values.put("misses", stat.misses)
        values.put("accuracy", stat.getAccuracy())
        values.put("time", stat.time)
        values.put("perfect", if (stat.isPerfect) 1 else 0)

        val result = db!!.insert(DBOpenHelper.SCORES_TABLENAME, null, values)
        Debug.i("Inserting data, result = $result")
    }

    fun getMapScores(columns: Array<String>, filename: String): android.database.Cursor? {
        val track = getTrackPath(filename)
        if (db == null) return null
        return db!!.query(
            DBOpenHelper.SCORES_TABLENAME, columns, "filename = ?",
            arrayOf(track), null, null, "score DESC"
        )
    }

    fun getBestMark(trackPath: String): String? {
        val track = getTrackPath(trackPath)
        val columns = arrayOf("mark", "filename", "id", "score")
        val response = db!!.query(
            DBOpenHelper.SCORES_TABLENAME, columns, "filename = ?",
            arrayOf(track), null, null, "score DESC"
        )
        if (response.count == 0) {
            response.close()
            return null
        }
        response.moveToFirst()

        val mark = response.getString(0)
        response.close()

        return mark
    }

    fun getScore(id: Int): StatisticV2 {
        val c = db!!.query(
            DBOpenHelper.SCORES_TABLENAME, null, "id = $id",
            null, null, null, null
        )
        val stat = StatisticV2()
        if (c.count == 0) {
            c.close()
            return stat
        }
        c.moveToFirst()

        stat.playerName = c.getString(c.getColumnIndexOrThrow("playername"))
        stat.replayName = c.getString(c.getColumnIndexOrThrow("replayfile"))
        stat.setModFromString(c.getString(c.getColumnIndexOrThrow("mode")))
        stat.setForcedScore(c.getInt(c.getColumnIndexOrThrow("score")))
        stat.maxCombo = c.getInt(c.getColumnIndexOrThrow("combo"))
        stat.setMark(c.getString(c.getColumnIndexOrThrow("mark")))
        stat.hit300k = c.getInt(c.getColumnIndexOrThrow("h300k"))
        stat.hit300 = c.getInt(c.getColumnIndexOrThrow("h300"))
        stat.hit100k = c.getInt(c.getColumnIndexOrThrow("h100k"))
        stat.hit100 = c.getInt(c.getColumnIndexOrThrow("h100"))
        stat.hit50 = c.getInt(c.getColumnIndexOrThrow("h50"))
        stat.misses = c.getInt(c.getColumnIndexOrThrow("misses"))
        stat.accuracy = c.getFloat(c.getColumnIndexOrThrow("accuracy"))
        stat.time = c.getLong(c.getColumnIndexOrThrow("time"))
        stat.isPerfect = c.getInt(c.getColumnIndexOrThrow("perfect")) != 0

        c.close()

        return stat
    }

    fun deleteScore(id: Int): Boolean {
        return db!!.delete(DBOpenHelper.SCORES_TABLENAME, "id = $id", null) != 0
    }

    companion object {
        private val newPathPattern = Pattern.compile("[^/]*/[^/]*\\z")
        private val lib = ScoreLibrary()

        @JvmStatic
        fun getInstance(): ScoreLibrary = lib

        @JvmStatic
        fun getTrackPath(track: String): String {
            val newPathMather = newPathPattern.matcher(track)
            if (newPathMather.find()) {
                return newPathMather.group()!!
            }
            return track
        }

        @JvmStatic
        fun getTrackDir(track: String): String {
            val s = getTrackPath(track)
            return if (s.endsWith(".osu")) {
                s.substring(0, s.indexOf('/'))
            } else {
                s.substring(s.indexOf('/') + 1, s.length)
            }
        }
    }
}
