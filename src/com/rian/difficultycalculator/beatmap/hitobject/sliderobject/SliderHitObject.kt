package com.rian.difficultycalculator.beatmap.hitobject.sliderobject

import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.math.Vector2

abstract class SliderHitObject : HitObject {
    @JvmField
    protected val spanIndex: Int

    @JvmField
    protected val spanStartTime: Double

    constructor(startTime: Double, x: Float, y: Float, spanIndex: Int, spanStartTime: Double) :
            this(startTime, Vector2(x, y), spanIndex, spanStartTime)

    constructor(startTime: Double, position: Vector2, spanIndex: Int, spanStartTime: Double) : super(startTime, position) {
        this.spanIndex = spanIndex
        this.spanStartTime = spanStartTime
    }

    fun getSpanIndex(): Int {
        return spanIndex
    }

    fun getSpanStartTime(): Double {
        return spanStartTime
    }
}
