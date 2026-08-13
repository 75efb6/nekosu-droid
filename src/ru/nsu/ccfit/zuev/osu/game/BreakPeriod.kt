package ru.nsu.ccfit.zuev.osu.game

class BreakPeriod(starttime: Float, endtime: Float) {
    internal val length: Float
    internal val start: Float

    init {
        start = starttime
        length = endtime - starttime
    }

    fun getLength(): Float = length
    fun getStart(): Float = start
}
