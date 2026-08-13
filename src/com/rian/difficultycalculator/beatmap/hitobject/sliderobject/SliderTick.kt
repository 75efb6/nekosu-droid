package com.rian.difficultycalculator.beatmap.hitobject.sliderobject

import com.rian.difficultycalculator.math.Vector2

class SliderTick : SliderHitObject {
    constructor(startTime: Double, x: Float, y: Float, spanIndex: Int, spanStartTime: Double) :
            this(startTime, Vector2(x, y), spanIndex, spanStartTime)

    constructor(startTime: Double, position: Vector2, spanIndex: Int, spanStartTime: Double) :
            super(startTime, position, spanIndex, spanStartTime)

    private constructor(source: SliderTick) :
            this(source.startTime, source.position.x, source.position.y, source.spanIndex, source.spanStartTime)

    override fun deepClone(): SliderTick {
        return SliderTick(this)
    }
}
