package com.rian.difficultycalculator.skills

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject
import java.util.EnumSet

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

abstract class Skill {
    @JvmField
    protected val mods: EnumSet<GameMod>

    constructor(mods: EnumSet<GameMod>) {
        this.mods = mods
    }

    abstract fun process(current: DifficultyHitObject)

    abstract fun difficultyValue(): Double
}
