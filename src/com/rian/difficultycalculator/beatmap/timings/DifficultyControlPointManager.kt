package com.rian.difficultycalculator.beatmap.timings

class DifficultyControlPointManager : ControlPointManager<DifficultyControlPoint> {
    constructor() : super(DifficultyControlPoint(0.0, 1.0, true))

    private constructor(source: DifficultyControlPointManager) : super(source.defaultControlPoint) {
        for (point in source.controlPoints) {
            controlPoints.add(point.deepClone())
        }
    }

    override fun controlPointAt(time: Double): DifficultyControlPoint {
        return binarySearchWithFallback(time)
    }

    override fun deepClone(): DifficultyControlPointManager {
        return DifficultyControlPointManager(this)
    }
}
