package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData

abstract class BeatmapSectionParser {
    companion object {
        const val maxCoordinateValue = 131072
    }

    abstract fun parse(data: BeatmapData, line: String)

    protected fun parseInt(str: String): Int = str.toInt()

    protected fun parseInt(str: String, parseLimit: Int): Int = str.toInt()

    protected fun parseFloat(str: String): Float = str.toFloat()

    protected fun parseFloat(str: String, parseLimit: Float): Float = str.toFloat()

    protected fun parseFloat(str: String, allowNaN: Boolean): Float = str.toFloat()

    protected fun parseFloat(str: String, parseLimit: Float, allowNaN: Boolean): Float = str.toFloat()

    protected fun parseDouble(str: String): Double = str.toDouble()

    protected fun parseDouble(str: String, parseLimit: Double): Double = str.toDouble()

    protected fun parseDouble(str: String, allowNaN: Boolean): Double = str.toDouble()

    protected fun parseDouble(str: String, parseLimit: Double, allowNaN: Boolean): Double = str.toDouble()
}
