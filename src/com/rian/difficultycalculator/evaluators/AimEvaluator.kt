package com.rian.difficultycalculator.evaluators

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import com.rian.difficultycalculator.math.MathUtils
import kotlin.UnsupportedOperationException

object AimEvaluator {
    private const val wideAngleMultiplier = 1.5
    private const val acuteAngleMultiplier = 1.95
    private const val sliderMultiplier = 1.35
    private const val velocityChangeMultiplier = 0.75

    @JvmStatic
    fun evaluateDifficultyOf(current: DifficultyHitObject, withSliders: Boolean): Double {
        val last = current.previous(0) ?: return 0.0

        if (current.`object` is Spinner || current.index <= 1 || last.`object` is Spinner) {
            return 0.0
        }

        val lastLast = current.previous(1)

        var currentVelocity = current.lazyJumpDistance / current.strainTime

        if (last.`object` is Slider && withSliders) {
            val travelVelocity = last.travelDistance / last.travelTime
            val movementVelocity = current.minimumJumpDistance / current.minimumJumpTime
            currentVelocity = Math.max(currentVelocity, movementVelocity + travelVelocity)
        }

        var prevVelocity = last.lazyJumpDistance / last.strainTime

        if (lastLast != null && lastLast.`object` is Slider && withSliders) {
            val travelVelocity = lastLast.travelDistance / lastLast.travelTime
            val movementVelocity = last.minimumJumpDistance / last.minimumJumpTime
            prevVelocity = Math.max(prevVelocity, movementVelocity + travelVelocity)
        }

        var wideAngleBonus = 0.0
        var acuteAngleBonus = 0.0
        var sliderBonus = 0.0
        var velocityChangeBonus = 0.0

        var strain = currentVelocity

        if (
            Math.max(current.strainTime, last.strainTime) < 1.25 * Math.min(current.strainTime, last.strainTime) &&
            !current.angle.isNaN() &&
            !last.angle.isNaN() &&
            lastLast != null && !lastLast.angle.isNaN()
        ) {
            val angleBonus = Math.min(currentVelocity, prevVelocity)

            wideAngleBonus = calculateWideAngleBonus(current.angle)
            acuteAngleBonus = calculateAcuteAngleBonus(current.angle)

            if (current.strainTime > 100) {
                acuteAngleBonus = 0.0
            } else {
                acuteAngleBonus *=
                        calculateAcuteAngleBonus(last.angle) *
                        Math.min(angleBonus, 125 / current.strainTime) *
                        Math.pow(Math.sin(Math.PI / 2 * Math.min(1.0, (100 - current.strainTime) / 25)), 2.0) *
                        Math.pow(Math.sin(Math.PI / 2 * (MathUtils.clamp(current.lazyJumpDistance, 50.0, 100.0) - 50) / 50), 2.0)
            }

            wideAngleBonus *= angleBonus * (1 - Math.min(wideAngleBonus, Math.pow(calculateWideAngleBonus(last.angle), 3.0)))
            if (lastLast != null) {
                acuteAngleBonus *= 0.5 + 0.5 * (1 - Math.min(acuteAngleBonus, Math.pow(calculateAcuteAngleBonus(lastLast.angle), 3.0)))
            }
        }

        if (Math.max(prevVelocity, currentVelocity) != 0.0) {
            val avgPrevVelocity = (last.lazyJumpDistance + (lastLast?.travelDistance ?: 0.0)) / last.strainTime
            val avgCurrentVelocity = (current.lazyJumpDistance + last.travelDistance) / current.strainTime

            val distanceRatio = Math.pow(Math.sin(Math.PI / 2 * Math.abs(avgPrevVelocity - avgCurrentVelocity) / Math.max(avgPrevVelocity, avgCurrentVelocity)), 2.0)

            val overlapVelocityBuff = Math.min(125 / Math.min(current.strainTime, last.strainTime), Math.abs(avgPrevVelocity - avgCurrentVelocity))

            velocityChangeBonus = overlapVelocityBuff * distanceRatio

            velocityChangeBonus *= Math.pow(Math.min(current.strainTime, last.strainTime) / Math.max(current.strainTime, last.strainTime), 2.0)
        }

        if (last.`object` is Slider) {
            sliderBonus = last.travelDistance / last.travelTime
        }

        strain += Math.max(acuteAngleBonus * acuteAngleMultiplier, wideAngleBonus * wideAngleMultiplier + velocityChangeBonus * velocityChangeMultiplier)

        if (withSliders) {
            strain += sliderBonus * sliderMultiplier
        }

        return strain
    }

    private fun calculateWideAngleBonus(angle: Double): Double {
        return Math.pow(
            Math.sin(
                (3.0 / 4) *
                        (Math.min((5.0 / 6) * Math.PI, Math.max(Math.PI / 6, angle)) -
                                Math.PI / 6)
            ),
            2.0
        )
    }

    private fun calculateAcuteAngleBonus(angle: Double): Double {
        return 1 - calculateWideAngleBonus(angle)
    }
}
