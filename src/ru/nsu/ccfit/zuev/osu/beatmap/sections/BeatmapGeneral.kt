package ru.nsu.ccfit.zuev.osu.beatmap.sections

import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapCountdown
import ru.nsu.ccfit.zuev.osu.beatmap.constants.SampleBank

class BeatmapGeneral {
    var audioFilename: String = ""
    var audioLeadIn: Int = 0
    var previewTime: Int = -1
    var countdown: BeatmapCountdown = BeatmapCountdown.normal
    var sampleBank: SampleBank = SampleBank.normal
    var sampleVolume: Int = 100
    var stackLeniency: Float = 0.7f
    var letterboxInBreaks: Boolean = false
    var mode: Int = 0

    constructor()

    private constructor(source: BeatmapGeneral) {
        audioFilename = source.audioFilename
        audioLeadIn = source.audioLeadIn
        previewTime = source.previewTime
        countdown = source.countdown
        sampleBank = source.sampleBank
        sampleVolume = source.sampleVolume
        stackLeniency = source.stackLeniency
        letterboxInBreaks = source.letterboxInBreaks
        mode = source.mode
    }

    fun deepClone(): BeatmapGeneral = BeatmapGeneral(this)
}
