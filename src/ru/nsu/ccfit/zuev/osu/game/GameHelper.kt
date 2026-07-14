package ru.nsu.ccfit.zuev.osu.game

import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyData
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinManager

object GameHelper {
    var isKiai = false
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

    var controlPoints = com.edlplan.osu.support.timing.controlpoint.ControlPoints()

    var difficulty: DifficultyData? = null

    var difficultyHelper: DifficultyHelper = DifficultyHelper.StdDifficulty

    fun reset() {
        isKiai = false
        tickRate = 1f
        difficulty = null
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

    fun getDifficulty() = difficulty

    fun Round(value: Float, places: Int): Double {
        val factor = Math.pow(10.0, places.toDouble())
        return Math.round(value * factor) / factor
    }

    fun Round(value: Double, places: Int): Double {
        val factor = Math.pow(10.0, places.toDouble())
        return Math.round(value * factor) / factor
    }

    fun ar2ms(ar: Double): Double {
        val ms = if (ar < 5) 1800.0 - 120.0 * ar else 1950.0 - 150.0 * ar
        return ms.coerceIn(450.0, 1800.0)
    }

    fun ms2ar(ms: Double): Float {
        val ar = if (ms > 1200) (1800.0 - ms) / 120.0 else (1950.0 - ms) / 150.0
        return ar.coerceIn(0.0, 10.0).toFloat()
    }

    fun od2ms(od: Float): Float {
        return (80f - 6f * od).coerceIn(20f, 80f)
    }

    fun ms2od(ms: Float): Float {
        val od = (80f - ms) / 6f
        return od.coerceIn(0f, 10f)
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
    fun getStackLeniency(): Float = stackLeniency

    fun setSpeed(value: Number) { speed = value.toFloat() }
    fun setTickRate(value: Float) { tickRate = value }
    fun setScale(value: Float) { scale = value }
    fun setDifficulty(value: Float) { approachRate = value }
    fun setDrain(value: Float) { drain = value }
    fun getDrain(): Float = drain
    fun setApproachRate(value: Float) { approachRate = value }

    fun setSliderColor(color: RGBColor) { sliderColor = color }

    fun setTimingOffset(value: Number) { timingOffset = value.toFloat() }
    fun setBeatLength(value: Number) { beatLength = value.toFloat() }
    fun setTimeSignature(value: Int) { timeSignature = value }
    fun setKiai(value: Boolean) { isKiai = value }
    fun setInitalBeatLength(value: Float) { initalBeatLength = value }

    fun setGlobalTime(value: Double) { globalTime = value }

    fun setDifficultyHelper(helper: DifficultyHelper) { difficultyHelper = helper }

    fun updateGameid() {}

    fun clearPools() {
        GameObjectPool.instance.purge()
        SpritePool.getInstance().purge()
    }

    fun calculatePath(
        start: android.graphics.PointF,
        keywords: Array<String>,
        length: Float,
        offset: Float
    ): GameHelper.SliderPath {
        return SliderPath(start, keywords, length, offset)
    }

    class SliderPath(
        val start: android.graphics.PointF,
        val keywords: Array<String>,
        val length: Float,
        val offset: Float
    )
}
