package ru.nsu.ccfit.zuev.osu.datatypes

abstract class DefaultData<T> @JvmOverloads constructor(
    defaultValue: T? = instanceDefaultValue()
) : IDefaultableData<T> {
    override val defaultValue: T = defaultValue ?: instanceDefaultValue()

    override var currentValue: T = defaultValue ?: instanceDefaultValue()

    override fun currentIsDefault(): Boolean {
        return currentValue == defaultValue
    }

    protected abstract fun instanceDefaultValue(): T
}
