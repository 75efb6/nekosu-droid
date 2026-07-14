package ru.nsu.ccfit.zuev.osu.beatmap.constants

enum class SampleBank(val prefix: String) {
    none(""),
    normal("normal"),
    soft("soft"),
    drum("drum");

    companion object {
        @JvmStatic
        fun parse(value: Int): SampleBank = when (value) {
            1 -> normal
            2 -> soft
            3 -> drum
            else -> none
        }

        @JvmStatic
        fun parse(value: String): SampleBank = when (value) {
            "Normal" -> normal
            "Soft" -> soft
            "Drum" -> drum
            else -> none
        }
    }
}
