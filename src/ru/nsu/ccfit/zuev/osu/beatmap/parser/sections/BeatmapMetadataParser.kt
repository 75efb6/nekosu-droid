package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData

class BeatmapMetadataParser : BeatmapKeyValueSectionParser() {
    override fun parse(data: BeatmapData, line: String) {
        val p = splitProperty(line)

        when (p[0]) {
            "Title" -> data.metadata.title = p[1]
            "TitleUnicode" -> data.metadata.titleUnicode = p[1]
            "Artist" -> data.metadata.artist = p[1]
            "ArtistUnicode" -> data.metadata.artistUnicode = p[1]
            "Creator" -> data.metadata.creator = p[1]
            "Version" -> data.metadata.version = p[1]
            "Source" -> data.metadata.source = p[1]
            "Tags" -> data.metadata.tags = p[1]
            "BeatmapID" -> data.metadata.beatmapID = parseInt(p[1])
            "BeatmapSetID" -> data.metadata.beatmapSetID = parseInt(p[1])
        }
    }
}
