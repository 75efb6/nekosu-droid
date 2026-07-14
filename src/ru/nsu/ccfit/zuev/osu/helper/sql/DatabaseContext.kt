package ru.nsu.ccfit.zuev.osu.helper.sql

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.FileUtils
import ru.nsu.ccfit.zuev.osu.Config
import java.io.File
import java.io.IOException

class DatabaseContext(base: Context) : ContextWrapper(base) {

    private val context: Context = base

    override fun getDatabasePath(name: String): File {
        var dbfile = Config.getCorePath() + "databases/" + name
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db"
        }
        val result = File(dbfile)
        if (!result.parentFile.exists()) {
            result.parentFile.mkdirs()
        }
        Debug.i("getDatabasePath($name) = ${result.absolutePath}")
        val olddb = context.getDatabasePath(name)
        if (!result.exists() && olddb.exists()) {
            try {
                FileUtils.copyFile(olddb, result)
            } catch (e: IOException) {
                Debug.e(e)
            }
        }
        val olddbfile = Config.getCorePath() + File.separator + "databases" + File.separator + name
        val olddb2 = File(olddbfile)
        if (!result.exists() && olddb2.exists()) {
            try {
                FileUtils.copyFile(olddb2, result)
            } catch (e: IOException) {
                Debug.e(e)
            }
        }
        val olddbfile2 = Config.getCorePath() + File.separator + "databases" + File.separator + "osudroid.db"
        val olddb3 = File(olddbfile2)
        if (!result.exists() && olddb3.exists()) {
            try {
                FileUtils.copyFile(olddb3, result)
            } catch (e: IOException) {
                Debug.e(e)
            }
        }
        return result
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase {
        return openOrCreateDatabase(name, mode, factory)
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?
    ): SQLiteDatabase {
        val result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null)
        Debug.i("openOrCreateDatabase($name) = ${result.path}")
        return result
    }
}
