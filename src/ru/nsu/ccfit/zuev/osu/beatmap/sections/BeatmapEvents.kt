package ru.nsu.ccfit.zuev.osu.beatmap.sections

import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.game.BreakPeriod

class BeatmapEvents {
    var backgroundFilename: String? = null
    var videoFilename: String? = null
    var videoStartTime: Int = 0
    var breaks: ArrayList<BreakPeriod> = ArrayList()
    var backgroundColor: RGBColor? = null

    constructor()

    private constructor(source: BeatmapEvents) {
        backgroundFilename = source.backgroundFilename

        for (breakPeriod in source.breaks) {
            breaks.add(BreakPeriod(breakPeriod.start, breakPeriod.start + breakPeriod.length))
        }

        backgroundColor = source.backgroundColor?.let { RGBColor(it) }
        videoFilename = source.videoFilename
        videoStartTime = source.videoStartTime
    }

    fun deepClone(): BeatmapEvents = BeatmapEvents(this)
}
