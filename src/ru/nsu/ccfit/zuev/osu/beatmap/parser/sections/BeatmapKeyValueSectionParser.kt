package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

abstract class BeatmapKeyValueSectionParser : BeatmapSectionParser() {
    protected fun splitProperty(line: String): Array<String> {
        val colon = line.indexOf(':')
        return if (colon < 0) {
            arrayOf(line.trim(), "")
        } else {
            arrayOf(
                line.substring(0, colon).trim(),
                line.substring(colon + 1).trim()
            )
        }
    }
}
