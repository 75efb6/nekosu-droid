package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath
import com.rian.difficultycalculator.beatmap.hitobject.SliderPathType
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint
import com.rian.difficultycalculator.math.Precision
import com.rian.difficultycalculator.math.Vector2
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.constants.HitObjectType

class BeatmapHitObjectsParser : BeatmapSectionParser() {
    companion object {
        private const val MAX_GAMEPLAY_CONTROL_POINTS = 24
        private const val MAX_CALCULATOR_CONTROL_POINTS = 1000
    }

    override fun parse(data: BeatmapData, line: String) {
        val pars = line.split(",").toTypedArray()

        if (pars.size < 4) {
            throw UnsupportedOperationException("Malformed hit object")
        }

        val time = data.getOffsetTime(parseDouble(pars[2]))

        val type = HitObjectType.valueOf(parseInt(pars[3]) % 16)
        val position = Vector2(
            parseFloat(pars[0], maxCoordinateValue.toFloat()),
            parseFloat(pars[1], maxCoordinateValue.toFloat())
        )

        var object_: HitObject? = null

        if (type == HitObjectType.Normal || type == HitObjectType.NormalNewCombo) {
            object_ = createCircle(time, position)
        } else if (type == HitObjectType.Slider || type == HitObjectType.SliderNewCombo) {
            object_ = createSlider(data, time, position, pars)
        } else if (type == HitObjectType.Spinner) {
            object_ = createSpinner(data, time, pars)
        }

        data.rawHitObjects.add(line)
        data.hitObjects.add(object_!!)
    }

    private fun createCircle(time: Double, position: Vector2): HitCircle {
        return HitCircle(time, position)
    }

    private fun createSlider(data: BeatmapData, time: Double, position: Vector2, pars: Array<String>): Slider {
        if (pars.size < 8) {
            throw UnsupportedOperationException("Malformed slider")
        }

        val repeat = parseInt(pars[6])
        val rawLength = Math.max(0.0, parseDouble(pars[7]))

        if (repeat > 9000) {
            throw UnsupportedOperationException("Repeat count is way too high")
        }

        val curvePoints: ArrayList<Vector2> = if (data.isCalculator) {
            parseCurvePointsCalculator(pars[5], position)
        } else {
            parseCurvePointsGameplay(pars[5], position)
        }

        var sliderType = SliderPathType.parse(pars[5][0])

        if (curvePoints.size >= 2 && curvePoints[0] == curvePoints[1]) {
            curvePoints.removeAt(0)
        }

        if (sliderType == SliderPathType.PerfectCurve) {
            if (curvePoints.size != 3) {
                sliderType = SliderPathType.Bezier
            } else if (
                Precision.almostEqualsNumber(
                    0.0,
                    ((curvePoints[1].y - curvePoints[0].y) * (curvePoints[2].x - curvePoints[0].x) -
                            (curvePoints[1].x - curvePoints[0].x) * (curvePoints[2].y - curvePoints[0].y)).toDouble()
                )
            ) {
                sliderType = SliderPathType.Linear
            }
        }

        val path = SliderPath(sliderType, curvePoints, rawLength)
        val timingControlPoint: TimingControlPoint = data.timingPoints.timing.controlPointAt(time)
        val difficultyControlPoint: DifficultyControlPoint = data.timingPoints.difficulty.controlPointAt(time)

        return Slider(
            time,
            position,
            timingControlPoint,
            difficultyControlPoint,
            repeat,
            path,
            data.difficulty.sliderMultiplier.toDouble(),
            data.difficulty.sliderTickRate.toDouble(),
            if (data.getFormatVersion() < 8) 1 / difficultyControlPoint.speedMultiplier else 1.0,
            difficultyControlPoint.generateTicks
        )
    }

    private fun parseCurvePointsGameplay(sliderData: String, position: Vector2): ArrayList<Vector2> {
        val curvePoints = ArrayList<Vector2>()
        curvePoints.add(Vector2(0f))

        var start = 2
        var pointCount = 0
        while (start < sliderData.length && pointCount < MAX_GAMEPLAY_CONTROL_POINTS) {
            var pipe = sliderData.indexOf('|', start)
            if (pipe == -1) pipe = sliderData.length

            val colon = sliderData.indexOf(':', start)
            if (colon > 0 && colon < pipe) {
                val curvePointPosition = Vector2(
                    parseFloat(sliderData.substring(start, colon)),
                    parseFloat(sliderData.substring(colon + 1, pipe))
                )
                curvePoints.add(curvePointPosition.subtract(position))
                pointCount++
            }

            start = pipe + 1
        }

        return curvePoints
    }

    private fun parseCurvePointsCalculator(sliderData: String, position: Vector2): ArrayList<Vector2> {
        val curvePointsData = sliderData.split("|".toRegex()).toTypedArray()
        val curvePoints = ArrayList<Vector2>()
        curvePoints.add(Vector2(0f))

        val limit = Math.min(curvePointsData.size, MAX_CALCULATOR_CONTROL_POINTS + 1)
        for (i in 1 until limit) {
            val curvePointData = curvePointsData[i].split(":").toTypedArray()
            val curvePointPosition = Vector2(
                parseFloat(curvePointData[0], maxCoordinateValue.toFloat()),
                parseFloat(curvePointData[1], maxCoordinateValue.toFloat())
            )
            curvePoints.add(curvePointPosition.subtract(position))
        }

        return curvePoints
    }

    private fun createSpinner(data: BeatmapData, time: Double, pars: Array<String>): Spinner {
        val endTime = data.getOffsetTime(parseInt(pars[5]).toDouble())
        return Spinner(time, endTime)
    }
}
