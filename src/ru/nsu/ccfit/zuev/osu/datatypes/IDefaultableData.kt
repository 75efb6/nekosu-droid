package ru.nsu.ccfit.zuev.osu.datatypes

interface IDefaultableData<T> {
    val defaultValue: T
    var currentValue: T
    fun currentIsDefault(): Boolean
}
