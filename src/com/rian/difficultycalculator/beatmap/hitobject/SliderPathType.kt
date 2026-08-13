package com.rian.difficultycalculator.beatmap.hitobject

enum class SliderPathType {
    Catmull,
    Bezier,
    Linear,
    PerfectCurve;

    companion object {
        @JvmStatic
        fun parse(value: Char): SliderPathType {
            return when (value) {
                'C' -> Catmull
                'L' -> Linear
                'P' -> PerfectCurve
                else -> Bezier
            }
        }
    }
}
