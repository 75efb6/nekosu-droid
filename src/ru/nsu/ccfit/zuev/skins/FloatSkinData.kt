package ru.nsu.ccfit.zuev.skins

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultFloat

class FloatSkinData @JvmOverloads constructor(tag: String, defaultValue: Float = DefaultFloat().currentValue) :
    SkinData<Float>(tag, DefaultFloat(defaultValue)) {

    override fun setFromJson(data: JSONObject) {
        currentValue = data.optDouble(getTag(), defaultValue.toDouble()).toFloat()
    }
}
