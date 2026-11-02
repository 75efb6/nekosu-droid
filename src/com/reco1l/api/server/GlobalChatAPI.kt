package com.reco1l.api.server

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Global chat namespace client. Connects to /global_chat.
 */
object GlobalChatAPI {

    private var socket: Socket? = null
    private var connected = false

    private var messageListener: ((JSONObject) -> Unit)? = null
    private var historyListener: ((Array<Any>) -> Unit)? = null

    fun connect(userId: Long, username: String) {
        if (connected && socket?.connected() == true) return

        socket?.off()
        socket = null

        val opts = IO.Options().apply {
            reconnection = true
            reconnectionAttempts = 5
            reconnectionDelay = 3000
            auth = mutableMapOf<String, String>().apply {
                put("uid", userId.toString())
                put("username", username)
            }
        }

        socket = IO.socket("${ServerAPI.HOST}/global_chat", opts).apply {
            on(Socket.EVENT_CONNECT) {
                connected = true
            }
            on(Socket.EVENT_DISCONNECT) {
                connected = false
            }
            on(Socket.EVENT_CONNECT_ERROR) {
                connected = false
            }
            on("chat_message") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                messageListener?.invoke(data)
            }
            on("chat_history") { args ->
                historyListener?.invoke(args)
            }
            connect()
        }

        // Wait for connection
        socket?.let { s ->
            if (!s.connected()) {
                val latch = CountDownLatch(1)
                s.on(Socket.EVENT_CONNECT) { latch.countDown() }
                s.on(Socket.EVENT_CONNECT_ERROR) { latch.countDown() }
                latch.await(5, TimeUnit.SECONDS)
            }
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        connected = false
        messageListener = null
        historyListener = null
    }

    val isConnected: Boolean get() = connected && socket?.connected() == true

    fun sendMessage(userId: Long, content: String) {
        val s = socket ?: return
        if (!s.connected()) return
        val data = JSONObject().apply {
            put("userId", userId.toString())
            put("content", content)
        }
        s.emit("chat_message", data)
    }

    fun onMessage(listener: (JSONObject) -> Unit) {
        messageListener = listener
    }

    fun onHistory(listener: (Array<Any>) -> Unit) {
        historyListener = listener
    }
}
