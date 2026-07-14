package com.rian.difficultycalculator.evaluators

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import kotlin.UnsupportedOperationException

object RhythmEvaluator {
    private const val rhythmMultiplier = 0.75

    @JvmStatic
    fun evaluateDifficultyOf(current: DifficultyHitObject, greatWindow: Double): Double {
        if (current.`object` is Spinner) {
            return 0.0
        }

        var previousIslandSize = 0
        var rhythmComplexitySum = 0.0
        var islandSize = 1

        var startRatio = 0.0

        var firstDeltaSwitch = false
        var rhythmStart = 0

        val historicalNoteCount = Math.min(current.index, 32)

        val historyTimeMax = 5000
        while (rhythmStart < historicalNoteCount - 2 &&
            current.startTime - current.previous(rhythmStart)!!.startTime < historyTimeMax
        ) {
            ++rhythmStart
        }

        for (i in rhythmStart downTo 1) {
            val currentObject = current.previous(i - 1)!!
            val prevObject = current.previous(i)!!
            val lastObject = current.previous(i + 1)!!

            val currentHistoricalDecay = (historyTimeMax - (current.startTime - currentObject.startTime)) / historyTimeMax

            val decay = Math.min(currentHistoricalDecay, (historicalNoteCount - i).toDouble() / historicalNoteCount)

            val currentDelta = currentObject.strainTime
            val prevDelta = prevObject.strainTime
            val lastDelta = lastObject.strainTime

            val currentRatio = 1 + 6 * Math.min(0.5, Math.pow(Math.sin(Math.PI / (Math.min(prevDelta, currentDelta) / Math.max(prevDelta, currentDelta))), 2.0))

            val windowPenalty = Math.min(1.0, Math.max(0.0, Math.abs(prevDelta - currentDelta) - greatWindow * 0.6) / (greatWindow * 0.6))

            val effectiveRatio = windowPenalty * currentRatio

            if (firstDeltaSwitch) {
                if (prevDelta <= 1.25 * currentDelta && prevDelta * 1.25 >= currentDelta) {
                    if (islandSize < 7) {
                        ++islandSize
                    }
                } else {
                    var ratio = effectiveRatio

                    if (currentObject.`object` is Slider) {
                        ratio /= 8
                    }

                    if (prevObject.`object` is Slider) {
                        ratio /= 4
                    }

                    if (previousIslandSize == islandSize) {
                        ratio /= 4
                    }

                    if (previousIslandSize % 2 == islandSize % 2) {
                        ratio /= 2
                    }

                    if (lastDelta > prevDelta + 10 && prevDelta > currentDelta + 10) {
                        ratio /= 8
                    }

                    rhythmComplexitySum += Math.sqrt(ratio * startRatio) * decay * Math.sqrt(4.0 + islandSize) / 2 * Math.sqrt(4.0 + previousIslandSize) / 2

                    startRatio = ratio

                    previousIslandSize = islandSize

                    if (prevDelta * 1.25 < currentDelta) {
                        firstDeltaSwitch = false
                    }

                    islandSize = 1
                }
            } else if (prevDelta > 1.25 * currentDelta) {
                firstDeltaSwitch = true
                startRatio = effectiveRatio
                islandSize = 1
            }
        }

        return Math.sqrt(4 + rhythmComplexitySum * rhythmMultiplier) / 2
    }
}
