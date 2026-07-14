package ru.nsu.ccfit.zuev.osu.game.cursor.main

import org.anddev.andengine.entity.modifier.ParallelEntityModifier
import org.anddev.andengine.entity.modifier.RotationByModifier
import org.anddev.andengine.entity.modifier.ScaleModifier
import org.anddev.andengine.entity.modifier.SequenceEntityModifier
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.ISliderListener
import ru.nsu.ccfit.zuev.skins.OsuSkin

class CursorSprite(pX: Float, pY: Float, pTextureRegion: TextureRegion) : Sprite(pX, pY, pTextureRegion), ISliderListener {
    @JvmField
    val baseSize = Config.getCursorSize() * 2
    private val clickAnimationTime = 0.5f / 2f
    private var previousClickModifier: ParallelEntityModifier? = null
    private var currentRotation: RotationByModifier? = null
    private val rotate = OsuSkin.get().isRotateCursor()

    init {
        setScale(baseSize)
    }

    fun clickInModifier(): ScaleModifier {
        return ScaleModifier(clickAnimationTime, scaleX, baseSize * 1.25f)
    }

    fun clickOutModifier(): ScaleModifier {
        return ScaleModifier(clickAnimationTime, scaleX, baseSize)
    }

    fun handleClick() {
        if (previousClickModifier != null) {
            unregisterEntityModifier(previousClickModifier!!)
            setScale(baseSize)
        }
        registerEntityModifier(
            ParallelEntityModifier(
                SequenceEntityModifier(clickInModifier(), clickOutModifier())
            ).also { previousClickModifier = it }
        )
    }

    private fun rotateCursor() {
        if (currentRotation == null || currentRotation!!.isFinished) {
            registerEntityModifier(RotationByModifier(14f, 360f).also { currentRotation = it })
        }
    }

    fun update(pSecondsElapsed: Float) {
        if (scaleX > 2f) {
            setScale(maxOf(baseSize, this.scaleX - baseSize * 0.75f * pSecondsElapsed))
        }

        if (rotate) {
            rotateCursor()
        }
    }

    override fun onSliderStart() {}

    override fun onSliderTracking() {
        registerEntityModifier(clickInModifier())
    }

    override fun onSliderEnd() {
        registerEntityModifier(clickOutModifier())
    }
}
