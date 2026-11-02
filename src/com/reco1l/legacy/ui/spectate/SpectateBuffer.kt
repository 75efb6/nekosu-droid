package com.reco1l.legacy.ui.spectate

import org.json.JSONObject

/**
 * Buffer for spectate data packets. Stores incoming cursor/score data
 * for smooth playback when spectating.
 */
class SpectateBuffer {

    val packets = mutableListOf<SpectatePacket>()
    var currentState: SpectateState? = null
        private set

    @Volatile
    private var writeIndex = 0

    // Minimum packets needed before playback starts (prevents playing stale/empty data)
    private val MIN_BUFFER_THRESHOLD = 1

    fun addPacket(packet: SpectatePacket) {
        synchronized(packets) {
            packets.add(packet)
            // Keep buffer bounded (~10 seconds at 12fps)
            if (packets.size > 120) {
                packets.removeAt(0)
            }
        }
    }

    fun addPackets(newPackets: List<SpectatePacket>) {
        synchronized(packets) {
            packets.addAll(newPackets)
            while (packets.size > 150) {
                packets.removeAt(0)
            }
        }
    }

    fun updateState(state: SpectateState) {
        currentState = state
        // Clear buffer on new game state
        synchronized(packets) {
            packets.clear()
        }
    }

    /**
     * Get the packet that should be active at the given time.
     * Returns null if no packet is available for that time.
     */
    fun getPacketAt(timeMs: Int): SpectatePacket? {
        synchronized(packets) {
            if (packets.size < MIN_BUFFER_THRESHOLD) return null

            // Find the latest packet with time <= timeMs
            var result: SpectatePacket? = null
            for (packet in packets) {
                if (packet.time <= timeMs) {
                    result = packet
                } else {
                    break
                }
            }
            // If spectator is behind all packets, return the earliest packet
            if (result == null && packets.isNotEmpty()) {
                return packets[0]
            }
            return result
        }
    }

    /**
     * Get all packets in a time range.
     */
    fun getPacketsInRange(startMs: Int, endMs: Int): List<SpectatePacket> {
        synchronized(packets) {
            return packets.filter { it.time in startMs..endMs }
        }
    }

    /**
     * Get the most recent packet.
     */
    fun getLatestPacket(): SpectatePacket? {
        synchronized(packets) {
            return packets.lastOrNull()
        }
    }

    fun clear() {
        synchronized(packets) {
            packets.clear()
        }
        currentState = null
    }

    fun isEmpty(): Boolean {
        synchronized(packets) {
            return packets.isEmpty()
        }
    }
}

/**
 * A single frame of spectate data.
 */
data class SpectatePacket(
    val time: Int,
    val cursors: Array<SpectateCursor>,
    val score: Int,
    val combo: Int,
    val acc: Float,
    val hp: Float,
    val alive: Boolean,
    val hits: List<HitResult>
) {
    companion object {
        fun fromJson(json: JSONObject): SpectatePacket {
            val cursorsArr = json.optJSONArray("c") ?: org.json.JSONArray()
            val cursors = Array(3) { i ->
                if (i < cursorsArr.length()) {
                    val c = cursorsArr.optJSONObject(i) ?: JSONObject()
                    SpectateCursor(
                        x = c.optDouble("x", 0.0).toFloat(),
                        y = c.optDouble("y", 0.0).toFloat(),
                        down = c.optBoolean("d", false)
                    )
                } else {
                    SpectateCursor(0f, 0f, false)
                }
            }

            val scoreObj = json.optJSONObject("s") ?: JSONObject()
            val hitsArr = json.optJSONArray("h") ?: org.json.JSONArray()
            val hits = mutableListOf<HitResult>()
            for (i in 0 until hitsArr.length()) {
                val h = hitsArr.optJSONObject(i) ?: continue
                hits.add(HitResult(h.optInt("id", 0), h.optInt("r", 0)))
            }

            return SpectatePacket(
                time = json.optInt("t", 0),
                cursors = cursors,
                score = scoreObj.optInt("score", 0),
                combo = scoreObj.optInt("combo", 0),
                acc = scoreObj.optDouble("acc", 0.0).toFloat(),
                hp = scoreObj.optDouble("hp", 1.0).toFloat(),
                alive = scoreObj.optBoolean("alive", true),
                hits = hits
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpectatePacket) return false
        return time == other.time
    }

    override fun hashCode(): Int = time
}

/**
 * Cursor position in a spectate packet.
 */
data class SpectateCursor(val x: Float, val y: Float, val down: Boolean)

/**
 * Hit result in a spectate packet.
 */
data class HitResult(val objectId: Int, val result: Int)

/**
 * A single frame for batch sending.
 */
data class SpectateFrame(
    val timeMs: Int,
    val cursors: Array<ru.nsu.ccfit.zuev.osu.game.cursor.main.Cursor?>,
    val score: Int,
    val combo: Int,
    val acc: Float,
    val hp: Float,
    val alive: Boolean,
    val hits: List<Pair<Int, Int>>
)

/**
 * Full game state for sync.
 */
data class SpectateState(
    val beatmapMd5: String,
    val timeMs: Int,
    val mods: String,
    val score: Int,
    val combo: Int,
    val acc: Float,
    val hp: Float,
    val alive: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): SpectateState {
            return SpectateState(
                beatmapMd5 = json.optString("beatmapMd5", ""),
                timeMs = json.optInt("time", 0),
                mods = json.optString("mods", ""),
                score = json.optInt("score", 0),
                combo = json.optInt("combo", 0),
                acc = json.optDouble("acc", 0.0).toFloat(),
                hp = json.optDouble("hp", 1.0).toFloat(),
                alive = json.optBoolean("alive", true)
            )
        }
    }
}
