package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.math.Vector2

abstract class HitObjectWithDuration : HitObject {
    @JvmField
    var endTime: Double

    constructor(startTime: Double, endTime: Double, position: Vector2) : super(startTime, position) {
        this.endTime = endTime
    }

    constructor(startTime: Double, endTime: Double, x: Float, y: Float) : this(startTime, endTime, Vector2(x, y))

    protected constructor(source: HitObjectWithDuration) : super(source) {
        endTime = source.endTime
    }

    fun getEndTime(): Double {
        return endTime
    }

    fun getDuration(): Double {
        return endTime - startTime
    }
}
