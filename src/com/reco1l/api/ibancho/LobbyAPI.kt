package com.reco1l.api.ibancho

import com.reco1l.api.ibancho.data.*
import com.reco1l.api.server.ServerAPI
import org.json.JSONArray
import org.json.JSONObject

object LobbyAPI
{
    /**
     * The hostname.
     */
    const val HOST = "http://10.0.0.103:3000/multi"

    /**
     * The invite link host.
     */
    const val INVITE_HOST = "https://nodmp"


    /**
     * Get room list.
     */
    fun getRooms(query: String?, sign: String?): List<Room>
    {
        val response = ServerAPI.getRooms() ?: return emptyList()

        val array = when (response) {
            is JSONArray -> response
            is JSONObject -> {
                val data = response.opt("data")
                when (data) {
                    is JSONArray -> data
                    else -> return emptyList()
                }
            }
            else -> return emptyList()
        }

        return List(array.length()) { i ->

            val json = array.optJSONObject(i) ?: return@List null

            runCatching {
                Room(
                    id = json.getLong("id"),
                    name = json.getString("name"),
                    isLocked = json.getBoolean("isLocked"),
                    maxPlayers = json.getInt("maxPlayers"),
                    mods = parseMods(json.getJSONObject("mods")),
                    gameplaySettings = parseGameplaySettings(json.getJSONObject("gameplaySettings")),
                    teamMode = TeamMode.from(json.getInt("teamMode")),
                    winCondition = WinCondition.from(json.getInt("winCondition")),
                    playerCount = json.getInt("playerCount"),
                    playerNames = json.getString("playerNames"),
                    status = RoomStatus.from(json.getInt("status"))
                )
            }.getOrNull()

        }.filterNotNull()
    }

    /**
     * Create room and get the ID.
     */
    fun createRoom(name: String, beatmap: RoomBeatmap?, hostUID: Long, hostUsername: String, sign: String?, password: String? = null, maxPlayers: Int = 8): Long
    {
        val beatmapJson = if (beatmap != null) {
            JSONObject().apply {
                put("md5", beatmap.md5)
                put("title", beatmap.title)
                put("artist", beatmap.artist)
                put("creator", beatmap.creator)
                put("version", beatmap.version)
            }
        } else null

        val response = ServerAPI.createRoom(
            name = name,
            beatmap = beatmapJson,
            hostUID = hostUID,
            hostUsername = hostUsername,
            password = password,
            maxPlayers = maxPlayers
        )

        return response?.getString("id")?.toLongOrNull() ?: -1
    }
}
