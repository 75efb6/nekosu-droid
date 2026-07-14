package com.rian.difficultycalculator.beatmap.hitobject.sliderobject

import com.rian.difficultycalculator.math.Vector2

class SliderHead : SliderHitObject {
    constructor(startTime: Double, x: Float, y: Float) : this(startTime, Vector2(x, y))

    constructor(startTime: Double, position: Vector2) : super(startTime, position, 0, startTime)

    private constructor(source: SliderHead) : this(source.startTime, source.position.x, source.position.y)

    override fun deepClone(): SliderHead {
        return SliderHead(this)
    }
}
