package com.rian.difficultycalculator.attributes

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.EnumSet

class DifficultyAttributes {
    @JvmField
    var mods: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)

    @JvmField
    var starRating: Double = 0.0

    @JvmField
    var maxCombo: Int = 0

    @JvmField
    var aimDifficulty: Double = 0.0

    @JvmField
    var speedDifficulty: Double = 0.0

    @JvmField
    var flashlightDifficulty: Double = 0.0

    @JvmField
    var speedNoteCount: Double = 0.0

    @JvmField
    var aimSliderFactor: Double = 0.0

    @JvmField
    var approachRate: Double = 0.0

    @JvmField
    var overallDifficulty: Double = 0.0

    @JvmField
    var hitCircleCount: Int = 0

    @JvmField
    var sliderCount: Int = 0

    @JvmField
    var spinnerCount: Int = 0
}
