package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight

import android.graphics.Color
import com.edlplan.andengine.TextureHelper
import org.anddev.andengine.entity.modifier.AlphaModifier

class FlashLightDimLayerSprite : FlashlightAreaSizedSprite(TextureHelper.create1xRegion(Color.BLACK)!!) {

    companion object {
        @JvmField
        val BASE_SLIDER_DIM_ALPHA = 0.8f
    }

    init {
        setScale(MainFlashLightSprite.TEXTURE_WIDTH.toFloat(), MainFlashLightSprite.TEXTURE_HEIGHT.toFloat())
        setAlpha(0f)
    }

    fun onTrackingSliders(isTrackingSliders: Boolean) {
        val newAlpha = if (isTrackingSliders) BASE_SLIDER_DIM_ALPHA else 0f
        registerEntityModifier(AlphaModifier(0.05f, alpha, newAlpha))
    }
}
