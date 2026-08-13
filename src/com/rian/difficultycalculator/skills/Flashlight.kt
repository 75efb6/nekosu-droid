package com.rian.difficultycalculator.skills

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.evaluators.FlashlightEvaluator
import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class Flashlight : StrainSkill {
    private var currentStrain: Double = 0.0
    private val hasHidden: Boolean

    constructor(mods: EnumSet<GameMod>) : super(mods) {
        hasHidden = mods.contains(GameMod.MOD_HIDDEN)
    }

    override fun strainValueAt(current: DifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        val skillMultiplier = 0.052
        currentStrain += FlashlightEvaluator.evaluateDifficultyOf(current, hasHidden) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DifficultyHitObject): Double {
        return currentStrain * strainDecay(time - current.previous(0)!!.startTime)
    }

    override fun saveToHitObject(current: DifficultyHitObject) {
        current.flashlightStrain = currentStrain
    }

    override fun getReducedSectionCount(): Int {
        return 0
    }

    override fun getReducedSectionBaseline(): Double {
        return 1.0
    }

    override fun getDecayWeight(): Double {
        return 1.0
    }

    private fun strainDecay(ms: Double): Double {
        return Math.pow(0.15, ms / 1000)
    }
}
