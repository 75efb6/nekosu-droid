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

    fun getSource(): String? = source

    fun setSource(source: String?) {
        this.source = source
    }

    fun getTags(): String? = tags

    fun setTags(tags: String?) {
        this.tags = tags
    }

    fun getMusic(): String? = tracks[0].audioFilename

    fun getTitle(): String? = title

    fun setTitle(title: String?) {
        this.title = title
    }

    fun getArtist(): String? = artist

    fun setArtist(artist: String?) {
        this.artist = artist
    }

    fun getTitleUnicode(): String? = titleUnicode

    fun setTitleUnicode(titleUnicode: String?) {
        this.titleUnicode = titleUnicode
    }

    fun getArtistUnicode(): String? = artistUnicode

    fun setArtistUnicode(artistUnicode: String?) {
        this.artistUnicode = artistUnicode
    }

    fun getCreator(): String? = creator

    fun setCreator(creator: String?) {
        this.creator = creator
    }

    fun getPath(): String? = path

    fun setPath(path: String?) {
        this.path = path
    }

    fun addTrack(track: TrackInfo) {
        tracks.add(track)
    }

    fun getTrack(index: Int): TrackInfo = tracks[index]

    fun getCount(): Int = tracks.size

    fun getTracks(): ArrayList<TrackInfo> = tracks

    fun getDate(): Long = date

    fun setDate(date: Long) {
        this.date = date
    }

    fun getPreviewTime(): Int = tracks[0].previewTime

    override fun equals(o: Any?): Boolean {
        return o === this || o is BeatmapInfo && o.getPath() == path
    }

    companion object {
        private const val serialVersionUID = -3865268984942011628L
    }
}
