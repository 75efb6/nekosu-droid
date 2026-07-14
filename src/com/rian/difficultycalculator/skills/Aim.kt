package com.rian.difficultycalculator.skills

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.evaluators.AimEvaluator
import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class Aim : StrainSkill {
    private val withSliders: Boolean
    private var currentStrain: Double = 0.0

    constructor(mods: EnumSet<GameMod>, withSliders: Boolean) : super(mods) {
        this.withSliders = withSliders
    }

    override fun strainValueAt(current: DifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        val skillMultiplier = 23.55
        currentStrain += AimEvaluator.evaluateDifficultyOf(current, withSliders) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DifficultyHitObject): Double {
        return currentStrain * strainDecay(time - current.previous(0)!!.startTime)
    }

    override fun saveToHitObject(current: DifficultyHitObject) {
        if (withSliders) {
            current.aimStrainWithSliders = currentStrain
        } else {
            current.aimStrainWithoutSliders = currentStrain
        }
    }

    private fun strainDecay(ms: Double): Double {
        return Math.pow(0.15, ms / 1000)
    }
}
