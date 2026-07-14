package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import com.edlplan.framework.math.FMath
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData

class BeatmapDifficultyParser : BeatmapKeyValueSectionParser() {
    override fun parse(data: BeatmapData, line: String) {
        val p = splitProperty(line)

        when (p[0]) {
            "CircleSize" -> data.difficulty.cs = parseFloat(p[1])
            "OverallDifficulty" -> {
                data.difficulty.od = parseFloat(p[1])
                if (data.difficulty.ar.isNaN()) {
                    data.difficulty.ar = data.difficulty.od
                }
            }
            "ApproachRate" -> data.difficulty.ar = parseFloat(p[1])
            "HPDrainRate" -> data.difficulty.hp = parseFloat(p[1])
            "SliderMultiplier" -> data.difficulty.sliderMultiplier = FMath.clamp(parseDouble(p[1]), 0.4, 3.6)
            "SliderTickRate" -> data.difficulty.sliderTickRate = FMath.clamp(parseDouble(p[1]), 0.5, 8.0)
        }
    }
}
