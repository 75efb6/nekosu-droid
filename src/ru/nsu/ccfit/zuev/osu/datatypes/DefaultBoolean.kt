package ru.nsu.ccfit.zuev.osu.datatypes

class DefaultBoolean @JvmOverloads constructor(
    defaultValue: Boolean = false
) : DefaultData<Boolean>(defaultValue) {
    override fun instanceDefaultValue(): Boolean = false
}
