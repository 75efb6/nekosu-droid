package ru.nsu.ccfit.zuev.osu

import java.io.Serializable

class TrackInfo(var beatmap: BeatmapInfo?) : Serializable {
    var filename: String? = null
    var publicName: String? = null
    var mode: String? = null
    var creator: String? = null
    var md5: String? = null
    var background: String? = null
    var beatmapID: Int = 0
    var beatmapSetID: Int = 0
    var difficulty: Float = 0f
    var hpDrain: Float = 0f
    var overallDifficulty: Float = 0f
    var approachRate: Float = 0f
    var circleSize: Float = 0f
    var bpmMax: Float = 0f
    var bpmMin: Float = Float.MAX_VALUE
    var musicLength: Long = 0
    var hitCircleCount: Int = 0
    var sliderCount: Int = 0
    var spinnerCount: Int = 0
    var totalHitObjectCount: Int = 0
    var maxCombo: Int = 0
    var audioFilename: String? = null
    var previewTime: Int = -1

    fun setMD5(md5: String?) {
        this.md5 = md5
    }

    fun getMD5(): String? = md5

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o is TrackInfo) {
            val track = o
            return md5 != null && track.md5 != null && track.md5 == md5
        }
        return false
    }

    companion object {
        private const val serialVersionUID = 2049627581836712912L
    }
}
