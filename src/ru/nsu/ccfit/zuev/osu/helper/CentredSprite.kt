package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion

class CentredSprite(pX: Float, pY: Float, pTextureRegion: TextureRegion) : Sprite(
    pX - pTextureRegion.width / 2,
    pY - pTextureRegion.height / 2,
    pTextureRegion
)
