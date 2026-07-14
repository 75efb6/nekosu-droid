package ru.nsu.ccfit.zuev.osu.storyboard

enum class Command {
    F, M, MX, MY, S, V, R, C, P, L, T, NONE;

    companion object {
        @JvmStatic
        fun getType(type: String): Command {
            return try {
                valueOf(type.uppercase())
            } catch (e: Exception) {
                NONE
            }
        }
    }
}
