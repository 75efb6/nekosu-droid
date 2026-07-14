package com.edlplan.osu.support

enum class SampleSet(val value: String) {
    None("None"),
    Soft("Soft"),
    Normal("Normal"),
    Drum("Drum");

    fun value(): String = value

    override fun toString(): String = value

    companion object {
        @JvmStatic
        fun parse(s: String): SampleSet? = when (s) {
            "0", "None" -> None
            "1", "Normal" -> Normal
            "2", "Soft" -> Soft
            "3", "Drum" -> Drum
            else -> null
        }

        @JvmStatic
        fun fromName(s: String): SampleSet? = when (s) {
            "None" -> None
            "Normal" -> Normal
            "Soft" -> Soft
            "Drum" -> Drum
            else -> null
        }
    }
}
