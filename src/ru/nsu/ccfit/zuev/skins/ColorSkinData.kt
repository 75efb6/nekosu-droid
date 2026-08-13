package ru.nsu.ccfit.zuev.skins

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultRGBColor

class ColorSkinData(tag: String, private val defaultHex: String) :
    SkinData<RGBColor>(tag, DefaultRGBColor(RGBColor.hex2Rgb(defaultHex))) {

    private var currentHex: String = defaultHex

    override fun setFromJson(data: JSONObject) {
        val hex = data.optString(getTag())
        if (hex.isEmpty()) {
            currentHex = defaultHex
            currentValue = defaultValue
        } else {
            currentHex = hex
            currentValue = RGBColor.hex2Rgb(hex)
        }
    }

    override fun currentIsDefault(): Boolean {
        return currentHex.equals(defaultHex, ignoreCase = true)
    }
}
