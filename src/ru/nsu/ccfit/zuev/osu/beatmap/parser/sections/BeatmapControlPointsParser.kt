package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData

class BeatmapControlPointsParser : BeatmapSectionParser() {
    override fun parse(data: BeatmapData, line: String) {
        val pars = line.split(",").toTypedArray()

        if (pars.size < 2) {
            throw UnsupportedOperationException("Malformed timing point")
        }

        val time = data.getOffsetTime(parseDouble(pars[0].trim { it <= ' ' }))

        val msPerBeat = parseDouble(pars[1].trim { it <= ' ' }, true)

        var timeSignature = 4
        if (pars.size >= 3) {
            timeSignature = parseInt(pars[2])
        }

        if (timeSignature < 1) {
            throw UnsupportedOperationException("The numerator of a time signature must be positive")
        }

        var timingChange = true
        if (pars.size >= 7) {
            timingChange = pars[6] == "1"
        }

        val manager: BeatmapControlPointsManager = data.timingPoints

        if (timingChange) {
            if (msPerBeat.isNaN()) {
                throw UnsupportedOperationException("Beat length cannot be NaN in a timing control point")
            }

            manager.timing.add(TimingControlPoint(time, msPerBeat, timeSignature))
        }

        manager.difficulty.add(
            DifficultyControlPoint(
                time,
                if (msPerBeat < 0) 100 / -msPerBeat else 1.0,
                !msPerBeat.isNaN()
            )
        )

        data.rawTimingPoints.add(line)
    }
}
