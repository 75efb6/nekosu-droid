package ru.nsu.ccfit.zuev.osu.beatmap.sections

import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.beatmap.ComboColor

class BeatmapColor {
    var comboColors: ArrayList<ComboColor> = ArrayList()

    var sliderBorderColor: RGBColor? = null

    constructor()

    private constructor(source: BeatmapColor) {
        for (color in source.comboColors) {
            comboColors.add(color.deepClone())
        }

        sliderBorderColor = source.sliderBorderColor?.let { RGBColor(it) }
    }

    fun deepClone(): BeatmapColor = BeatmapColor(this)
}
