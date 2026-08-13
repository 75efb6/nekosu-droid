package com.edlplan.ext

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osuplus.R

object EdExtensionHelper {

    const val EXT_BROADCAST_ANY = "osu.droid.ext.broadcast.any"

    @JvmStatic
    fun downloadExtension(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.edplan.cn/osu/droid/extension/latest_ext.php"))
        GlobalManager.getInstance().getMainActivity()!!.startActivity(intent)
        return true
    }

    @JvmStatic
    fun isExtensionEnable(): Boolean {
        return Config.isEnableExtension()
    }

    @JvmStatic
    fun broadcastMsg(apiName: String, data: String) {
        if (!isExtensionEnable()) return
        val intent = Intent(EXT_BROADCAST_ANY)
        intent.putExtra("api", apiName)
        intent.putExtra("type", "anyBroadcast")
        intent.putExtra("data", data)
        GlobalManager.getInstance().getMainActivity()!!.sendBroadcast(intent)
    }

    @JvmStatic
    fun onSelectTrack(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            broadcastMsg("onSelectTrack", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onStartGame(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onStartGame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onRestartGame(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onRestartGame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onExitGame(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onExitgame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onGameover(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onGameover", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onEndGame(lastTrack: TrackInfo?, stat: StatisticV2?) {
        if (!isExtensionEnable() || lastTrack == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", lastTrack.filename)
            game.put("mods", JSONArray(stat!!.mod))
            game.put("score", stat.totalScoreWithMultiplier)
            game.put("combo", stat.maxCombo)
            game.put("hit300", stat.hit300)
            game.put("hit100", stat.hit100)
            game.put("hit50", stat.hit50)
            game.put("miss", stat.misses)
            broadcastMsg("onEndGame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onPauseGame(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onPauseGame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onResume(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onResumeGame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onQuitGame(info: TrackInfo?) {
        if (!isExtensionEnable() || info == null) {
            return
        }
        try {
            val game = JSONObject()
            game.put("file", info.filename)
            game.put("mods", JSONArray(ModMenu.getInstance().mod))
            game.put("gameId", 0)
            broadcastMsg("onQuitGame", game.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun openBeatmap(filepath: String): Boolean {
        if (!isExtensionEnable()) return false
        try {
            val intent = Intent()
            val componentName = ComponentName("com.edplan.osu.osudroidextension", "com.edplan.osu.osudroidextension.ApiActivity")
            intent.component = componentName
            intent.putExtra("api", "openOsuFile")
            try {
                val game = JSONObject()
                game.put("file", filepath)
                game.put("mods", JSONArray(ModMenu.getInstance().mod))
                intent.putExtra("data", game.toString())
                GlobalManager.getInstance().getMainActivity()!!.startActivity(intent)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return true
        } catch (e: ActivityNotFoundException) {
            ToastLogger.showText(StringTable.get(R.string.message_extension_not_found), false)
            return false
        }
    }
}
