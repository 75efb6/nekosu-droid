package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData

class BeatmapGeneralParser : BeatmapKeyValueSectionParser() {
    override fun parse(data: BeatmapData, line: String) {
        val p = splitProperty(line)

        when (p[0]) {
            "AudioFilename" -> data.general.audioFilename = p[1]
            "AudioLeadIn" -> data.general.audioLeadIn = parseInt(p[1])
            "PreviewTime" -> data.general.previewTime = data.getOffsetTime(parseInt(p[1]))
            "Countdown" -> data.general.countdown = ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapCountdown.parse(p[1])
            "SampleSet" -> data.general.sampleBank = ru.nsu.ccfit.zuev.osu.beatmap.constants.SampleBank.parse(p[1])
            "SampleVolume" -> data.general.sampleVolume = parseInt(p[1])
            "StackLeniency" -> data.general.stackLeniency = parseFloat(p[1])
            "LetterboxInBreaks" -> data.general.letterboxInBreaks = p[1] == "1"
            "Mode" -> data.general.mode = parseInt(p[1])
        }
    }
}
