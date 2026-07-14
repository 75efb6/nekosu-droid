package com.rian.difficultycalculator.skills

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.evaluators.RhythmEvaluator
import com.rian.difficultycalculator.evaluators.SpeedEvaluator
import java.util.ArrayList
import java.util.Collections
import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class Speed : StrainSkill {
    private var currentStrain: Double = 0.0
    private var currentRhythm: Double = 0.0
    private val objectStrains: ArrayList<Double> = ArrayList()
    private val greatWindow: Double

    constructor(mods: EnumSet<GameMod>, greatWindow: Double) : super(mods) {
        this.greatWindow = greatWindow
    }

    fun relevantNoteCount(): Double {
        if (objectStrains.isEmpty())
            return 0.0

        val maxStrain = Collections.max(objectStrains)

        if (maxStrain == 0.0)
            return 0.0

        var relevantNoteCount = 0.0

        for (strain in objectStrains) {
            relevantNoteCount += 1 / (1 + Math.exp(-(strain / maxStrain * 12 - 6)))
        }

        return relevantNoteCount
    }

    override fun strainValueAt(current: DifficultyHitObject): Double {
        currentStrain *= strainDecay(current.strainTime)
        val skillMultiplier = 1375.0
        currentStrain += SpeedEvaluator.evaluateDifficultyOf(current, greatWindow) * skillMultiplier
        currentRhythm = RhythmEvaluator.evaluateDifficultyOf(current, greatWindow)

        val totalStrain = currentStrain * currentRhythm

        objectStrains.add(totalStrain)

        return totalStrain
    }

    override fun calculateInitialStrain(time: Double, current: DifficultyHitObject): Double {
        return currentStrain * currentRhythm * strainDecay(time - current.previous(0)!!.startTime)
    }

    override fun saveToHitObject(current: DifficultyHitObject) {
        current.speedStrain = currentStrain
        current.rhythmMultiplier = currentRhythm
    }

    override fun getDifficultyMultiplier(): Double {
        return 1.04
    }

    override fun getReducedSectionCount(): Int {
        return 5
    }

    private fun strainDecay(ms: Double): Double {
        return Math.pow(0.3, ms / 1000)
    }
}
