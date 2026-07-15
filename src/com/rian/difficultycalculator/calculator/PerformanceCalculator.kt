package com.rian.difficultycalculator.calculator

import com.rian.difficultycalculator.attributes.DifficultyAttributes
import com.rian.difficultycalculator.attributes.PerformanceAttributes
import com.rian.difficultycalculator.math.MathUtils

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class PerformanceCalculator {
    @JvmField
    val difficultyAttributes: DifficultyAttributes

    private var scoreMaxCombo: Int = 0
    private var countGreat: Int = 0
    private var countOk: Int = 0
    private var countMeh: Int = 0
    private var countMiss: Int = 0
    private var effectiveMissCount: Double = 0.0

    constructor(attributes: DifficultyAttributes) {
        this.difficultyAttributes = attributes

        processParameters(null)
    }

    fun calculate(): PerformanceAttributes {
        return createPerformanceAttributes()
    }

    fun calculate(parameters: PerformanceCalculationParameters): PerformanceAttributes {
        processParameters(parameters)

        return createPerformanceAttributes()
    }

    private fun createPerformanceAttributes(): PerformanceAttributes {
        var multiplier = finalMultiplier

        if (difficultyAttributes.mods.contains(GameMod.MOD_NOFAIL)) {
            multiplier *= Math.max(0.9, 1 - 0.02 * effectiveMissCount)
        }

        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            val okMultiplier = Math.max(0.0, if (difficultyAttributes.overallDifficulty > 0) 1 - Math.pow(difficultyAttributes.overallDifficulty / 13.33, 1.8) else 1.0)
            val mehMultiplier = Math.max(0.0, if (difficultyAttributes.overallDifficulty > 0) 1 - Math.pow(difficultyAttributes.overallDifficulty / 13.33, 5.0) else 1.0)

            effectiveMissCount = Math.min(effectiveMissCount + countOk * okMultiplier + countMeh * mehMultiplier, getTotalHits().toDouble())
        }

        val attributes = PerformanceAttributes()

        attributes.effectiveMissCount = effectiveMissCount
        attributes.aim = calculateAimValue()
        attributes.speed = calculateSpeedValue()
        attributes.accuracy = calculateAccuracyValue()
        attributes.flashlight = calculateFlashlightValue()

        attributes.total = Math.pow(
            Math.pow(attributes.aim, 1.1) +
                    Math.pow(attributes.speed, 1.1) +
                    Math.pow(attributes.accuracy, 1.1) +
                    Math.pow(attributes.flashlight, 1.1),
            1 / 1.1
        ) * multiplier

        return attributes
    }

    private fun processParameters(parameters: PerformanceCalculationParameters?) {
        if (parameters == null) {
            resetDefaults()
            return
        }

        scoreMaxCombo = parameters.maxCombo
        countGreat = parameters.countGreat
        countOk = parameters.countOk
        countMeh = parameters.countMeh
        countMiss = parameters.countMiss
        effectiveMissCount = calculateEffectiveMissCount()
    }

    private fun getAccuracy(): Double {
        return (countGreat * 6 + countOk * 2 + countMeh).toDouble() / (getTotalHits() * 6)
    }

    private fun getTotalHits(): Int {
        return difficultyAttributes.hitCircleCount + difficultyAttributes.sliderCount + difficultyAttributes.spinnerCount
    }

    private fun resetDefaults() {
        scoreMaxCombo = difficultyAttributes.maxCombo
        countGreat = getTotalHits()
        countOk = 0
        countMeh = 0
        countMiss = 0
        effectiveMissCount = 0.0
    }

    private fun calculateAimValue(): Double {
        var aimValue = Math.pow(5 * Math.max(1.0, difficultyAttributes.aimDifficulty / 0.0675) - 4, 3.0) / 100000

        var lengthBonus = 0.95 + 0.4 * Math.min(1.0, getTotalHits() / 2000.0)
        if (getTotalHits() > 2000) {
            lengthBonus += Math.log10(getTotalHits() / 2000.0) * 0.5
        }

        aimValue *= lengthBonus

        if (effectiveMissCount > 0) {
            aimValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), effectiveMissCount)
        }

        aimValue *= getComboScalingFactor()

        if (!difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            var approachRateFactor = 0.0
            if (difficultyAttributes.approachRate > 10.33) {
                approachRateFactor += 0.3 * (difficultyAttributes.approachRate - 10.33)
            } else if (difficultyAttributes.approachRate < 8) {
                approachRateFactor += 0.05 * (8 - difficultyAttributes.approachRate)
            }

            aimValue *= 1 + approachRateFactor * lengthBonus
        }

        if (difficultyAttributes.mods.contains(GameMod.MOD_HIDDEN)) {
            aimValue *= 1 + 0.04 * (12 - difficultyAttributes.approachRate)
        }

        val estimateDifficultSliders = difficultyAttributes.sliderCount * 0.15

        if (estimateDifficultSliders > 0) {
            val estimateSliderEndsDropped = MathUtils.clamp(Math.min((countOk + countMeh + countMiss).toDouble(), (difficultyAttributes.maxCombo - scoreMaxCombo).toDouble()), 0.0, estimateDifficultSliders)
            val sliderNerfFactor = (1 - difficultyAttributes.aimSliderFactor) * Math.pow(1 - estimateSliderEndsDropped / estimateDifficultSliders, 3.0) + difficultyAttributes.aimSliderFactor
            aimValue *= sliderNerfFactor
        }

        aimValue *= getAccuracy()

        aimValue *= 0.98 + Math.pow(difficultyAttributes.overallDifficulty, 2.0) / 2500

        return aimValue
    }

    private fun calculateSpeedValue(): Double {
        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            return 0.0
        }

        var speedValue = Math.pow(5 * Math.max(1.0, difficultyAttributes.speedDifficulty / 0.0675) - 4, 3.0) / 100000

        var lengthBonus = 0.95 + 0.4 * Math.min(1.0, getTotalHits() / 2000.0)
        if (getTotalHits() > 2000) {
            lengthBonus += Math.log10(getTotalHits() / 2000.0) * 0.5
        }

        speedValue *= lengthBonus

        if (effectiveMissCount > 0) {
            speedValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), Math.pow(effectiveMissCount, 0.875))
        }

        speedValue *= getComboScalingFactor()

        if (difficultyAttributes.approachRate > 10.33) {
            speedValue *= 1 + 0.3 * (difficultyAttributes.approachRate - 10.33) * lengthBonus
        }

        if (difficultyAttributes.mods.contains(GameMod.MOD_HIDDEN)) {
            speedValue *= 1 + 0.04 * (12 - difficultyAttributes.approachRate)
        }

        val relevantTotalDiff = getTotalHits() - difficultyAttributes.speedNoteCount
        val relevantCountGreat = Math.max(0.0, countGreat - relevantTotalDiff)
        val relevantCountOk = Math.max(0.0, countOk - Math.max(0.0, relevantTotalDiff - countGreat))
        val relevantCountMeh = Math.max(0.0, countMeh - Math.max(0.0, relevantTotalDiff - countGreat - countOk))
        val relevantAccuracy = if (difficultyAttributes.speedNoteCount == 0.0) 0.0 else (relevantCountGreat * 6 + relevantCountOk * 2 + relevantCountMeh) / (difficultyAttributes.speedNoteCount * 6)

        speedValue *= (0.95 + Math.pow(difficultyAttributes.overallDifficulty, 2.0) / 750) * Math.pow((getAccuracy() + relevantAccuracy) / 2, (14.5 - Math.max(difficultyAttributes.overallDifficulty, 8.0)) / 2)

        speedValue *= Math.pow(0.99, Math.max(0.0, countMeh - getTotalHits() / 500.0))

        return speedValue
    }

    private fun calculateAccuracyValue(): Double {
        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            return 0.0
        }

        var betterAccuracyPercentage = 0.0
        val circleCount = difficultyAttributes.hitCircleCount

        if (circleCount > 0) {
            betterAccuracyPercentage = Math.max(0.0, ((countGreat - (getTotalHits() - circleCount)) * 6 + countOk * 2 + countMeh) / (circleCount * 6.0))
        }

        var accuracyValue = Math.pow(1.52163, difficultyAttributes.overallDifficulty) * Math.pow(betterAccuracyPercentage, 24.0) * 2.83

        accuracyValue *= Math.min(1.15, Math.pow(circleCount / 1000.0, 0.3))

        if (difficultyAttributes.mods.contains(GameMod.MOD_HIDDEN)) {
            accuracyValue *= 1.08
        }
        if (difficultyAttributes.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            accuracyValue *= 1.02
        }

        return accuracyValue
    }

    private fun calculateFlashlightValue(): Double {
        if (!difficultyAttributes.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            return 0.0
        }

        var flashlightValue = Math.pow(difficultyAttributes.flashlightDifficulty, 2.0) * 25

        if (effectiveMissCount > 0) {
            flashlightValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), Math.pow(effectiveMissCount, 0.875))
        }

        flashlightValue *= getComboScalingFactor()

        flashlightValue *= 0.7 + 0.1 * Math.min(1.0, getTotalHits() / 200.0) +
                (if (getTotalHits() > 200) 0.2 * Math.min(1.0, (getTotalHits() - 200) / 200.0) else 0.0)

        flashlightValue *= 0.5 + getAccuracy() / 2

        flashlightValue *= 0.98 + Math.pow(difficultyAttributes.overallDifficulty, 2.0) / 2500

        return flashlightValue
    }

    private fun calculateEffectiveMissCount(): Double {
        var comboBasedMissCount = 0.0

        if (difficultyAttributes.sliderCount > 0) {
            val fullComboThreshold = difficultyAttributes.maxCombo - 0.1 * difficultyAttributes.sliderCount

            if (scoreMaxCombo < fullComboThreshold) {
                comboBasedMissCount = Math.min(
                    fullComboThreshold / Math.max(1.0, scoreMaxCombo.toDouble()),
                    (countOk + countMeh + countMiss).toDouble()
                )
            }
        }

        return Math.max(countMiss.toDouble(), comboBasedMissCount)
    }

    private fun getComboScalingFactor(): Double {
        return if (difficultyAttributes.maxCombo <= 0) 0.0 else Math.min(Math.pow(scoreMaxCombo.toDouble(), 0.8) / Math.pow(difficultyAttributes.maxCombo.toDouble(), 0.8), 1.0)
    }

    companion object {
        @JvmField
        val finalMultiplier = 1.14
    }
}
