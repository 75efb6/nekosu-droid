package ru.nsu.ccfit.zuev.osu.beatmap.constants

enum class HitObjectType(private val value: Int) {
    Normal(1),
    Slider(2),
    NewCombo(4),
    NormalNewCombo(5),
    SliderNewCombo(6),
    Spinner(8);

    fun value(): Int = value

    companion object {
        @JvmStatic
        fun valueOf(value: Int): HitObjectType = when (value) {
            1 -> Normal
            2 -> Slider
            4 -> NewCombo
            5 -> NormalNewCombo
            6 -> SliderNewCombo
            else -> Spinner
        }
    }
}
