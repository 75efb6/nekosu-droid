package com.rian.difficultycalculator.beatmap

import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider

class DifficultyBeatmap {
    private var formatVersion: Int = 14

    private var stackLeniency: Float = 0.7f

    private val difficultyManager: BeatmapDifficultyManager

    private val hitObjectsManager: BeatmapHitObjectsManager

    constructor(difficultyManager: BeatmapDifficultyManager) {
        this.difficultyManager = difficultyManager.deepClone()
        this.hitObjectsManager = BeatmapHitObjectsManager()
    }

    constructor(difficultyManager: BeatmapDifficultyManager, hitObjectsManager: BeatmapHitObjectsManager) {
        this.difficultyManager = difficultyManager.deepClone()
        this.hitObjectsManager = hitObjectsManager.deepClone()
    }

    private constructor(source: DifficultyBeatmap) : this(source.difficultyManager, source.hitObjectsManager) {
        formatVersion = source.formatVersion
        stackLeniency = source.stackLeniency
    }

    fun getDifficultyManager(): BeatmapDifficultyManager {
        return difficultyManager
    }

    fun getHitObjectsManager(): BeatmapHitObjectsManager {
        return hitObjectsManager
    }

    fun getFormatVersion(): Int {
        return formatVersion
    }

    fun setFormatVersion(formatVersion: Int) {
        this.formatVersion = formatVersion
    }

    fun getStackLeniency(): Float {
        return stackLeniency
    }

    fun setStackLeniency(stackLeniency: Float) {
        this.stackLeniency = stackLeniency
    }

    fun getOffsetTime(time: Double): Double {
        return time + (if (formatVersion < 5) 24.0 else 0.0)
    }

    fun getMaxCombo(): Int {
        var combo = 0

        for (obj in hitObjectsManager.getObjects()) {
            ++combo

            if (obj is Slider) {
                combo += obj.getNestedHitObjects().size - 1
            }
        }

        return combo
    }

    fun deepClone(): DifficultyBeatmap {
        return DifficultyBeatmap(this)
    }
}
