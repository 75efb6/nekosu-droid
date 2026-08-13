package ru.nsu.ccfit.zuev.osu.datatypes

import ru.nsu.ccfit.zuev.osu.RGBColor

class DefaultRGBColor(defaultValue: RGBColor) : DefaultData<RGBColor>(defaultValue) {
    val instanceDefaultHex: String = "#FFFFFF"

    fun instanceDefaultHex(): String = instanceDefaultHex

    override fun instanceDefaultValue(): RGBColor = RGBColor.hex2Rgb(instanceDefaultHex)
}
