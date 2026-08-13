package ru.nsu.ccfit.zuev.osu.game.mods

interface IModSwitcher {
    fun switchMod(mod: GameMod): Boolean
}
