package com.reco1l.api.server

import com.dgsrz.bancho.security.SecurityUtils
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter.Listener
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unified Socket.IO API client. Manages a single connection to the server root namespace.
 * All API calls go through this socket instead of HTTP.
 */
object ServerAPI {

    const val HOST = "http://10.0.0.103:3000"
    private const val RESPONSE_TIMEOUT = 10L

    private var socket: Socket? = null
    private var connected = false

    /**
     * Connect to the server's main API namespace.
     */
    fun connect(userId: Long, username: String) {
        android.util.Log.d("ServerAPI", "connect() uid=$userId username=$username connected=$connected socketConnected=${socket?.connected()}")
        if (connected && socket?.connected() == true) return

        socket?.off()
        socket = null

        val opts = IO.Options().apply {
            reconnection = true
            reconnectionAttempts = 5
            reconnectionDelay = 2000
            auth = mutableMapOf<String, String>().apply {
                put("uid", userId.toString())
                put("username", username)
            }
        }

        socket = IO.socket("$HOST/game", opts).apply {
            on(Socket.EVENT_CONNECT) {
                android.util.Log.i("ServerAPI", "CONNECTED sid=${socket?.id()}")
                connected = true
                registerNotificationListeners()
            }
            on(Socket.EVENT_DISCONNECT) {
                android.util.Log.w("ServerAPI", "DISCONNECTED")
                connected = false
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val err = args.getOrNull(0)
                android.util.Log.e("ServerAPI", "CONNECT_ERROR: $err", err as? Throwable)
                connected = false
            }
            connect()
        }

        // Wait for connection
        socket?.let { s ->
            if (!s.connected()) {
                android.util.Log.d("ServerAPI", "Waiting for connection...")
                val latch = CountDownLatch(1)
                s.on(Socket.EVENT_CONNECT) { android.util.Log.d("ServerAPI", "Latch: connected"); latch.countDown() }
                s.on(Socket.EVENT_CONNECT_ERROR) { android.util.Log.d("ServerAPI", "Latch: connect_error"); latch.countDown() }
                val ok = latch.await(RESPONSE_TIMEOUT, TimeUnit.SECONDS)
                android.util.Log.d("ServerAPI", "Latch result=$ok connected=${s.connected()}")
            }
        }
    }

    fun disconnect() {
        android.util.Log.d("ServerAPI", "disconnect()")
        socket?.off()
        socket?.disconnect()
        socket = null
        connected = false
    }

    private fun registerNotificationListeners() {
        val s = socket ?: return

        // Message received notification
        s.on("message_received") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on

            // Skip if actively playing a map
            val gameScene = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().gameScene
            val engine = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().engine
            if (gameScene != null && engine != null && engine.scene == gameScene.scene && gameScene.stat != null && !gameScene.isSpectating) {
                return@on
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                ru.nsu.ccfit.zuev.osu.ToastLogger.showText("Someone messaged you privately", false)
            }
        }

        // Friend online notification
        s.on("presence_changed") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val status = data.optString("status", "")
            if (status != "online") return@on

            val uid = data.optLong("userId", 0)
            if (uid <= 0) return@on

            // Skip self
            if (uid == ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance().userId) return@on

            // Skip if actively playing a map
            val gameScene = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().gameScene
            val engine = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().engine
            if (gameScene != null && engine != null && engine.scene == gameScene.scene && gameScene.stat != null && !gameScene.isSpectating) {
                return@on
            }

            // Fetch name async then show notification
            java.lang.Thread {
                val result = getUser(uid)
                val name = result?.optString("name") ?: "Friend"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    ru.nsu.ccfit.zuev.osu.ToastLogger.showText("$name is now online", false)
                }
            }.start()
        }

        // Spectate: target receives spectator count updates
        s.on("spectate_count") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val count = data.optInt("count", 0)
            com.reco1l.api.server.SpectateAPI.onSpectatorCountReceived(count)
        }

        // Friend request received notification
        s.on("friend_request") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on

            // Skip if actively playing a map
            val gameScene = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().gameScene
            val engine = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().engine
            if (gameScene != null && engine != null && engine.scene == gameScene.scene && gameScene.stat != null && !gameScene.isSpectating) {
                return@on
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                ru.nsu.ccfit.zuev.osu.ToastLogger.showText("Someone wants to be your friend", false)
            }
        }
    }

    val isConnected: Boolean get() = connected && socket?.connected() == true

    /**
     * Emit an event without waiting for a response.
     */
    fun emit(event: String, data: JSONObject? = null) {
        val s = socket ?: run { android.util.Log.w("ServerAPI", "emit($event): socket is null"); return }
        if (!s.connected()) { android.util.Log.w("ServerAPI", "emit($event): socket not connected"); return }
        if (data != null) {
            s.emit(event, data)
        } else {
            s.emit(event)
        }
    }

    /**
     * Register a listener for a server-pushed event.
     */
    fun on(event: String, listener: Listener) {
        android.util.Log.d("ServerAPI", "on($event) registered")
        socket?.on(event, listener)
    }

    /**
     * Remove a listener for a server-pushed event.
     */
    fun off(event: String, listener: Listener) {
        android.util.Log.d("ServerAPI", "off($event) removed")
        socket?.off(event, listener)
    }

    /**
     * Emit an event and wait for a response event.
     */
    internal fun emitAndWait(emitEvent: String, responseEvent: String, data: JSONObject? = null): JSONObject? {
        val s = socket ?: run { android.util.Log.e("ServerAPI", "emitAndWait($emitEvent): socket is null"); return null }
        if (!s.connected()) { android.util.Log.e("ServerAPI", "emitAndWait($emitEvent): socket not connected"); return null }

        android.util.Log.d("ServerAPI", "emitAndWait($emitEvent) -> waiting for $responseEvent")
        val latch = CountDownLatch(1)
        var result: JSONObject? = null

        val responseListener = Listener {
            result = it.getOrNull(0) as? JSONObject ?: JSONObject()
            android.util.Log.d("ServerAPI", "emitAndWait($emitEvent): got response")
            latch.countDown()
        }

        s.on(responseEvent, responseListener)

        if (data != null) {
            s.emit(emitEvent, data)
        } else {
            s.emit(emitEvent)
        }

        val ok = latch.await(RESPONSE_TIMEOUT, TimeUnit.SECONDS)
        if (!ok) android.util.Log.w("ServerAPI", "emitAndWait($emitEvent): TIMEOUT after ${RESPONSE_TIMEOUT}s")
        s.off(responseEvent, responseListener)

        return result
    }

    /**
     * Emit an event and wait for a response that is a raw object (not necessarily JSONObject).
     */
    internal fun emitAndWaitRaw(emitEvent: String, responseEvent: String, data: JSONObject? = null): Any? {
        val s = socket ?: run { android.util.Log.e("ServerAPI", "emitAndWaitRaw($emitEvent): socket is null"); return null }
        if (!s.connected()) { android.util.Log.e("ServerAPI", "emitAndWaitRaw($emitEvent): socket not connected"); return null }

        android.util.Log.d("ServerAPI", "emitAndWaitRaw($emitEvent) -> waiting for $responseEvent")
        val latch = CountDownLatch(1)
        var result: Any? = null

        val responseListener = Listener {
            result = it.getOrNull(0)
            android.util.Log.d("ServerAPI", "emitAndWaitRaw($emitEvent): got response")
            latch.countDown()
        }

        s.on(responseEvent, responseListener)

        if (data != null) {
            s.emit(emitEvent, data)
        } else {
            s.emit(emitEvent)
        }

        val ok = latch.await(RESPONSE_TIMEOUT, TimeUnit.SECONDS)
        if (!ok) android.util.Log.w("ServerAPI", "emitAndWaitRaw($emitEvent): TIMEOUT after ${RESPONSE_TIMEOUT}s")
        s.off(responseEvent, responseListener)

        return result
    }

    /**
     * Emit an event with a callback and return the first argument from the response.
     */
    internal fun emitWithCallback(emitEvent: String, responseEvent: String, data: JSONObject? = null): JSONObject? {
        val s = socket ?: run { android.util.Log.e("ServerAPI", "emitWithCallback($emitEvent): socket is null"); return null }
        if (!s.connected()) { android.util.Log.e("ServerAPI", "emitWithCallback($emitEvent): socket not connected"); return null }

        android.util.Log.d("ServerAPI", "emitWithCallback($emitEvent) -> waiting for $responseEvent")
        val latch = CountDownLatch(1)
        var result: JSONObject? = null

        val responseListener = Listener {
            val arg = it.getOrNull(0)
            result = when (arg) {
                is JSONObject -> arg
                else -> {
                    val json = JSONObject()
                    json.put("data", arg?.toString() ?: "")
                    json
                }
            }
            android.util.Log.d("ServerAPI", "emitWithCallback($emitEvent): got response status=${result?.optString("status")}")
            latch.countDown()
        }

        s.on(responseEvent, responseListener)

        if (data != null) {
            s.emit(emitEvent, data)
        } else {
            s.emit(emitEvent)
        }

        val ok = latch.await(RESPONSE_TIMEOUT, TimeUnit.SECONDS)
        if (!ok) android.util.Log.w("ServerAPI", "emitWithCallback($emitEvent): TIMEOUT after ${RESPONSE_TIMEOUT}s")
        s.off(responseEvent, responseListener)

        return result
    }

    // =========================================================
    // AUTH
    // =========================================================

    fun login(username: String, password: String, version: String, deviceID: String): JSONObject? {
        val data = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("version", version)
            put("deviceID", deviceID)
        }
        return emitWithCallback("login", "login_result", data)
    }

    // =========================================================
    // SUBMIT
    // =========================================================

    fun submitStart(userID: Long, ssid: String, filename: String, hash: String,
                    songTitle: String, songArtist: String, songCreator: String, songID: String? = null): JSONObject? {
        val data = JSONObject().apply {
            put("userID", userID.toString())
            put("ssid", ssid)
            put("filename", filename)
            put("hash", hash)
            put("songTitle", songTitle)
            put("songArtist", songArtist)
            put("songCreator", songCreator)
            if (songID != null) put("songID", songID)
        }
        return emitWithCallback("submit", "submit_result", data)
    }

    fun submitRecord(userID: Long, playID: String, recordData: String): JSONObject? {
        val data = JSONObject().apply {
            put("userID", userID.toString())
            put("playID", playID)
            put("data", recordData)
        }
        return emitWithCallback("submit", "submit_result", data)
    }

    // =========================================================
    // RANK
    // =========================================================

    fun getrank(filename: String, hash: String, uid: Long): JSONObject? {
        val data = JSONObject().apply {
            put("filename", filename)
            put("hash", hash)
            put("uid", uid.toString())
        }
        return emitWithCallback("getrank", "getrank_result", data)
    }

    fun gettop(playID: Int): JSONObject? {
        val data = JSONObject().apply {
            put("playID", playID.toString())
        }
        return emitWithCallback("gettop", "gettop_result", data)
    }

    // =========================================================
    // QUERY
    // =========================================================

    fun getUser(id: Long? = null, name: String? = null): JSONObject? {
        val data = JSONObject()
        if (id != null) data.put("id", id.toString())
        if (name != null) data.put("name", name)
        return emitWithCallback("get_user", "get_user_result", data)
    }

    fun getScores(id: Long, limit: Int = 50): Any? {
        val data = JSONObject().apply {
            put("id", id.toString())
            put("limit", limit)
        }
        return emitAndWaitRaw("get_scores", "get_scores_result", data)
    }

    fun getLeaderboard(): Any? {
        return emitAndWaitRaw("leaderboard", "leaderboard_result")
    }

    fun getOnline(): JSONObject? {
        return emitWithCallback("get_online", "get_online_result")
    }

    fun getBeatmap(md5: String? = null, bid: String? = null): JSONObject? {
        val data = JSONObject()
        if (md5 != null) data.put("md5", md5)
        if (bid != null) data.put("bid", bid)
        return emitWithCallback("beatmap", "beatmap_result", data)
    }

    fun getMapStatus(md5: String): JSONObject? {
        val data = JSONObject().apply {
            put("md5", md5)
        }
        return emitWithCallback("map_status", "map_status_result", data)
    }

    fun getUpdate(): JSONObject? {
        return emitWithCallback("update", "update_result")
    }

    fun calculate(md5: String? = null, bid: String? = null, mods: String = "",
                  acc: Float? = null, miss: Int? = null, combo: Int? = null): JSONObject? {
        val data = JSONObject()
        if (md5 != null) data.put("md5", md5)
        if (bid != null) data.put("bid", bid)
        data.put("mods", mods)
        if (acc != null) data.put("acc", acc.toString())
        if (miss != null) data.put("miss", miss.toString())
        if (combo != null) data.put("combo", combo.toString())
        return emitWithCallback("calculate", "calculate_result", data)
    }

    // =========================================================
    // LOBBY
    // =========================================================

    fun getRooms(): Any? {
        return emitAndWaitRaw("getrooms", "getrooms_result")
    }

    fun createRoom(name: String, beatmap: JSONObject?, hostUID: Long, hostUsername: String,
                   password: String? = null, maxPlayers: Int = 8): JSONObject? {
        val data = JSONObject().apply {
            put("name", name)
            put("maxPlayers", maxPlayers)

            put("host", JSONObject().apply {
                put("uid", hostUID.toString())
                put("username", hostUsername)
            })

            if (beatmap != null) {
                put("beatmap", beatmap)
            }

            if (!password.isNullOrBlank()) {
                put("password", password)
            }
        }
        return emitWithCallback("createroom", "createroom_result", data)
    }

    fun dumpState() {
        android.util.Log.w("ServerAPI", "=== STATE DUMP ===")
        android.util.Log.w("ServerAPI", "HOST=$HOST")
        android.util.Log.w("ServerAPI", "socket=${socket != null}")
        android.util.Log.w("ServerAPI", "connected=$connected")
        android.util.Log.w("ServerAPI", "socket.connected()=${socket?.connected()}")
        android.util.Log.w("ServerAPI", "socket.id()=${socket?.id()}")
        android.util.Log.w("ServerAPI", "socket.connected()=${socket?.connected()}")
        android.util.Log.w("ServerAPI", "=== END STATE ===")
    }
}
