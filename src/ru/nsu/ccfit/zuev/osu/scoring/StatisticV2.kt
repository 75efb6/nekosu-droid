package ru.nsu.ccfit.zuev.osu.scoring

import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.api.ibancho.data.WinCondition
import com.reco1l.legacy.Multiplayer
import org.jetbrains.annotations.Nullable
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import java.io.Serializable
import java.util.EnumSet
import java.util.Locale
import java.util.Random

class StatisticV2 : Serializable {
    @JvmField
    var hit300 = 0
    @JvmField
    var hit100 = 0
    @JvmField
    var hit50 = 0
    @JvmField
    var hit300k = 0
    @JvmField
    var hit100k = 0
    @JvmField
    var misses = 0
    @JvmField
    var maxCombo = 0
    @JvmField
    var accuracy = -1f
    @JvmField
    var time: Long = 0
    @JvmField
    var isAlive = true
    @JvmField
    var canFail = true

    private var notes = 0
    @JvmField
    var isPerfect = false
    private var currentCombo = 0
    private var scoreHash = 0
    private var totalScore = 0
    private var possibleScore = 0
    private var realScore = 0
    internal var hp = 1f
    internal var diffModifier = 1f
    internal var mod: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)
    @JvmField
    var playerName: String? = ""
    private var fileName = ""
    @JvmField
    var replayName = ""
    private var forcedScore = -1
    private var mark: String? = null
    @JvmField
    var changeSpeed = 1.0f
    private val MAX_SCORE = 1000000
    private val ACC_PORTION = 0.3f
    private val COMBO_PORTION = 0.7f
    @JvmField
    var maxObjectsCount = 0
    @JvmField
    var maxHighestCombo = 0
    @JvmField
    var bonusScore = 0
    @JvmField
    var flFollowDelay = FlashLightEntity.defaultMoveDelayS
    private var positiveTotalOffsetSum = 0
    private var positiveHitOffsetSum = 0.0
    private var negativeTotalOffsetSum = 0
    private var negativeHitOffsetSum = 0.0
    @JvmField
    var unstableRate = 0.0

    internal var beatmapCS: Float? = null
    internal var beatmapOD: Float? = null
    @JvmField
    var customAR: Float? = null
    @JvmField
    var customOD: Float? = null
    @JvmField
    var customCS: Float? = null
    @JvmField
    var customHP: Float? = null

    private var legacySC = false
    private var modScoreMultiplier = 1f
    private var life = 1

    constructor() {
        playerName = null
        if (Config.isStayOnline) {
            playerName = OnlineManager.getInstance().username
            if (playerName.isNullOrEmpty())
                playerName = Config.getOnlineUsername()
        }

        if (playerName.isNullOrEmpty())
            playerName = Config.getLocalUsername()
    }

    constructor(stat: Statistic) {
        notes = stat.notes
        hit300 = stat.hit300
        hit100 = stat.hit100
        hit50 = stat.hit50
        hit300k = stat.hit300k
        hit100k = stat.hit100k
        misses = stat.misses
        maxCombo = stat.maxCombo
        currentCombo = stat.currentCombo
        totalScore = stat.totalScore
        possibleScore = stat.possibleScore
        realScore = stat.realScore
        hp = stat.hp
        diffModifier = stat.diffModifier
        mod = stat.mod.clone() as EnumSet<GameMod>
        if (stat.mod.contains(GameMod.MOD_EASY)) {
            life = 3
        }

        playerName = Config.getLocalUsername()
        computeModScoreMultiplier()
    }

    constructor(params: Array<String>) {
        playerName = ""
        if (params.size < 6) return

        setModFromString(params[0])
        setForcedScore(params[1].toInt())
        maxCombo = params[2].toInt()
        mark = params[3]
        hit300k = params[4].toInt()
        hit300 = params[5].toInt()
        hit100k = params[6].toInt()
        hit100 = params[7].toInt()
        hit50 = params[8].toInt()
        misses = params[9].toInt()
        accuracy = params[10].toInt() / 100000f
        if (params.size >= 12) {
            time = params[11].toLong()
        }
        if (params.size >= 13) {
            isPerfect = params[12].toInt() != 0
        }
        if (params.size >= 14) {
            playerName = params[13]
        }
        computeModScoreMultiplier()
    }

    fun getHp(): Float = hp

    fun changeHp(amount: Float) {
        hp += amount
        if (hp < 0) {
            hp = 0f
            life = Math.max(0, life - 1)

            if (canFail && life == 0) {
                isAlive = false
            }
        }
        if (hp > 1) {
            hp = 1f
            isAlive = true
        }
    }

    fun getTotalScore(): Int = totalScore

    val totalScoreWithMultiplier: Int
        get() {
            if (forcedScore > 0)
                return forcedScore

            return (totalScore * modScoreMultiplier).toInt()
        }

    fun registerSpinnerHit() {
        addScore(100, false)
    }

    fun registerHit(score: Int, k: Boolean, g: Boolean) {
        if (score == 1000) {
            addScore(score, false)
            return
        }
        if (score < 50 && score > 0) {
            changeHp(0.05f)
            addScore(score, false)
            currentCombo++
            return
        }
        if (score == 0 && k) {
            changeHp(-(5 + GameHelper.getDrain()) / 100f)
            if (currentCombo > maxCombo) {
                maxCombo = currentCombo
            }
            currentCombo = 0
            return
        }

        notes++
        possibleScore += 300

        when (score) {
            300 -> {
                changeHp(if (k) 0.10f else 0.05f)
                if (g) hit300k++
                hit300++
                addScore(300, true)
                realScore += 300
                currentCombo++
            }
            100 -> {
                changeHp(if (k) 0.15f else 0.05f)
                if (k) hit100k++
                hit100++
                addScore(100, true)
                realScore += 100
                currentCombo++
            }
            50 -> {
                changeHp(0.05f)
                hit50++
                addScore(50, true)
                realScore += 50
                currentCombo++
            }
            else -> {
                changeHp(-(5 + GameHelper.getDrain()) / 100f)
                misses++
                isPerfect = false
                if (currentCombo > maxCombo) {
                    maxCombo = currentCombo
                }
                currentCombo = 0
            }
        }
    }

    val accuracyForServer: Float
        get() {
            var value = (hit300 * 6f + hit100 * 2f + hit50) / ((hit300 + hit100 + hit50 + misses) * 6f)

            if (value.isNaN() || value.isInfinite())
                value = 0f

            return value
        }

    fun getAccuracy(): Float {
        if (accuracy >= 0)
            return accuracy
        if (possibleScore == 0) {
            return 0f
        }
        return realScore / possibleScore.toFloat()
    }

    fun setAccuracy(accuracy: Float) {
        this.accuracy = accuracy
    }

    private fun addScore(amount: Int, combo: Boolean) {
        if (!isScoreValid) {
            scoreHash = Random().nextInt(1313) or 3455
            return
        }
        if (GameHelper.isScoreV2()) {
            if (amount == 1000) {
                bonusScore += amount
            }
            val percentage = notes.toFloat() / maxObjectsCount
            var maxcb = getMaxCombo()
            if (currentCombo == maxcb) maxcb++
            var acc = 0f
            if (possibleScore > 0) {
                acc = when (amount) {
                    300 -> (realScore + 300) / possibleScore.toFloat()
                    100 -> (realScore + 100) / possibleScore.toFloat()
                    50 -> (realScore + 50) / possibleScore.toFloat()
                    else -> realScore / possibleScore.toFloat()
                }
            }
            totalScore = (MAX_SCORE * (ACC_PORTION * Math.pow(acc.toDouble(), 10.0) * percentage
                    + COMBO_PORTION * maxcb / maxHighestCombo) + bonusScore).toInt()
        } else if (amount + amount * currentCombo * diffModifier / 25 > 0) {
            if (amount * currentCombo * diffModifier / 25 + amount < 0 || totalScore == Int.MAX_VALUE) {
                totalScore = Int.MAX_VALUE
            } else {
                totalScore += amount
                if (combo) {
                    totalScore += (amount * currentCombo * diffModifier / 25).toInt()
                }
            }
        }
        scoreHash = SecurityUtils.getHigh16Bits(totalScore)
    }

    fun getMark(): String? {
        if (mark != null) return mark
        var isH = false
        for (m in mod) {
            when (m) {
                GameMod.MOD_HIDDEN -> {
                    isH = true
                    break
                }
                GameMod.MOD_FLASHLIGHT -> {
                    isH = true
                    break
                }
                else -> {}
            }
        }

        if (hit100 == 0 && hit50 == 0 && misses == 0) {
            return if (isH) "XH" else "X"
        }
        if (hit300.toFloat() / notes > 0.9f && misses == 0
            && hit50.toFloat() / notes < 0.01f
        ) {
            return if (isH) "SH" else "S"
        }
        if (hit300.toFloat() / notes > 0.8f && misses == 0
            || hit300.toFloat() / notes > 0.9f
        ) {
            return "A"
        }
        if (hit300.toFloat() / notes > 0.7f && misses == 0
            || hit300.toFloat() / notes > 0.8f
        ) {
            return "B"
        }
        if (hit300.toFloat() / notes > 0.6f) {
            return "C"
        }
        return "D"
    }

    fun setMark(mark: String?) {
        this.mark = mark
    }

    fun setTotalScore(totalScore: Int) {
        this.totalScore = totalScore
    }

    fun getMaxCombo(): Int {
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo
        }
        return maxCombo
    }

    fun setMaxCombo(maxCombo: Int) {
        this.maxCombo = maxCombo
    }

    fun getNotes(): Int = notes

    fun setNotes(notes: Int) {
        this.notes = notes
    }

    fun getHit300(): Int = hit300

    fun setHit300(hit300: Int) {
        this.hit300 = hit300
    }

    fun getHit100(): Int = hit100

    fun setHit100(hit100: Int) {
        this.hit100 = hit100
    }

    fun getHit50(): Int = hit50

    fun setHit50(hit50: Int) {
        this.hit50 = hit50
    }

    fun getHit300k(): Int = hit300k

    fun setHit300k(hit300k: Int) {
        this.hit300k = hit300k
    }

    fun getHit100k(): Int = hit100k

    fun setHit100k(hit100k: Int) {
        this.hit100k = hit100k
    }

    fun getMisses(): Int = misses

    fun setMisses(misses: Int) {
        this.misses = misses
    }

    fun getPerfect(): Boolean = isPerfect

    fun setPerfect(perfect: Boolean) {
        isPerfect = perfect
    }

    fun getCombo(): Int = currentCombo

    fun setCombo(combo: Int) {
        currentCombo = combo
    }

    fun getTime(): Long = time

    fun setTime(time: Long) {
        this.time = time
    }

    fun getMod(): EnumSet<GameMod> = mod

    fun setMod(mod: EnumSet<GameMod>) {
        this.mod = mod.clone() as EnumSet<GameMod>
        computeModScoreMultiplier()
    }

    fun getDiffModifier(): Float = diffModifier

    fun setDiffModifier(diffModifier: Float) {
        this.diffModifier = diffModifier
    }

    fun getPlayerName(): String? = playerName

    fun setPlayerName(playerName: String?) {
        this.playerName = playerName
    }

    fun getModString(): String {
        val sb = StringBuilder()

        if (mod.contains(GameMod.MOD_AUTO)) sb.append("a")
        if (mod.contains(GameMod.MOD_RELAX)) sb.append("x")
        if (mod.contains(GameMod.MOD_AUTOPILOT)) sb.append("p")
        if (mod.contains(GameMod.MOD_EASY)) sb.append("e")
        if (mod.contains(GameMod.MOD_NOFAIL)) sb.append("n")
        if (mod.contains(GameMod.MOD_HARDROCK)) sb.append("r")
        if (mod.contains(GameMod.MOD_HIDDEN)) sb.append("h")
        if (mod.contains(GameMod.MOD_FLASHLIGHT)) sb.append("i")
        if (mod.contains(GameMod.MOD_DOUBLETIME)) sb.append("d")
        if (mod.contains(GameMod.MOD_NIGHTCORE)) sb.append("c")
        if (mod.contains(GameMod.MOD_HALFTIME)) sb.append("t")
        if (mod.contains(GameMod.MOD_PRECISE)) sb.append("s")
        if (mod.contains(GameMod.MOD_REALLYEASY)) sb.append("l")
        if (mod.contains(GameMod.MOD_PERFECT)) sb.append("f")
        if (mod.contains(GameMod.MOD_SUDDENDEATH)) sb.append("u")
        if (mod.contains(GameMod.MOD_SCOREV2)) sb.append("v")
        sb.append("|")
        sb.append(extraModString)
        return sb.toString()
    }

    fun setModFromString(s: String) {
        val strMod = s.split("\\|".toRegex(), limit = 2).toTypedArray()
        mod = EnumSet.noneOf(GameMod::class.java)
        for (i in strMod[0].indices) {
            when (strMod[0][i]) {
                'a' -> mod.add(GameMod.MOD_AUTO)
                'x' -> mod.add(GameMod.MOD_RELAX)
                'p' -> mod.add(GameMod.MOD_AUTOPILOT)
                'e' -> {
                    mod.add(GameMod.MOD_EASY)
                    life = 3
                }
                'n' -> mod.add(GameMod.MOD_NOFAIL)
                'r' -> mod.add(GameMod.MOD_HARDROCK)
                'h' -> mod.add(GameMod.MOD_HIDDEN)
                'i' -> mod.add(GameMod.MOD_FLASHLIGHT)
                'd' -> mod.add(GameMod.MOD_DOUBLETIME)
                'c' -> mod.add(GameMod.MOD_NIGHTCORE)
                't' -> mod.add(GameMod.MOD_HALFTIME)
                's' -> mod.add(GameMod.MOD_PRECISE)
                'm' -> legacySC = true
                'l' -> mod.add(GameMod.MOD_REALLYEASY)
                'u' -> mod.add(GameMod.MOD_SUDDENDEATH)
                'f' -> mod.add(GameMod.MOD_PERFECT)
                'v' -> mod.add(GameMod.MOD_SCOREV2)
            }
        }
        if (strMod.size > 1)
            setExtraModFromString(strMod[1])

        computeModScoreMultiplier()
    }

    fun setReplayName(replayName: String) {
        this.replayName = replayName
    }

    fun setForcedScore(forcedScore: Int) {
        this.forcedScore = forcedScore
        totalScore = forcedScore
    }

    fun getFileName(): String = fileName

    fun setFileName(fileName: String) {
        this.fileName = fileName
    }

    val isScoreValid: Boolean
        get() = SecurityUtils.getHigh16Bits(totalScore) == scoreHash

    fun compile(): String {
        val builder = StringBuilder()
        var mstring = getModString()
        if (mstring.isEmpty()) mstring = "-"
        builder.append(mstring)
        builder.append(' ')
        builder.append(totalScoreWithMultiplier)
        builder.append(' ')
        builder.append(maxCombo)
        builder.append(' ')
        builder.append(mark)
        builder.append(' ')
        builder.append(hit300k)
        builder.append(' ')
        builder.append(hit300)
        builder.append(' ')
        builder.append(hit100k)
        builder.append(' ')
        builder.append(hit100)
        builder.append(' ')
        builder.append(hit50)
        builder.append(' ')
        builder.append(misses)
        builder.append(' ')
        builder.append((accuracy * 100000f).toInt())
        builder.append(' ')
        builder.append(time)
        builder.append(' ')
        builder.append(if (isPerfect) 1 else 0)
        builder.append(' ')
        builder.append(playerName)
        return builder.toString()
    }

    fun setMaxObjectsCount(count: Int) {
        maxObjectsCount = count
    }

    fun setMaxHighestCombo(count: Int) {
        maxHighestCombo = count
    }

    fun getChangeSpeed(): Float = changeSpeed

    fun setChangeSpeed(speed: Float) {
        changeSpeed = speed
        computeModScoreMultiplier()
    }

    fun isCustomAR(): Boolean = customAR != null

    fun getCustomAR(): Float? = customAR

    fun setCustomAR(@Nullable ar: Float?) {
        customAR = ar
    }

    fun isCustomOD(): Boolean = customOD != null

    fun getCustomOD(): Float? = customOD

    fun setCustomOD(@Nullable customOD: Float?) {
        this.customOD = customOD
    }

    fun isCustomHP(): Boolean = customHP != null

    fun getCustomHP(): Float? = customHP

    fun setCustomHP(@Nullable customHP: Float?) {
        this.customHP = customHP
    }

    fun isCustomCS(): Boolean = customCS != null

    fun getCustomCS(): Float? = customCS

    fun setCustomCS(@Nullable customCS: Float?) {
        this.customCS = customCS
    }

    fun setBeatmapCS(beatmapCS: Float) {
        this.beatmapCS = beatmapCS
    }

    fun setBeatmapOD(beatmapOD: Float) {
        this.beatmapOD = beatmapOD
    }

    fun setFLFollowDelay(delay: Float) {
        flFollowDelay = delay
    }

    fun getFLFollowDelay(): Float = flFollowDelay

    fun getUnstableRate(): Double = unstableRate

    fun addHitOffset(accuracy: Double) {
        val msAccuracy = accuracy * 1000

        if (accuracy >= 0) {
            positiveHitOffsetSum += msAccuracy
            positiveTotalOffsetSum++
        } else {
            negativeHitOffsetSum += msAccuracy
            negativeTotalOffsetSum++
        }

        val totalOffsetSum = positiveTotalOffsetSum + negativeTotalOffsetSum
        val hitOffsetSum = positiveHitOffsetSum + negativeHitOffsetSum

        if (totalOffsetSum > 1) {
            val avgOffset = hitOffsetSum / totalOffsetSum
            val oldMean = (hitOffsetSum - msAccuracy) / (totalOffsetSum - 1)

            unstableRate = 10 * Math.sqrt(
                ((totalOffsetSum - 1) * Math.pow(unstableRate / 10, 2.0) +
                        (msAccuracy - oldMean) * (msAccuracy - avgOffset)) / totalOffsetSum
            )
        }
    }

    val negativeHitError: Double
        get() = if (negativeTotalOffsetSum == 0) 0.0 else negativeHitOffsetSum / negativeTotalOffsetSum

    val positiveHitError: Double
        get() = if (positiveTotalOffsetSum == 0) 0.0 else positiveHitOffsetSum / positiveTotalOffsetSum

    fun getSpeed(): Float {
        var speed = changeSpeed
        if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
            speed *= 1.5f
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            speed *= 0.75f
        }
        return speed
    }

    val extraModString: String
        get() {
            val builder = StringBuilder()
            if (changeSpeed != 1f) {
                builder.append(String.format(Locale.ENGLISH, "x%.2f|", changeSpeed))
            }
            if (isCustomAR()) {
                builder.append(String.format(Locale.ENGLISH, "AR%.1f|", customAR))
            }
            if (isCustomOD()) {
                builder.append(String.format(Locale.ENGLISH, "OD%.1f|", customOD))
            }
            if (isCustomCS()) {
                builder.append(String.format(Locale.ENGLISH, "CS%.1f|", customCS))
            }
            if (isCustomHP()) {
                builder.append(String.format(Locale.ENGLISH, "HP%.1f|", customHP))
            }
            if (flFollowDelay != FlashLightEntity.defaultMoveDelayS) {
                builder.append(String.format(Locale.ENGLISH, "FLD%.2f|", flFollowDelay))
            }
            if (builder.isNotEmpty()) {
                builder.deleteCharAt(builder.length - 1)
            }

            return builder.toString()
        }

    fun setExtraModFromString(s: String) {
        for (str in s.split("\\|".toRegex())) {
            if (str.startsWith("x") && str.length == 5) {
                changeSpeed = str.substring(1).toFloat()
                continue
            }
            if (str.startsWith("AR")) customAR = str.substring(2).toFloat()
            if (str.startsWith("OD")) customOD = str.substring(2).toFloat()
            if (str.startsWith("CS")) customCS = str.substring(2).toFloat()
            if (str.startsWith("HP")) customHP = str.substring(2).toFloat()
            if (str.startsWith("FLD")) flFollowDelay = str.substring(3).toFloat()
        }

        computeModScoreMultiplier()
    }

    fun toJson(): JSONObject {
        return try {
            object : JSONObject() {
                init {
                    put("accuracy", accuracyForServer)
                    put("score", totalScoreWithMultiplier)
                    put("username", playerName)
                    put("modstring", getModString())
                    put("maxCombo", maxCombo)
                    put("geki", hit300k)
                    put("perfect", hit300)
                    put("katu", hit100k)
                    put("good", hit100)
                    put("bad", hit50)
                    put("miss", misses)
                    put("isAlive", isAlive)
                }
            }
        } catch (e: Exception) {
            Multiplayer.log(e)
            JSONObject()
        }
    }

    fun toBoardItem(): ScoreBoardItem {
        val combo = if (!Multiplayer.isConnected || Multiplayer.room?.winCondition !== WinCondition.MAX_COMBO) currentCombo else maxCombo

        return ScoreBoardItem(playerName, totalScoreWithMultiplier, combo, accuracyForServer, isAlive)
    }

    private fun computeModScoreMultiplier() {
        modScoreMultiplier = 1f

        for (m in mod) {
            modScoreMultiplier *= m.scoreMultiplier
        }

        if (isCustomCS() && beatmapCS != null) {
            modScoreMultiplier *= getCustomCSScoreMultiplier(beatmapCS!!, customCS!!)
        }

        if (isCustomOD() && beatmapOD != null) {
            modScoreMultiplier *= getCustomODScoreMultiplier(beatmapOD!!, customOD!!)
        }

        if (changeSpeed != 1f) {
            modScoreMultiplier *= getSpeedChangeScoreMultiplier()
        }
    }

    private fun getSpeedChangeScoreMultiplier(): Float {
        return getSpeedChangeScoreMultiplier(getSpeed(), mod)
    }

    val isLegacySC: Boolean get() = legacySC

    fun processLegacySC(track: TrackInfo) {
        var cs = track.circleSize

        for (m in mod) {
            when (m) {
                GameMod.MOD_HARDROCK -> ++cs
                GameMod.MOD_EASY, GameMod.MOD_REALLYEASY -> --cs
                else -> {}
            }
        }

        customCS = cs + 4
    }

    companion object {
        private const val serialVersionUID = 8339570462000129479L
        private val random = Random()

        @JvmStatic
        fun getSpeedChangeScoreMultiplier(speed: Float, mod: EnumSet<GameMod>): Float {
            var multi = speed
            if (multi > 1) {
                multi = 1.0f + (multi - 1.0f) * 0.24f
            } else if (multi < 1) {
                multi = Math.pow(0.3, ((1.0 - multi) * 4).toDouble()).toFloat()
            } else if (multi == 1f) {
                return 1f
            }
            if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
                multi /= 1.12f
            }
            if (mod.contains(GameMod.MOD_HALFTIME)) {
                multi /= 0.3f
            }
            return multi
        }

        @JvmStatic
        fun getCustomCSScoreMultiplier(beatmapCS: Float, customCS: Float): Float {
            val diff = customCS - beatmapCS
            return if (diff >= 0)
                1 + 0.0075f * Math.pow(diff.toDouble(), 1.5).toFloat()
            else
                2 / (1 + Math.exp(-0.5 * diff).toFloat())
        }

        @JvmStatic
        fun getCustomODScoreMultiplier(beatmapOD: Float, customOD: Float): Float {
            val diff = customOD - beatmapOD
            return if (diff >= 0)
                1 + 0.005f * Math.pow(diff.toDouble(), 1.3).toFloat()
            else
                2 / (1 + Math.exp(-0.25 * diff).toFloat())
        }
    }
}
