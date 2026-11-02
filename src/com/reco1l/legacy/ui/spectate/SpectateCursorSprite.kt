package com.reco1l.legacy.ui.spectate

import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Simple cursor sprite for spectate mode.
 * Shows the remote player's cursor position.
 */
class SpectateCursorSprite : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("cursor")) {

    private var isShowing = false

    override fun setPosition(pX: Float, pY: Float) {
        super.setPosition(pX - this.width / 2f, pY - this.height / 2f)
    }

    fun setShowing(show: Boolean) {
        isShowing = show
        this.isVisible = show
        if (show) {
            this.setAlpha(0.8f)
        }
    }

    fun click() {
        this.setScale(1.2f)
        // Simple click animation - scale back down
        this.registerEntityModifier(
            org.anddev.andengine.entity.modifier.ScaleModifier(0.1f, 1.2f, 1.0f)
        )
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        super.onManagedUpdate(pSecondsElapsed)
        // Keep alpha stable
        if (isShowing && this.alpha < 0.7f) {
            this.setAlpha(0.8f)
        }
    }
}
