package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData

abstract class BeatmapSectionParser {
    companion object {
        const val maxCoordinateValue = 131072
        private const val maxParseLimit = Int.MAX_VALUE
    }

    abstract fun parse(data: BeatmapData, line: String)

    protected fun parseInt(str: String): Int = parseInt(str, maxParseLimit)

    protected fun parseInt(str: String, parseLimit: Int): Int {
        val output = str.toInt()

        if (output < -parseLimit) {
            throw NumberFormatException("Value is too low")
        }

        if (output > parseLimit) {
            throw NumberFormatException("Value is too high")
        }

        return output
    }

    protected fun parseFloat(str: String): Float = parseFloat(str, maxParseLimit.toFloat(), false)

    protected fun parseFloat(str: String, parseLimit: Float): Float = parseFloat(str, parseLimit, false)

    protected fun parseFloat(str: String, allowNaN: Boolean): Float = parseFloat(str, maxParseLimit.toFloat(), allowNaN)

    protected fun parseFloat(str: String, parseLimit: Float, allowNaN: Boolean): Float {
        val output = str.toFloat()

        if (output < -parseLimit) {
            throw NumberFormatException("Value is too low")
        }

        if (output > parseLimit) {
            throw NumberFormatException("Value is too high")
        }

        if (!allowNaN && output.isNaN()) {
            throw NumberFormatException("Not a number")
        }

        return output
    }

    protected fun parseDouble(str: String): Double = parseDouble(str, maxParseLimit.toDouble(), false)

    protected fun parseDouble(str: String, parseLimit: Double): Double = parseDouble(str, parseLimit, false)

    protected fun parseDouble(str: String, allowNaN: Boolean): Double = parseDouble(str, maxParseLimit.toDouble(), allowNaN)

    protected fun parseDouble(str: String, parseLimit: Double, allowNaN: Boolean): Double {
        val output = str.toDouble()

        if (output < -parseLimit) {
            throw NumberFormatException("Value is too low")
        }

        if (output > parseLimit) {
            throw NumberFormatException("Value is too high")
        }

        if (!allowNaN && output.isNaN()) {
            throw NumberFormatException("Not a number")
        }

        return output
    }
}
