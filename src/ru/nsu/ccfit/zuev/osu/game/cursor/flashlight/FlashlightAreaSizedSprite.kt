package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion

open class FlashlightAreaSizedSprite(pTextureRegion: TextureRegion) : Sprite(
    -MainFlashLightSprite.TEXTURE_WIDTH / 2f,
    -MainFlashLightSprite.TEXTURE_HEIGHT / 2f,
    pTextureRegion
) {
    companion object {
        @JvmField
        val BASE_SIZE = 6f
    }

    init {
        setScale(BASE_SIZE)
    }
}
