package com.reco1l.api.server

import android.util.Log
import com.reco1l.legacy.ui.spectate.SpectateFrame
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.game.cursor.main.Cursor

/**
 * Spectate API. All calls go through [ServerAPI]'s socket connection.
 *
 * The target (player being watched) uses: sendMapStart, sendMapPause, sendMapFinish, sendSpectateData, sendSpectateEnd.
 * The spectator (watcher) uses: startSpectating, stopSpectating, onData/onState/onEnded callbacks.
 *
 * spectatorCount is updated via the global spectate_count socket listener in ServerAPI.registerNotificationListeners().
 * Do NOT clear spectatorCount in clearListeners() — it is target-side state.
 */
object SpectateAPI {

    private var dataListener: ((JSONObject) -> Unit)? = null
    private var stateListener: ((JSONObject) -> Unit)? = null
    private var endedListener: ((Long) -> Unit)? = null
    private var startedListener: ((JSONObject) -> Unit)? = null
    private var errorListener: ((String) -> Unit)? = null
    private var countListener: ((Int) -> Unit)? = null
    private var pausedListener: ((JSONObject) -> Unit)? = null
    private var finishedListener: ((JSONObject) -> Unit)? = null
    private var deathListener: ((JSONObject) -> Unit)? = null
    private var skipListener: ((JSONObject) -> Unit)? = null
    private var resumeListener: ((JSONObject) -> Unit)? = null

    @Volatile
    var spectatorCount = 0
        private set

    // Buffered last state so SpectateScene can replay it even if it registered after the event
    @Volatile
    private var lastStateEvent: JSONObject? = null

    // Stored socket listener references for proper cleanup
    private val socketListeners = mutableListOf<Pair<String, Emitter.Listener>>()

    private fun registerSocketListener(event: String, handler: (Array<Any?>) -> Unit) {
        val listener = Emitter.Listener(handler)
        socketListeners.add(event to listener)
        ServerAPI.on(event, listener)
    }

    fun startSpectating(targetId: Long) {
        // Remove any previous socket listeners to prevent stacking
        removeSocketListeners()

        // Register spectator-side socket listeners
        registerSocketListener("spectate_started") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            startedListener?.invoke(data)
        }
        registerSocketListener("spectate_data") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            dataListener?.invoke(data)
        }
        registerSocketListener("spectate_state") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            lastStateEvent = data
            stateListener?.invoke(data)
        }
        registerSocketListener("spectate_ended") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            val uid = data.optLong("targetId", 0)
            if (uid > 0) endedListener?.invoke(uid)
        }
        registerSocketListener("spectate_error") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            val error = data.optString("error", "Unknown error")
            errorListener?.invoke(error)
        }
        registerSocketListener("spectate_paused") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            pausedListener?.invoke(data)
        }
        registerSocketListener("spectate_finished") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            finishedListener?.invoke(data)
        }
        registerSocketListener("spectate_death") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            deathListener?.invoke(data)
        }
        registerSocketListener("spectate_skip") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            skipListener?.invoke(data)
        }
        registerSocketListener("spectate_resume") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@registerSocketListener
            resumeListener?.invoke(data)
        }

        lastStateEvent = null

        val json = JSONObject().apply {
            put("targetId", targetId.toString())
        }
        ServerAPI.emit("spectate_start", json)
    }

    fun stopSpectating(targetId: Long) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
        }
        ServerAPI.emit("spectate_stop", json)
        clearListeners()
    }

    private fun removeSocketListeners() {
        for ((event, listener) in socketListeners) {
            ServerAPI.off(event, listener)
        }
        socketListeners.clear()
    }

    // =========================================================
    // TARGET → SERVER: frame data, state sync, map events
    // =========================================================

    fun sendSpectateData(
        targetId: Long,
        timeMs: Int,
        cursors: Array<Cursor?>,
        score: Int,
        combo: Int,
        acc: Float,
        hp: Float,
        alive: Boolean,
        hits: List<Pair<Int, Int>>
    ) {
        val cursorsArr = JSONArray()
        for (i in 0 until 3) {
            val c = cursors[i]
            val cursorObj = JSONObject().apply {
                put("x", c?.mousePos?.x?.toDouble() ?: 0.0)
                put("y", c?.mousePos?.y?.toDouble() ?: 0.0)
                put("d", c?.mouseDown ?: false)
            }
            cursorsArr.put(cursorObj)
        }

        val hitsArr = JSONArray()
        for ((id, result) in hits) {
            val hitObj = JSONObject().apply {
                put("id", id)
                put("r", result)
            }
            hitsArr.put(hitObj)
        }

        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("t", timeMs)
            put("c", cursorsArr)
            put("s", JSONObject().apply {
                put("score", score)
                put("combo", combo)
                put("acc", acc)
                put("hp", hp)
                put("alive", alive)
            })
            put("h", hitsArr)
        }
        ServerAPI.emit("spectate_data", json)
    }

    /**
     * Send a batch of spectate frames in a single emission to reduce network overhead.
     * The server relays each frame to spectators.
     */
    fun sendSpectateDataBatch(
        targetId: Long,
        frames: List<SpectateFrame>
    ) {
        if (frames.isEmpty()) return

        val framesArr = JSONArray()
        for (frame in frames) {
            val cursorsArr = JSONArray()
            for (i in 0 until 3) {
                val c = frame.cursors[i]
                cursorsArr.put(JSONObject().apply {
                    put("x", c?.mousePos?.x?.toDouble() ?: 0.0)
                    put("y", c?.mousePos?.y?.toDouble() ?: 0.0)
                    put("d", c?.mouseDown ?: false)
                })
            }

            val hitsArr = JSONArray()
            for ((id, result) in frame.hits) {
                hitsArr.put(JSONObject().apply {
                    put("id", id)
                    put("r", result)
                })
            }

            framesArr.put(JSONObject().apply {
                put("t", frame.timeMs)
                put("c", cursorsArr)
                put("s", JSONObject().apply {
                    put("score", frame.score)
                    put("combo", frame.combo)
                    put("acc", frame.acc.toDouble())
                    put("hp", frame.hp.toDouble())
                    put("alive", frame.alive as Any)
                })
                put("h", hitsArr)
            })
        }

        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("batch", framesArr)
        }
        ServerAPI.emit("spectate_data_batch", json)
    }

    fun sendSpectateState(
        targetId: Long,
        beatmapMd5: String,
        timeMs: Int,
        mods: String,
        score: Int,
        combo: Int,
        acc: Float,
        hp: Float,
        alive: Boolean
    ) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("beatmapMd5", beatmapMd5)
            put("time", timeMs)
            put("mods", mods)
            put("score", score)
            put("combo", combo)
            put("acc", acc)
            put("hp", hp)
            put("alive", alive)
        }
        ServerAPI.emit("spectate_state", json)
    }

    fun sendSpectateEnd(targetId: Long) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
        }
        ServerAPI.emit("spectate_end", json)
    }

    fun sendSpectateDeath(targetId: Long) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
        }
        ServerAPI.emit("spectate_death", json)
    }

    fun sendSpectateSkip(targetId: Long, timeMs: Int) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("timeMs", timeMs)
        }
        ServerAPI.emit("spectate_skip", json)
    }

    fun sendSpectateResume(targetId: Long) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
        }
        ServerAPI.emit("spectate_resume", json)
    }

    fun sendMapStart(targetId: Long, beatmapMd5: String, mods: String = "") {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("beatmapMd5", beatmapMd5)
            put("mods", mods)
        }
        ServerAPI.emit("spectate_map_start", json)
    }

    fun sendMapPause(targetId: Long, timeMs: Int) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("timeMs", timeMs)
        }
        ServerAPI.emit("spectate_map_pause", json)
    }

    fun sendMapFinish(targetId: Long, score: Int, combo: Int, acc: Float, rank: String) {
        val json = JSONObject().apply {
            put("targetId", targetId.toString())
            put("score", score)
            put("combo", combo)
            put("acc", acc)
            put("rank", rank)
        }
        ServerAPI.emit("spectate_map_finish", json)
    }

    // =========================================================
    // SPECTATOR-SIDE LISTENERS
    // =========================================================

    fun onData(listener: (JSONObject) -> Unit) {
        dataListener = listener
    }

    fun onState(listener: (JSONObject) -> Unit) {
        stateListener = listener
        // If we already have a buffered state, deliver it now
        lastStateEvent?.let { state ->
            listener.invoke(state)
        }
    }

    fun onEnded(listener: (Long) -> Unit) {
        endedListener = listener
    }

    fun onStarted(listener: (JSONObject) -> Unit) {
        startedListener = listener
    }

    fun onError(listener: (String) -> Unit) {
        errorListener = listener
    }

    fun onCount(listener: (Int) -> Unit) {
        countListener = listener
    }

    fun onPaused(listener: (JSONObject) -> Unit) {
        pausedListener = listener
    }

    fun onFinished(listener: (JSONObject) -> Unit) {
        finishedListener = listener
    }

    fun onDeath(listener: (JSONObject) -> Unit) {
        deathListener = listener
    }

    fun onSkip(listener: (JSONObject) -> Unit) {
        skipListener = listener
    }

    fun onResume(listener: (JSONObject) -> Unit) {
        resumeListener = listener
    }

    fun clearListeners() {
        removeSocketListeners()
        dataListener = null
        stateListener = null
        endedListener = null
        startedListener = null
        errorListener = null
        countListener = null
        pausedListener = null
        finishedListener = null
        deathListener = null
        skipListener = null
        resumeListener = null
        lastStateEvent = null
        // NOTE: spectatorCount is NOT cleared here — it is target-side state
    }

    // =========================================================
    // CALLED BY ServerAPI when spectate_count event arrives
    // =========================================================

    fun onSpectatorCountReceived(count: Int) {
        spectatorCount = count
        Log.d("SpectateAPI", "Spectator count: $spectatorCount")
        countListener?.invoke(count)
    }
}
