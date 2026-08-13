package ru.nsu.ccfit.zuev.osu.helper.sql

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper private constructor(context: Context) :
    SQLiteOpenHelper(DatabaseContext(context), DBNAME, null, DBVERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $SCORES_TABLENAME ("
                + "id INTEGER PRIMARY KEY,"
                + "filename TEXT,"
                + "playername TEXT,"
                + "replayfile TEXT,"
                + "mode TEXT,"
                + "score INTEGER,"
                + "combo INTEGER,"
                + "mark TEXT,"
                + "h300k INTEGER,"
                + "h300 INTEGER,"
                + "h100k INTEGER,"
                + "h100 INTEGER,"
                + "h50 INTEGER,"
                + "misses INTEGER,"
                + "accuracy FLOAT,"
                + "time TIMESTAMP,"
                + "perfect INTEGER);")

        db.execSQL("CREATE TABLE IF NOT EXISTS $MAPS_TABLENAME ("
                + "id INTEGER PRIMARY KEY,"
                + "size INTEGER,"
                + "inserttime INTEGER,"
                + "link TEXT);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 5 && newVersion == 6) {
            if (oldVersion != 5) {
                val sql = "alter table [$SCORES_TABLENAME] add [time] TIMESTAMP"
                db.execSQL(sql)
            }
            val sql = "alter table [$SCORES_TABLENAME] add [perfect] INTEGER"
            db.execSQL(sql)
        }
    }

    companion object {
        @JvmField
        val SCORES_TABLENAME = "scores"
        @JvmField
        val MAPS_TABLENAME = "ddlmaps"
        private const val DBNAME = "nekosu_droid"
        private const val DBVERSION = 6
        private var helper: DBOpenHelper? = null

        @JvmStatic
        fun getOrCreate(context: Context): DBOpenHelper {
            if (helper == null) {
                helper = DBOpenHelper(context)
            }
            return helper!!
        }
    }
}
