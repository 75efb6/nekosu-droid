package ru.nsu.ccfit.zuev.osu.game.mods

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils

class ModButton(pX: Float, pY: Float, texture: String, private val mod: GameMod) : Sprite(
    Utils.toRes(pX), Utils.toRes(pY), ResourceManager.getInstance().getTexture(texture)
) {
    private var switcher: IModSwitcher? = null

    init {
        setScale(INITIAL_SCALE)
    }

    fun setSwitcher(switcher: IModSwitcher) {
        this.switcher = switcher
    }

    fun setModEnabled(enabled: Boolean) {
        if (enabled) {
            setScale(SELECTED_SCALE)
            setRotation(SELECTED_ROTATE)
            setColor(1f, 1f, 1f)
        } else {
            setScale(INITIAL_SCALE)
            setRotation(INITIAL_ROTATE)
            setColor(0.7f, 0.7f, 0.7f)
        }
    }

    override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
        if (pSceneTouchEvent.isActionDown && switcher != null) {
            setModEnabled(switcher!!.switchMod(mod))
            return true
        }
        return false
    }

    companion object {
        private const val INITIAL_SCALE = 1.4f
        private const val SELECTED_SCALE = 1.8f
        private const val INITIAL_ROTATE = 0f
        private const val SELECTED_ROTATE = 5f
    }
}
