package ru.nsu.ccfit.zuev.skins

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultData
import ru.nsu.ccfit.zuev.osu.datatypes.IDefaultableData

abstract class SkinData<I>(internal val tag: String, private val data: DefaultData<I>) : IDefaultableData<I> {

    fun getTag(): String = tag

    override val defaultValue: I get() = data.defaultValue

    override var currentValue: I
        get() = data.currentValue
        set(value) { data.currentValue = value }

    override fun currentIsDefault(): Boolean {
        return data.currentValue == data.defaultValue
    }

    abstract fun setFromJson(data: JSONObject)
}
