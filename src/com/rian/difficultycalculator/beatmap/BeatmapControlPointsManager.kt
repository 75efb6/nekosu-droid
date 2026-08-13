package com.rian.difficultycalculator.beatmap

import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPointManager
import com.rian.difficultycalculator.beatmap.timings.TimingControlPointManager

class BeatmapControlPointsManager {
    @JvmField
    val timing: TimingControlPointManager

    @JvmField
    val difficulty: DifficultyControlPointManager

    constructor() {
        timing = TimingControlPointManager()
        difficulty = DifficultyControlPointManager()
    }

    private constructor(source: BeatmapControlPointsManager) {
        timing = source.timing.deepClone()
        difficulty = source.difficulty.deepClone()
    }

    fun deepClone(): BeatmapControlPointsManager {
        return BeatmapControlPointsManager(this)
    }
}
