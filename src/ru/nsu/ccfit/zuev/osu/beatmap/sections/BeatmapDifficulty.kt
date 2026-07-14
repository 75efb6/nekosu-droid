package ru.nsu.ccfit.zuev.osu.beatmap.sections

class BeatmapDifficulty {
    var ar: Float = Float.NaN
    var cs: Float = 5f
    var od: Float = 5f
    var hp: Float = 5f
    var sliderMultiplier: Double = 1.0
    var sliderTickRate: Double = 1.0

    constructor()

    private constructor(source: BeatmapDifficulty) {
        ar = source.ar
        od = source.od
        cs = source.cs
        hp = source.hp
        sliderMultiplier = source.sliderMultiplier
        sliderTickRate = source.sliderTickRate
    }

    fun deepClone(): BeatmapDifficulty = BeatmapDifficulty(this)
}
