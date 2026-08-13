package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.math.Vector2

class HitCircle : HitObject {
    constructor(startTime: Double, position: Vector2) : super(startTime, position)

    constructor(startTime: Double, x: Float, y: Float) : super(startTime, x, y)

    private constructor(source: HitCircle) : super(source)

    override fun deepClone(): HitCircle {
        return HitCircle(this)
    }
}
