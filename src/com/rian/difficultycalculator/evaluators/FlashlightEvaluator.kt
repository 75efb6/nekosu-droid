package com.rian.difficultycalculator.evaluators

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import kotlin.UnsupportedOperationException

object FlashlightEvaluator {
    @JvmStatic
    fun evaluateDifficultyOf(current: DifficultyHitObject, isHiddenMod: Boolean): Double {
        if (current.`object` is Spinner) {
            return 0.0
        }

        val scalingFactor = 52 / current.`object`.getRadius()
        var smallDistNerf = 1.0
        var cumulativeStrainTime = 0.0
        var result = 0.0
        var last: DifficultyHitObject = current
        var angleRepeatCount = 0.0

        for (i in 0 until Math.min(current.index, 10)) {
            val currentObject = current.previous(i)!!

            if (currentObject.`object` !is Spinner) {
                val jumpDistance = current.`object`
                    .getStackedPosition()
                    .subtract(currentObject.`object`.getStackedEndPosition())
                    .getLength()

                cumulativeStrainTime += last.strainTime

                if (i == 0) {
                    smallDistNerf = Math.min(1.0, jumpDistance / 75.0)
                }

                val stackNerf = Math.min(1.0, currentObject.lazyJumpDistance / scalingFactor / 25)

                val opacityBonusMultiplier = 0.4
                val opacityBonus = 1 + opacityBonusMultiplier * (1 - current.opacityAt(currentObject.`object`.getStartTime(), isHiddenMod))

                result += (stackNerf * opacityBonus * scalingFactor * jumpDistance) / cumulativeStrainTime

                if (!Double.isNaN(currentObject.angle) && !current.angle.isNaN()) {
                    if (Math.abs(currentObject.angle - current.angle) < 0.02) {
                        angleRepeatCount += Math.max(0.0, 1 - 0.1 * i)
                    }
                }
            }

            last = currentObject
        }

        result = Math.pow(smallDistNerf * result, 2.0)

        if (isHiddenMod) {
            val hiddenBonus = 0.2
            result *= 1 + hiddenBonus
        }

        val minAngleMultiplier = 0.2
        result *= minAngleMultiplier + (1 - minAngleMultiplier) / (angleRepeatCount + 1)

        var sliderBonus = 0.0
        if (current.`object` is Slider) {
            val pixelTravelDistance = (current.`object` as Slider).getLazyTravelDistance() / scalingFactor

            val minVelocity = 0.5
            sliderBonus = Math.pow(Math.max(0.0, pixelTravelDistance / current.travelTime - minVelocity), 0.5)

            sliderBonus *= pixelTravelDistance

            sliderBonus /= (current.`object` as Slider).getRepeatCount()
        }

        val sliderMultiplier = 1.3
        result += sliderBonus * sliderMultiplier

        return result
    }
}
