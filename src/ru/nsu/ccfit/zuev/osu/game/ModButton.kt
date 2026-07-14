package ru.nsu.ccfit.zuev.osu.game

import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite

class ModButton(x: Float, y: Float) : Sprite(x, y, ResourceManager.getInstance().getTexture("selection-mod-normal")) {

    private var mod = GameMod.MOD_NOFAIL
    private var modList = ArrayList<GameMod>()
    private var modImage: AnimSprite? = null

    init {
        modList.add(GameMod.MOD_AUTO)
        modList.add(GameMod.MOD_HIDDEN)
        modList.add(GameMod.MOD_HARDROCK)
        modList.add(GameMod.MOD_EASY)
        modList.add(GameMod.MOD_DOUBLETIME)
        modList.add(GameMod.MOD_NIGHTCORE)
        modList.add(GameMod.MOD_FLASHLIGHT)
        modList.add(GameMod.MOD_RELAX)
        modList.add(GameMod.MOD_AUTOPILOT)
        modList.add(GameMod.MOD_SUDDENDEATH)
        modList.add(GameMod.MOD_PERFECT)
        modList.add(GameMod.MOD_SCOREV2)
    }

    fun setMod(mod: GameMod) {
        this.mod = mod
        if (modImage != null) detachChild(modImage)
        modImage = AnimSprite(
            0f, 0f,
            "selection-mod-${mod.shortName}", 1, 0f
        )
        modImage!!.setPosition(
            x + (getWidth() - modImage!!.getWidth()) / 2,
            y + (getHeight() - modImage!!.getHeight()) / 2
        )
        attachChild(modImage)
    }

    fun getMod(): GameMod = mod

    fun nextMod() {
        val index = modList.indexOf(mod)
        if (index == -1 || index == modList.size - 1) {
            setMod(modList[0])
        } else {
            setMod(modList[index + 1])
        }
    }

    fun prevMod() {
        val index = modList.indexOf(mod)
        if (index <= 0) {
            setMod(modList[modList.size - 1])
        } else {
            setMod(modList[index - 1])
        }
    }
}
