package ru.nsu.ccfit.zuev.osu.game.mods

enum class GameMod(
    @JvmField val shortName: String,
    @JvmField val scoreMultiplier: Float,
    @JvmField val unranked: Boolean = false
) {
    MOD_NOFAIL("nf", 0.5f),
    MOD_AUTO("auto", 1f, true),
    MOD_EASY("es", 0.5f),
    MOD_HARDROCK("hr", 1.06f),
    MOD_HIDDEN("hd", 1.06f),
    MOD_RELAX("relax", 0.001f, true),
    MOD_AUTOPILOT("ap", 0.001f, true),
    MOD_DOUBLETIME("dt", 1.12f),
    MOD_NIGHTCORE("nc", 1.12f),
    MOD_HALFTIME("ht", 0.3f),
    MOD_SUDDENDEATH("sd", 1f),
    MOD_PERFECT("pf", 1f),
    MOD_FLASHLIGHT("fl", 1.12f),
    MOD_PRECISE("pr", 1.06f, true),
    MOD_REALLYEASY("re", 0.5f, true),
    MOD_SCOREV2("v2", 1f, true);
}
