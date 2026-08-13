package ru.nsu.ccfit.zuev.osu.beatmap

import ru.nsu.ccfit.zuev.osu.RGBColor

class ComboColor(val index: Int, color: RGBColor) : RGBColor(color) {

    private constructor(source: ComboColor) : this(source.index, RGBColor(source.r(), source.g(), source.b()))

    fun deepClone(): ComboColor = ComboColor(this)
}
