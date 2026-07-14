package ru.nsu.ccfit.zuev.osu.datatypes

class DefaultFloat @JvmOverloads constructor(
    defaultValue: Float = 0f
) : DefaultData<Float>(defaultValue) {
    override fun instanceDefaultValue(): Float = 0f
}
