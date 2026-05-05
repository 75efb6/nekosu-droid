package com.reco1l.legacy.discord

import com.my.kizzyrpc.KizzyRPC as LibKizzyRPC
import com.my.kizzyrpc.entities.presence.Activity
import com.my.kizzyrpc.entities.presence.Assets
import com.my.kizzyrpc.entities.presence.Timestamps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object KizzyRPC {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var rpc: LibKizzyRPC? = null

    private fun ensureConnected(): LibKizzyRPC? {
        val token = Config.getDiscordToken() ?: return null
        if (rpc == null) rpc = LibKizzyRPC(token)
        return rpc
    }

    private fun postActivity(activity: Activity) {
        if (!Config.isDiscordRichPresence()) return
        scope.launch {
            runCatching {
                val instance = ensureConnected() ?: return@launch
                if (instance.isRpcRunning()) {
                    instance.updateRPC(activity)
                } else {
                    instance.setActivity(activity)
                }
            }.onFailure { rpc = null }
        }
    }

    private fun assets(largeText: String = "User is offline.") = Assets(largeImage = "mp:attachments/1490482439766802552/1501012669665251508/logo.png?ex=69fa867b&is=69f934fb&hm=5d9cf8f0fc576572ae1c70b44f3b032b1ade1fde9b412bdb3bbb2fd2d2fd3d0c", smallImage = null, largeText = largeText)

    fun updateForMainMenu() {
        if (OnlineManager.getInstance().isStayOnline) {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "In main menu...",
                type = 0,
                assets = assets("${OnlineManager.getInstance().username}")
            ))
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "In main menu...",
                type = 0,
                assets = assets()
            ))
        }
    }

    fun updateForSongSelection() {
        if (OnlineManager.getInstance().isStayOnline) {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Selecting a song...",
                type = 0,
                assets = assets("${OnlineManager.getInstance().username}")
            ))
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Selecting a song...",
                type = 0,
                assets = assets()
            ))
        }
    }

    fun updateForMultiLobby() {
        if (OnlineManager.getInstance().isStayOnline) {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Searching for a room...",
                type = 0,
                assets = assets("${OnlineManager.getInstance().username}")
            ))
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Searching for a room...",
                type = 0,
                assets = assets()
            ))
        }
    }


    fun updateForMultiRoom(roomName: String, playerCount: Int, maxPlayers: Int) {
        if (OnlineManager.getInstance().isStayOnline) {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Inside a room...",
                state = "$roomName ($playerCount of $maxPlayers)",
                type = 0,
                assets = assets("${OnlineManager.getInstance().username}")
            ))
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Inside a room...",
                state = "$roomName ($playerCount of $maxPlayers)",
                type = 0,
                assets = assets()
            ))
        }
    }

    fun updateForPlaying(isMultiplayer: Boolean, title: String, artist: String, difficulty: String, startMs: Long) {
        if (OnlineManager.getInstance().isStayOnline) {
            if (isMultiplayer) {
                    postActivity(Activity(
                    name = "nekosu!droid",
                    details = "Playing a beatmap in a multiplayer game...",
                    state = "$artist - $title [$difficulty]",
                    type = 0,
                    timestamps = Timestamps(start = startMs),
                    assets = assets("${OnlineManager.getInstance().username}")
                ))
            } else {        
                postActivity(Activity(
                    name = "nekosu!droid",
                    details = "Playing a beatmap...",
                    state = "$artist - $title [$difficulty]",
                    type = 0,
                    timestamps = Timestamps(start = startMs),
                    assets = assets("${OnlineManager.getInstance().username}")
                ))
            }
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Playing a beatmap...",
                state = "$artist - $title [$difficulty]",
                type = 0,
                timestamps = Timestamps(start = startMs),
                assets = assets()
            ))
        }
    }

    fun updateForReplay(artist: String, title: String, difficulty: String, startMs: Long) {
        if (OnlineManager.getInstance().isStayOnline) {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Viewing a replay...",
                state = "$artist - $title [$difficulty]",
                type = 0,
                timestamps = Timestamps(start = startMs),
                assets = assets("${OnlineManager.getInstance().username}")
            ))
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "Viewing a replay...",
                state = "$artist - $title [$difficulty]",
                type = 0,
                timestamps = Timestamps(start = startMs),
                assets = assets()
            ))
        }
    }

    fun updateForResults() {
        if (OnlineManager.getInstance().isStayOnline) {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "In results screen...",
                type = 0,
                assets = assets("${OnlineManager.getInstance().username}")
            ))
        } else {
            postActivity(Activity(
                name = "nekosu!droid",
                details = "In results screen...",
                type = 0,
                assets = assets()
            ))
        }
    }

    fun clear() {
        scope.launch {
            runCatching { rpc?.closeRPC() }
            rpc = null
        }
    }

    fun disconnect() {
        scope.launch {
            runCatching { rpc?.closeRPC() }
            rpc = null
        }
    }
}
