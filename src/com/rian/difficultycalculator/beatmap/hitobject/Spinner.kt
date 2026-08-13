package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.math.Vector2

class Spinner(startTime: Double, endTime: Double) : HitObjectWithDuration(startTime, endTime, Vector2(256f, 192f)) {
    override fun deepClone(): Spinner {
        return Spinner(startTime, endTime)
    }
}
