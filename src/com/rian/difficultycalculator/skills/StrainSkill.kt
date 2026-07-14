package com.rian.difficultycalculator.skills

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.math.Interpolation
import com.rian.difficultycalculator.math.MathUtils
import java.util.ArrayList
import java.util.Collections
import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

abstract class StrainSkill : Skill {
    @JvmField
    protected val strainPeaks: ArrayList<Double> = ArrayList()

    private var currentSectionPeak: Double = 0.0
    private var currentSectionEnd: Double = 0.0

    constructor(mods: EnumSet<GameMod>) : super(mods)

    override fun process(current: DifficultyHitObject) {
        val sectionLength = 400
        if (current.index == 0) {
            currentSectionEnd = Math.ceil(current.startTime / sectionLength) * sectionLength
        }

        while (current.startTime > currentSectionEnd) {
            saveCurrentPeak()
            startNewSectionFrom(currentSectionEnd, current)
            currentSectionEnd += sectionLength
        }

        currentSectionPeak = Math.max(strainValueAt(current), currentSectionPeak)
        saveToHitObject(current)
    }

    override fun difficultyValue(): Double {
        val strains = getCurrentStrainPeaks()
        strains.sortWith(compareByDescending { it })

        if (getReducedSectionCount() > 0) {
            for (i in 0 until Math.min(strains.size, getReducedSectionCount())) {
                val scale = Math.log10(
                    Interpolation.linear(
                        1.0, 10.0,
                        MathUtils.clamp(i.toFloat() / getReducedSectionCount(), 0f, 1f).toDouble()
                    )
                )

                strains[i] = strains[i] * Interpolation.linear(getReducedSectionBaseline(), 1.0, scale)
            }

            strains.sortWith(compareByDescending { it })
        }

        var difficulty = 0.0
        var weight = 1.0

        for (strain in strains) {
            difficulty += strain * weight
            weight *= getDecayWeight()
        }

        return difficulty * getDifficultyMultiplier()
    }

    fun getCurrentStrainPeaks(): ArrayList<Double> {
        val strains = ArrayList(strainPeaks)
        strains.add(currentSectionPeak)

        return strains
    }

    protected open fun getDifficultyMultiplier(): Double {
        return 1.06
    }

    protected open fun getDecayWeight(): Double {
        return 0.9
    }

    protected abstract fun strainValueAt(current: DifficultyHitObject): Double

    protected abstract fun calculateInitialStrain(time: Double, current: DifficultyHitObject): Double

    protected abstract fun saveToHitObject(current: DifficultyHitObject)

    protected open fun getReducedSectionCount(): Int {
        return 10
    }

    protected open fun getReducedSectionBaseline(): Double {
        return 0.75
    }

    private fun saveCurrentPeak() {
        strainPeaks.add(currentSectionPeak)
    }

    private fun startNewSectionFrom(time: Double, current: DifficultyHitObject) {
        currentSectionPeak = calculateInitialStrain(time, current)
    }
}
