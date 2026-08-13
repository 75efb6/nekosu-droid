package ru.nsu.ccfit.zuev.osu.beatmap.sections

class BeatmapMetadata {
    var title: String = ""
    var titleUnicode: String = ""
    var artist: String = ""
    var artistUnicode: String = ""
    var creator: String = ""
    var version: String = ""
    var source: String = ""
    var tags: String = ""
    var beatmapID: Int = -1
    var beatmapSetID: Int = -1

    constructor()

    private constructor(source: BeatmapMetadata) {
        title = source.title
        titleUnicode = source.titleUnicode
        artist = source.artist
        artistUnicode = source.artistUnicode
        creator = source.creator
        version = source.version
        this.source = source.source
        tags = source.tags
        beatmapID = source.beatmapID
        beatmapSetID = source.beatmapSetID
    }

    fun deepClone(): BeatmapMetadata = BeatmapMetadata(this)
}
