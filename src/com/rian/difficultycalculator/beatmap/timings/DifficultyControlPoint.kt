package com.rian.difficultycalculator.beatmap.timings

class DifficultyControlPoint : ControlPoint {
    @JvmField
    val speedMultiplier: Double

    @JvmField
    val generateTicks: Boolean

    constructor(time: Double, speedMultiplier: Double, generateTicks: Boolean) : super(time) {
        this.speedMultiplier = speedMultiplier
        this.generateTicks = generateTicks
    }

    private constructor(source: DifficultyControlPoint) : this(source.time, source.speedMultiplier, source.generateTicks)

    override fun isRedundant(existing: ControlPoint): Boolean {
        return existing is DifficultyControlPoint &&
                speedMultiplier == existing.speedMultiplier &&
                generateTicks == existing.generateTicks
    }

    override fun deepClone(): DifficultyControlPoint {
        return DifficultyControlPoint(this)
    }
}
