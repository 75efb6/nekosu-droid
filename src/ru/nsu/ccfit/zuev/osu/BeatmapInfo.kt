package ru.nsu.ccfit.zuev.osu

import java.io.Serializable
import java.util.ArrayList

class BeatmapInfo : Serializable {
    private val tracks = ArrayList<TrackInfo>()
    var title: String? = null
    var titleUnicode: String? = null
    var artist: String? = null
    var artistUnicode: String? = null
    var creator: String? = null
    var path: String? = null
    var source: String? = null
    var tags: String? = null
    var date: Long = 0

    fun getMusic(): String? = tracks[0].audioFilename

    fun addTrack(track: TrackInfo) {
        tracks.add(track)
    }

    fun getTrack(index: Int): TrackInfo = tracks[index]

    fun getCount(): Int = tracks.size

    fun getTracks(): ArrayList<TrackInfo> = tracks

    fun getPreviewTime(): Int = tracks[0].previewTime

    override fun equals(o: Any?): Boolean {
        return o === this || o is BeatmapInfo && o.path == path
    }

    companion object {
        private const val serialVersionUID = -3865268984942011628L
    }
}
