package com.rian.difficultycalculator.evaluators

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import com.rian.difficultycalculator.math.MathUtils
import kotlin.UnsupportedOperationException

object SpeedEvaluator {
    private const val singleSpacingThreshold = 125.0
    private const val minSpeedBonus = 75.0

    @JvmStatic
    fun evaluateDifficultyOf(current: DifficultyHitObject, greatWindow: Double): Double {
        if (current.`object` is Spinner) {
            return 0.0
        }

        val prev = current.previous(0)

        var strainTime = current.strainTime
        val greatWindowFull = greatWindow * 2

        var doubletapness = 1.0

        val next = current.next(0)
        if (next != null) {
            val currentDeltaTime = Math.max(1.0, current.deltaTime)
            val nextDeltaTime = Math.max(1.0, next.deltaTime)
            val deltaDifference = Math.abs(nextDeltaTime - currentDeltaTime)
            val speedRatio = currentDeltaTime / Math.max(currentDeltaTime, deltaDifference)
            val windowRatio = Math.pow(Math.min(1.0, currentDeltaTime / greatWindowFull), 2.0)
            doubletapness = Math.pow(speedRatio, 1 - windowRatio)
        }

        strainTime /= MathUtils.clamp(strainTime / greatWindowFull / 0.93, 0.92, 1.0)

        var speedBonus = 1.0
        if (strainTime < minSpeedBonus) {
            speedBonus += 0.75 * Math.pow((minSpeedBonus - strainTime) / 40, 2.0)
        }

        val travelDistance = prev?.travelDistance ?: 0.0
        val distance = Math.min(singleSpacingThreshold, travelDistance + current.minimumJumpDistance)

        return (speedBonus + speedBonus * Math.pow(distance / singleSpacingThreshold, 3.5)) * doubletapness / strainTime
    }
}
