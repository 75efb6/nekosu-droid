package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.game.BreakPeriod

class BeatmapEventsParser : BeatmapSectionParser() {
    override fun parse(data: BeatmapData, line: String) {
        val pars = line.split("\\s*,\\s*".toRegex()).toTypedArray()

        if (pars.size >= 3) {
            if (line.startsWith("0,0")) {
                data.events.backgroundFilename = pars[2].substring(1, pars[2].length - 1)
            }

            if (line.startsWith("2") || line.startsWith("Break")) {
                val start = data.getOffsetTime(parseInt(pars[1])).toFloat()
                val end = Math.max(start, data.getOffsetTime(parseInt(pars[2])).toFloat())
                data.events.breaks.add(BreakPeriod(start, end))
            }

            if (line.startsWith("1") || line.startsWith("Video")) {
                data.events.videoStartTime = parseInt(pars[1])
                data.events.videoFilename = pars[2].substring(1, pars[2].length - 1)
            }
        }

        if (pars.size >= 5 && line.startsWith("3")) {
            data.events.backgroundColor = RGBColor(
                parseInt(pars[2]).toFloat() / 255f,
                parseInt(pars[3]).toFloat() / 255f,
                parseInt(pars[4]).toFloat() / 255f
            )
        }
    }
}
