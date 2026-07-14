package ru.nsu.ccfit.zuev.osu.beatmap.constants

enum class BeatmapSection {
    general,
    editor,
    metadata,
    difficulty,
    events,
    timingPoints,
    colors,
    hitObjects;

    companion object {
        @JvmStatic
        fun parse(value: String): BeatmapSection? = when (value) {
            "General" -> general
            "Editor" -> editor
            "Metadata" -> metadata
            "Difficulty" -> difficulty
            "Events" -> events
            "TimingPoints" -> timingPoints
            "Colours" -> colors
            "HitObjects" -> hitObjects
            else -> null
        }
    }
}
