package com.rian.difficultycalculator.beatmap

class BeatmapDifficultyManager {
    private var cs: Float = 5f

    private var ar: Float = Float.NaN

    private var od: Float = 5f

    private var hp: Float = 5f

    private var sliderMultiplier: Double = 1.0

    private var sliderTickRate: Double = 1.0

    constructor()

    private constructor(source: BeatmapDifficultyManager) {
        cs = source.cs
        ar = source.ar
        od = source.od
        hp = source.hp
        sliderMultiplier = source.sliderMultiplier
        sliderTickRate = source.sliderTickRate
    }

    fun deepClone(): BeatmapDifficultyManager {
        return BeatmapDifficultyManager(this)
    }

    fun getCS(): Float {
        return cs
    }

    fun setCS(cs: Float) {
        this.cs = cs
    }

    fun getAR(): Float {
        return if (ar.isNaN()) od else ar
    }

    fun setAR(ar: Float) {
        this.ar = ar
    }

    fun getOD(): Float {
        return od
    }

    fun setOD(od: Float) {
        this.od = od
    }

    fun getHP(): Float {
        return hp
    }

    fun setHP(hp: Float) {
        this.hp = hp
    }

    fun getSliderMultiplier(): Double {
        return sliderMultiplier
    }

    fun setSliderMultiplier(sliderMultiplier: Double) {
        this.sliderMultiplier = sliderMultiplier
    }

    fun getSliderTickRate(): Double {
        return sliderTickRate
    }

    fun setSliderTickRate(sliderTickRate: Double) {
        this.sliderTickRate = sliderTickRate
    }
}
