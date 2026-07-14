package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import ru.nsu.ccfit.zuev.osu.Utils

class GameObjectData(line: String) {

    internal val time: Int
    @JvmField val comboCode: Int
    internal val rawdata: Array<String>
    internal val pos: PointF
    internal var posOffset: Float = 0f
    @JvmField val sampleSet: Int
    @JvmField val customSound: Int
    @JvmField val timingShift: Double

    init {
        val data = line.split(",".toRegex()).toTypedArray()

        var dataSize = data.size
        while (dataSize > 0 && data[dataSize - 1].matches("([0-9][:][0-9][|]?)+".toRegex())) {
            dataSize--
        }
        rawdata = if (dataSize < data.size) {
            Array(dataSize) { data[it] }
        } else {
            data
        }

        time = rawdata[2].toInt()
        comboCode = rawdata[3].toInt()
        pos = Utils.trackToRealCoords(PointF(rawdata[0].toFloat(), rawdata[1].toFloat()))
        posOffset = 0f

        sampleSet = if (rawdata.size > 4) parseIntSafe(rawdata[4]) else 0
        customSound = if (rawdata.size > 6) parseIntSafe(rawdata[6]) else 0
        timingShift = if (rawdata.size > 7) parseDoubleSafe(rawdata[7]) else 0.0
    }

    private fun parseIntSafe(s: String): Int {
        return try {
            s.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun parseDoubleSafe(s: String): Double {
        return try {
            s.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    fun getPos(): PointF = pos

    fun getPosOffset(): Float = posOffset

    fun setPosOffset(posOffset: Float) {
        this.posOffset = posOffset
    }

    fun getEnd(): PointF {
        if (rawdata.size >= 8) {
            val repeats = rawdata[6].toInt()
            if (repeats % 2 != 1) {
                return pos
            }
            val endP = rawdata[5].substring(rawdata[5].lastIndexOf('|') + 1).split(":".toRegex()).toTypedArray()
            return try {
                Utils.trackToRealCoords(PointF(endP[0].toFloat(), endP[1].toFloat()))
            } catch (e: NumberFormatException) {
                pos
            }
        }
        return pos
    }

    fun isNewCombo(): Boolean = comboCode and 4 > 0
    fun isSlider(): Boolean = comboCode and 2 > 0
    fun isSpinner(): Boolean = comboCode and 8 > 0
    fun getTime(): Float = time / 1000.0f
    fun getRawTime(): Int = time
    fun getData(): Array<String> = rawdata

    fun getComboNum(): Int {
        if (rawdata.size < 4) return -1
        var combo = rawdata[3].toInt()
        combo = combo and 3
        if (combo == 0) combo = (rawdata[3].toInt() shr 4) and 0xF
        return combo
    }

    fun isShowCombo(): Boolean = comboCode and 4 == 0

    fun isReverse(): Boolean {
        if (!isSlider()) return false
        if (rawdata.size < 7) return false
        return rawdata[6].toInt() > 1
    }

    fun isEndCombo(): Boolean = comboCode and 4 != 0

    fun getSliderLength(): Float {
        if (!isSlider() || rawdata.size < 6) return 0f
        return try {
            rawdata[5].split(":".toRegex()).toTypedArray()[2].toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    fun getSliderBody(sliderLength: Float): ArrayList<android.graphics.PointF> {
        val points = ArrayList<android.graphics.PointF>()
        if (!isSlider() || rawdata.size < 6) return points
        try {
            val pathData = rawdata[5]
            val pointsStr = pathData.split("|".toRegex()).drop(1).dropLastWhile { it.isEmpty() }
            for (pointStr in pointsStr) {
                val coords = pointStr.split(":".toRegex()).toTypedArray()
                if (coords.size >= 2) {
                    points.add(android.graphics.PointF(coords[0].toFloat(), coords[1].toFloat()))
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return points
    }
}
