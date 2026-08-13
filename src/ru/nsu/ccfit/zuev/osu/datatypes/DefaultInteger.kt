package ru.nsu.ccfit.zuev.osu.datatypes

class DefaultInteger @JvmOverloads constructor(
    defaultValue: Int = 0
) : DefaultData<Int>(defaultValue) {
    override fun instanceDefaultValue(): Int = 0
}
