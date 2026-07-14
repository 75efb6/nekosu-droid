package com.rian.difficultycalculator.calculator

import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class DifficultyCalculationParameters {
    @JvmField
    var mods: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)

    @JvmField
    var customSpeedMultiplier: Float = 1f

    @JvmField
    var customCS: Float = Float.NaN

    @JvmField
    var customAR: Float = Float.NaN

    @JvmField
    var customOD: Float = Float.NaN

    fun getTotalSpeedMultiplier(): Float {
        var speedMultiplier = customSpeedMultiplier

        if (mods.contains(GameMod.MOD_DOUBLETIME) || mods.contains(GameMod.MOD_NIGHTCORE)) {
            speedMultiplier *= 1.5f
        }

        if (mods.contains(GameMod.MOD_HALFTIME)) {
            speedMultiplier *= 0.75f
        }

        return speedMultiplier
    }

    fun isCustomCS(): Boolean {
        return !customCS.isNaN()
    }

    fun isCustomAR(): Boolean {
        return !customAR.isNaN()
    }

    fun isCustomOD(): Boolean {
        return !customOD.isNaN()
    }

    fun copy(): DifficultyCalculationParameters {
        val copy = DifficultyCalculationParameters()

        copy.mods = EnumSet.copyOf(mods)
        copy.customCS = customCS
        copy.customAR = customAR
        copy.customOD = customOD
        copy.customSpeedMultiplier = customSpeedMultiplier

        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is DifficultyCalculationParameters) {
            return false
        }

        if (customSpeedMultiplier != other.customSpeedMultiplier) {
            return false
        }

        if (
            isCustomCS() != other.isCustomCS() ||
            isCustomAR() != other.isCustomAR() ||
            isCustomOD() != other.isCustomOD()
        ) {
            return false
        }

        if (isCustomCS() && other.isCustomCS() && customCS != other.customCS) {
            return false
        }

        if (isCustomAR() && other.isCustomAR() && customAR != other.customAR) {
            return false
        }

        if (isCustomOD() && other.isCustomOD() && customOD != other.customOD) {
            return false
        }

        return mods.size == other.mods.size && mods.containsAll(other.mods)
    }
}
