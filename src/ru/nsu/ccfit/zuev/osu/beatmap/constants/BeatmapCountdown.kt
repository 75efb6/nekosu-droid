package ru.nsu.ccfit.zuev.osu.beatmap.constants

enum class BeatmapCountdown(val speed: Float) {
    noCountdown(0f),
    normal(1f),
    half(0.5f),
    twice(2f);

    companion object {
        @JvmStatic
        fun parse(data: String): BeatmapCountdown = when (data) {
            "0" -> noCountdown
            "2" -> half
            "3" -> twice
            else -> normal
        }
    }
}
