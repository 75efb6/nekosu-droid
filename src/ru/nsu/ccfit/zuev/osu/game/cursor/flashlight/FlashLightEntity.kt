package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight

import com.edlplan.framework.math.FMath
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.entity.modifier.MoveModifier
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.menu.ModMenu

class FlashLightEntity : Entity(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f) {

    companion object {
        const val defaultMoveDelayS = 0.12f
    }

    private val mainSprite = MainFlashLightSprite()
    private val dimLayer = FlashLightDimLayerSprite()
    private var isTrackingSliders = false
    private var currentModifier: IEntityModifier? = null
    private var nextPX = 0f
    private var nextPY = 0f

    init {
        attachChild(mainSprite)
        attachChild(dimLayer)
    }

    fun onBreak(isBreak: Boolean) {
        mainSprite.updateBreak(isBreak)
    }

    fun onMouseMove(pX: Float, pY: Float) {
        val flFollowDelay = ModMenu.getInstance().getFLfollowDelay()

        if (nextPX != 0f && nextPY != 0f && currentModifier != null && this.x != nextPX && this.y != nextPY) {
            unregisterEntityModifier(currentModifier!!)
        }

        nextPX = FMath.clamp(pX, 0f, Config.getRES_WIDTH().toFloat())
        nextPY = FMath.clamp(pY, 0f, Config.getRES_HEIGHT().toFloat())
        currentModifier = MoveModifier(flFollowDelay, this.x, nextPX, this.y, nextPY, EaseExponentialOut.getInstance())

        registerEntityModifier(currentModifier!!)
    }

    fun onTrackingSliders(isTrackingSliders: Boolean) {
        this.isTrackingSliders = isTrackingSliders
    }

    fun onUpdate(combo: Int) {
        dimLayer.onTrackingSliders(isTrackingSliders)
        mainSprite.onUpdate(combo)
    }
}
