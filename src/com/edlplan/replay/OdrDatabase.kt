package com.edlplan.replay

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File

class OdrDatabase {

    var database: SQLiteDatabase? = null
        private set

    var onDatabaseChangedListener: Runnable? = null

    constructor(file: File) {
        database = SQLiteDatabase.openDatabase(file.absolutePath, null, 0)
    }

    constructor(database: SQLiteDatabase) {
        this.database = database
    }

    fun available(): Boolean = database != null

    fun write(replay: OsuDroidReplay): Long {
        val values = ContentValues().apply {
            put("filename", SongsLibrary.get().toSetLocal(replay.fileName ?: ""))
            put("playername", replay.playerName)
            if (replay.isAbsoluteReplay) {
                put("replayfile", replay.replayFile)
            } else {
                put("replayfile", File(OdrConfig.getScoreDir(), replay.replayFile).absolutePath)
            }
            put("mode", replay.mode)
            put("score", replay.score)
            put("combo", replay.combo)
            put("mark", replay.mark)
            put("h300k", replay.h300k)
            put("h300", replay.h300)
            put("h100k", replay.h100k)
            put("h100", replay.h100)
            put("h50", replay.h50)
            put("misses", replay.misses)
            put("accuracy", replay.accuracy)
            put("time", replay.time)
            put("perfect", replay.perfect)
        }
        return database!!.insert("scores", null, values)
    }

    fun getReplayById(id: Int): List<OsuDroidReplay> {
        val replays = ArrayList<OsuDroidReplay>()
        if (!available()) {
            return replays
        }
        val cursor = database!!.rawQuery("SELECT * FROM scores WHERE id = ?", arrayOf(id.toString()))
        while (cursor.moveToNext()) {
            val replay = OsuDroidReplay().apply {
                fileName = cursor.getString(cursor.getColumnIndexOrThrow("filename"))
                playerName = cursor.getString(cursor.getColumnIndexOrThrow("playername"))
                replayFile = cursor.getString(cursor.getColumnIndexOrThrow("replayfile"))
                mode = cursor.getString(cursor.getColumnIndexOrThrow("mode"))
                score = cursor.getInt(cursor.getColumnIndexOrThrow("score"))
                combo = cursor.getInt(cursor.getColumnIndexOrThrow("combo"))
                mark = cursor.getString(cursor.getColumnIndexOrThrow("mark"))
                h300k = cursor.getInt(cursor.getColumnIndexOrThrow("h300k"))
                h300 = cursor.getInt(cursor.getColumnIndexOrThrow("h300"))
                h100k = cursor.getInt(cursor.getColumnIndexOrThrow("h100k"))
                h100 = cursor.getInt(cursor.getColumnIndexOrThrow("h100"))
                h50 = cursor.getInt(cursor.getColumnIndexOrThrow("h50"))
                misses = cursor.getInt(cursor.getColumnIndexOrThrow("misses"))
                accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy"))
                time = cursor.getLong(cursor.getColumnIndexOrThrow("time"))
                perfect = cursor.getInt(cursor.getColumnIndexOrThrow("perfect"))
            }
            replays.add(replay)
        }
        return replays
    }

    fun getReplays(): List<OsuDroidReplay> {
        val replays = ArrayList<OsuDroidReplay>()
        if (!available()) {
            return replays
        }
        val cursor = database!!.rawQuery("SELECT * FROM scores", arrayOf<String>())
        while (cursor.moveToNext()) {
            val replay = OsuDroidReplay().apply {
                fileName = cursor.getString(cursor.getColumnIndexOrThrow("filename"))
                playerName = cursor.getString(cursor.getColumnIndexOrThrow("playername"))
                replayFile = cursor.getString(cursor.getColumnIndexOrThrow("replayfile"))
                mode = cursor.getString(cursor.getColumnIndexOrThrow("mode"))
                score = cursor.getInt(cursor.getColumnIndexOrThrow("score"))
                combo = cursor.getInt(cursor.getColumnIndexOrThrow("combo"))
                mark = cursor.getString(cursor.getColumnIndexOrThrow("mark"))
                h300k = cursor.getInt(cursor.getColumnIndexOrThrow("h300k"))
                h300 = cursor.getInt(cursor.getColumnIndexOrThrow("h300"))
                h100k = cursor.getInt(cursor.getColumnIndexOrThrow("h100k"))
                h100 = cursor.getInt(cursor.getColumnIndexOrThrow("h100"))
                h50 = cursor.getInt(cursor.getColumnIndexOrThrow("h50"))
                misses = cursor.getInt(cursor.getColumnIndexOrThrow("misses"))
                accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy"))
                time = cursor.getLong(cursor.getColumnIndexOrThrow("time"))
                perfect = cursor.getInt(cursor.getColumnIndexOrThrow("perfect"))
            }
            replays.add(replay)
        }
        return replays
    }

    fun deleteReplay(id: Int): Int {
        if (!available()) {
            return 0
        }
        val result = database!!.delete("scores", "id = ?", arrayOf(id.toString()))
        onDatabaseChangedListener?.run()
        return result
    }

    companion object {
        private var odrDatabase: OdrDatabase? = null

        @JvmStatic
        fun get(): OdrDatabase {
            if (odrDatabase == null) {
                odrDatabase = OdrDatabase(OdrConfig.getMainDatabase())
            }
            return odrDatabase!!
        }
    }
}
