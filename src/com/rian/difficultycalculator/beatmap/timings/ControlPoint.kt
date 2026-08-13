package com.rian.difficultycalculator.beatmap.timings

abstract class ControlPoint {
    @JvmField
    val time: Double

    constructor(time: Double) {
        this.time = time
    }

    abstract fun isRedundant(existing: ControlPoint): Boolean

    open fun deepClone(): ControlPoint {
        return this
    }
}
