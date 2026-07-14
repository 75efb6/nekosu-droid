package ru.nsu.ccfit.zuev.skins

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultBoolean

class BooleanSkinData @JvmOverloads constructor(tag: String, defaultValue: Boolean = DefaultBoolean().currentValue) :
    SkinData<Boolean>(tag, DefaultBoolean(defaultValue)) {

    override fun setFromJson(data: JSONObject) {
        currentValue = data.optBoolean(getTag(), defaultValue)
    }
}
