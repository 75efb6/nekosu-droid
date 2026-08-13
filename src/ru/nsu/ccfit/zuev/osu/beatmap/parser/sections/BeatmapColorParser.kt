package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.ComboColor

class BeatmapColorParser : BeatmapKeyValueSectionParser() {
    override fun parse(data: BeatmapData, line: String) {
        val p = splitProperty(line)
        val s = p[1].split(",").toTypedArray()

        if (s.size != 3 && s.size != 4) {
            throw UnsupportedOperationException("Color specified in incorrect format (should be R,G,B or R,G,B,A)")
        }

        val color = RGBColor(
            parseInt(s[0]).toFloat() / 255f,
            parseInt(s[1]).toFloat() / 255f,
            parseInt(s[2]).toFloat() / 255f
        )

        if (p[0].startsWith("Combo")) {
            val index = Utils.tryParseInt(p[0].substring(5), data.colors.comboColors.size + 1)
            data.colors.comboColors.add(ComboColor(index, color))
            data.colors.comboColors.sortWith(compareBy { it.index })
        }

        if (p[0].startsWith("SliderBorder")) {
            data.colors.sliderBorderColor = color
        }
    }
}
