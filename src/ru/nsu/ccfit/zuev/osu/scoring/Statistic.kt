package ru.nsu.ccfit.zuev.osu.scoring

import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.io.Serializable
import java.util.EnumSet

class Statistic : Serializable {
    @Transient
    var notes = 0
    var hit300 = 0
    var hit100 = 0
    var hit50 = 0
    var hit300k = 0
    var hit100k = 0
    var misses = 0
    var maxCombo = 0
        get() {
            if (currentCombo > field) {
                field = currentCombo
            }
            return field
        }
    var currentCombo = 0
    var totalScore = 0
    var possibleScore = 0
    var realScore = 0
    var hp = 1f
    var diffModifier = 1f
    var mod: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)
        set(value) { field = value.clone() as EnumSet<GameMod> }

    fun changeHp(amount: Float) {
        hp += amount
        if (hp < 0) {
            hp = 0f
        }
        if (hp > 1) {
            hp = 1f
        }
    }

    fun getModifiedTotalScore(): Int {
        var mult = 1f
        if (mod.contains(GameMod.MOD_AUTO)) {
            mult *= 0f
        }
        if (mod.contains(GameMod.MOD_EASY)) {
            mult *= 0.5f
        }
        if (mod.contains(GameMod.MOD_NOFAIL)) {
            mult *= 0.5f
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            mult *= 1.06f
        }
        if (mod.contains(GameMod.MOD_HIDDEN)) {
            mult *= 1.06f
        }
        if (mod.contains(GameMod.MOD_FLASHLIGHT)) {
            mult *= 1.12f
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            mult *= 1.12f
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            mult *= 1.12f
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            mult *= 0.3f
        }
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            mult *= 0.4f
        }
        return (totalScore * mult).toInt()
    }

    fun getAutoTotalScore(): Int {
        var mult = 1f
        if (mod.contains(GameMod.MOD_EASY)) {
            mult *= 0.5f
        }
        if (mod.contains(GameMod.MOD_NOFAIL)) {
            mult *= 0.5f
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            mult *= 1.06f
        }
        if (mod.contains(GameMod.MOD_HIDDEN)) {
            mult *= 1.06f
        }
        if (mod.contains(GameMod.MOD_FLASHLIGHT)) {
            mult *= 1.12f
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            mult *= 1.12f
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            mult *= 1.12f
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            mult *= 0.3f
        }
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            mult *= 0.4f
        }
        return (totalScore * mult).toInt()
    }

    fun registerSpinnerHit() {
        totalScore += 100
    }

    fun registerHit(score: Int, k: Boolean, g: Boolean) {
        if (score == 1000) {
            totalScore += score
            return
        }
        if (score < 50 && score > 0) {
            changeHp(0.05f)
            totalScore += score
            currentCombo++
            return
        }
        if (score == 0 && k) {
            changeHp(-(5 + GameHelper.drain) / 100f)
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
                if (g) {
                    hit300k++
                }
                hit300++
                addScore(300)
                realScore += 300
                currentCombo++
            }
            100 -> {
                changeHp(if (k) 0.15f else 0.05f)
                if (k) {
                    hit100k++
                }
                hit100++
                addScore(100)
                realScore += 100
                currentCombo++
            }
            50 -> {
                changeHp(0.05f)
                hit50++
                addScore(50)
                realScore += 50
                currentCombo++
            }
            else -> {
                changeHp(-(5 + GameHelper.drain) / 100f)
                misses++
                if (currentCombo > maxCombo) {
                    maxCombo = currentCombo
                }
                currentCombo = 0
            }
        }
    }

    fun getAccuracy(): Float {
        if (possibleScore == 0) {
            return 0f
        }
        return realScore / possibleScore.toFloat()
    }

    fun addScore(amount: Int) {
        totalScore += amount + (amount * currentCombo * diffModifier / 25).toInt()
    }

    fun getMark(): String {
        var isH = false
        for (m in mod) {
            when (m) {
                GameMod.MOD_HIDDEN -> {
                    isH = true
                    break
                }
                else -> {}
            }
        }

        if (hit100 == 0 && hit100k == 0 && hit50 == 0 && misses == 0) {
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

    fun getCombo(): Int = currentCombo

    companion object {
        private const val serialVersionUID = 8339570462000129479L
    }
}
