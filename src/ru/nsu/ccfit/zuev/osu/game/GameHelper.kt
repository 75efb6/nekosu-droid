package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper
import ru.nsu.ccfit.zuev.osu.polygon.Spline
import java.util.ArrayList
import java.util.LinkedList
import java.util.Queue

object GameHelper {
    var isKiai = false
        get() = !ru.nsu.ccfit.zuev.skins.OsuSkin.get().isDisableKiai() && field
    var tickRate = 1f
    var objectTimePre = 0f
    var objectTimeFadeIn = 0f
    var approachRate = 0f

    var speed = 0f
    var beatLength = 0f
    var initalBeatLength = 0f
    var scale = 0f
    var drain = 0f
    var timingOffset = 0f
    var timeSignature = 4
    var globalTime = 0.0
    var stackLeniency = 0f
    var sliderColor: RGBColor = RGBColor(200f, 200f, 200f)
    var kiaiTickLength = 0.0
        get() = initalBeatLength.toDouble()
        private set

    private var hardrock = false
    private var doubleTime = false
    private var nightCore = false
    private var halfTime = false
    private var hidden = false
    private var flashLight = false
    private var relaxMod = false
    private var autopilotMod = false
    private var suddenDeath = false
    private var perfect = false
    private var scoreV2 = false
    private var easy = false
    private var auto = false
    private var timeMultiplier = 1f

    private var curveType: Spline.CurveTypes = Spline.CurveTypes.Linear

    var controlPoints = com.edlplan.osu.support.timing.controlpoint.ControlPoints()

    var overallDifficulty = 0f

    var difficultyHelper: DifficultyHelper = DifficultyHelper.StdDifficulty

    private val pathPool: Queue<SliderPath> = LinkedList()
    private val pointPool: Queue<PointF> = LinkedList()
    private const val MAX_CONTROL_POINTS = 24

    fun reset() {
        isKiai = false
        tickRate = 1f
        overallDifficulty = 0f
        objectTimePre = 0f
        objectTimeFadeIn = 0f
        approachRate = 0f
        speed = 0f
        beatLength = 0f
        initalBeatLength = 0f
        scale = 0f
        drain = 0f
        timingOffset = 0f
        timeSignature = 4
        globalTime = 0.0
        stackLeniency = 0f
        hardrock = false
        doubleTime = false
        nightCore = false
        halfTime = false
        hidden = false
        flashLight = false
        relaxMod = false
        autopilotMod = false
        suddenDeath = false
        perfect = false
        scoreV2 = false
        easy = false
        auto = false
        timeMultiplier = 1f
    }

    fun Round(value: Float, places: Int): Double {
        val factor = Math.pow(10.0, places.toDouble())
        return Math.round(value * factor) / factor
    }

    fun Round(value: Double, places: Int): Double {
        val factor = Math.pow(10.0, places.toDouble())
        return Math.round(value * factor) / factor
    }

    fun ar2ms(ar: Double): Double {
        return Round(if (ar <= 5) 1800.0 - 120.0 * ar else 1950.0 - 150.0 * ar, 0)
    }

    fun ms2ar(ms: Double): Float {
        return (if (ms <= 1200) (1200 - ms) / 150.0 + 5 else (1800.0 - ms) / 120.0).toFloat()
    }

    fun od2ms(od: Float): Float {
        return Round((80f - 6f * od).toDouble(), 1).toFloat()
    }

    fun ms2od(ms: Float): Float {
        return (80f - ms) / 6f
    }

    fun setHardrock(value: Boolean) { hardrock = value }
    fun isHardrock(): Boolean = hardrock

    fun setDoubleTime(value: Boolean) { doubleTime = value }
    fun isDoubleTime(): Boolean = doubleTime

    fun setNightCore(value: Boolean) { nightCore = value }
    fun isNightCore(): Boolean = nightCore

    fun setHalfTime(value: Boolean) { halfTime = value }
    fun isHalfTime(): Boolean = halfTime

    fun setHidden(value: Boolean) { hidden = value }
    fun isHidden(): Boolean = hidden

    fun setFlashLight(value: Boolean) { flashLight = value }
    fun isFlashLight(): Boolean = flashLight

    fun setRelaxMod(value: Boolean) { relaxMod = value }
    fun isRelaxMod(): Boolean = relaxMod

    fun setAutopilotMod(value: Boolean) { autopilotMod = value }
    fun isAutopilotMod(): Boolean = autopilotMod

    fun setSuddenDeath(value: Boolean) { suddenDeath = value }
    fun isSuddenDeath(): Boolean = suddenDeath

    fun setPerfect(value: Boolean) { perfect = value }
    fun isPerfect(): Boolean = perfect

    fun setScoreV2(value: Boolean) { scoreV2 = value }
    fun isScoreV2(): Boolean = scoreV2

    fun setEasy(value: Boolean) { easy = value }
    fun isEasy(): Boolean = easy

    fun setAuto(value: Boolean) { auto = value }
    fun isAuto(): Boolean = auto

    fun setTimeMultiplier(value: Float) { timeMultiplier = value }
    fun getTimeMultiplier(): Float = timeMultiplier

    fun setStackLeniency(value: Number) { stackLeniency = value.toFloat() }

    fun setSpeed(value: Number) { speed = value.toFloat() }
    fun setDifficulty(value: Float) { overallDifficulty = value }
    fun setBeatLength(value: Number) { beatLength = value.toFloat() }
    fun setTimingOffset(value: Number) { timingOffset = value.toFloat() }

    fun getSliderTickLength(): Double {
        return 100.0 * initalBeatLength / speed
    }

    fun updateGameid() {}

    fun clearPools() {
        pathPool.clear()
        pointPool.clear()
        GameObjectPool.instance.purge()
        SpritePool.getInstance().purge()
    }

    fun calculatePath(
        pos: PointF,
        data: Array<String>,
        maxLength: Float,
        offset: Float
    ): SliderPath {
        val points = ArrayList<ArrayList<PointF>>()
        points.add(ArrayList())
        var lastIndex = 0
        points[lastIndex].add(pos)

        val path = newPath()

        for (s in data) {
            if (s == data[0]) {
                curveType = Spline.getCurveType(s[0])

                if (curveType == Spline.CurveTypes.PerfectCurve && data.size != 3) {
                    curveType = Spline.CurveTypes.Bezier
                }

                continue
            }
            val nums = s.split(":".toRegex()).toTypedArray()
            val point = newPointF()
            point.set(nums[0].toFloat().toInt().toFloat(), nums[1].toFloat().toInt().toFloat())
            point.x += offset
            point.y += offset
            val ppoint = points[lastIndex][points[lastIndex].size - 1]
            if (point.x == ppoint.x && point.y == ppoint.y || data[0] == "C") {
                if (data[0] == "C") {
                    points[lastIndex].add(point)
                }
                points.add(ArrayList())
                lastIndex++
            }
            points[lastIndex].add(point)
        }

        for (plist in points) {
            if (plist.size > MAX_CONTROL_POINTS) {
                val step = Math.max(1, (plist.size - 1) / (MAX_CONTROL_POINTS - 1))
                val downsampled = ArrayList<PointF>(MAX_CONTROL_POINTS + 1)
                var i = 0
                while (i < plist.size) {
                    downsampled.add(plist[i])
                    i += step
                }
                val lastPt = plist[plist.size - 1]
                if (downsampled[downsampled.size - 1] !== lastPt) {
                    downsampled.add(lastPt)
                }
                plist.clear()
                plist.addAll(downsampled)
            }
        }

        var section: ArrayList<PointF>
        var pind = -1
        var trackLength = 0f
        val vec = newPointF()

        run breaking@{
            for (plist in points) {
                val spline = Spline.getInstance()
                spline.setControlPoints(plist)
                spline.setType(curveType)
                spline.refresh()
                section = spline.getPoints()

                if (curveType == Spline.CurveTypes.PerfectCurve && section.isEmpty()) {
                    spline.setType(Spline.CurveTypes.Bezier)
                    spline.refresh()
                    section = spline.getPoints()
                }

                for (p in section) {
                    if (pind < 0 || Math.abs(p.x - path.points[pind].x) +
                        Math.abs(p.y - path.points[pind].y) > 1f
                    ) {
                        if (path.points.isNotEmpty()) {
                            vec.set(
                                p.x - path.points[path.points.size - 1].x,
                                p.y - path.points[path.points.size - 1].y
                            )
                            trackLength += Utils.length(vec)
                            path.length.add(trackLength)
                        }
                        path.points.add(p)
                        pind++

                        if (trackLength >= maxLength) {
                            return@breaking
                        }
                    }
                }
            }
        }

        for (i in path.points.indices) {
            path.points[i] = Utils.trackToRealCoords(path.points[i])
        }

        if (path.points.size == 1) {
            path.points.add(PointF(path.points[0].x, path.points[0].y))
            path.length.add(0f)
        }

        return path
    }

    fun putPath(path: SliderPath) {
        pointPool.addAll(path.points)
        path.points.clear()
        path.length.clear()
        pathPool.add(path)
    }

    private fun newPath(): SliderPath {
        return if (pathPool.isEmpty()) {
            SliderPath()
        } else {
            pathPool.poll()
        }
    }

    private fun newPointF(): PointF {
        return if (pointPool.isEmpty()) {
            PointF()
        } else {
            pointPool.poll()
        }
    }

    class SliderPath {
        val points: ArrayList<PointF> = ArrayList()
        val length: ArrayList<Float> = ArrayList()
    }
}
