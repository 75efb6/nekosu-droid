package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight

import org.anddev.andengine.entity.modifier.ScaleModifier
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.ResourceManager

class MainFlashLightSprite : FlashlightAreaSizedSprite(DEFAULT_TEXTURE) {

    companion object {
        private val DEFAULT_TEXTURE: TextureRegion = ResourceManager.getInstance().getTexture("flashlight_cursor")!!
        @JvmField
        val TEXTURE_WIDTH: Int = DEFAULT_TEXTURE.getWidth()
        @JvmField
        val TEXTURE_HEIGHT: Int = DEFAULT_TEXTURE.getHeight()
    }

    @JvmField
    val AREA_CHANGE_FADE_DURATION = 0.8f

    @JvmField
    var currentSize = BASE_SIZE

    private fun changeArea(fromScale: Float, toScale: Float) {
        registerEntityModifier(ScaleModifier(AREA_CHANGE_FADE_DURATION, fromScale, toScale))
    }

    fun onUpdate(combo: Int) {
        handleAreaShrinking(combo)
    }

    fun handleAreaShrinking(combo: Int) {
        if (combo in 1..200 && combo % 100 == 0) {
            val newSize = (1 - 0.1f * combo / 100f) * BASE_SIZE
            changeArea(currentSize, newSize)
            currentSize = newSize
        }
    }

    fun updateBreak(isBreak: Boolean) {
        val fromScale = if (isBreak) currentSize else 1.5f * BASE_SIZE
        val toScale = if (isBreak) 1.5f * BASE_SIZE else currentSize
        changeArea(fromScale, toScale)
    }
}
