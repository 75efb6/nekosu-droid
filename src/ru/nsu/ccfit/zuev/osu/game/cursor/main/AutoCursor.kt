package ru.nsu.ccfit.zuev.osu.game.cursor.main

import org.anddev.andengine.entity.modifier.MoveModifier
import org.anddev.andengine.util.modifier.ease.EaseQuadOut
import org.anddev.andengine.util.modifier.ease.IEaseFunction
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.ISliderListener
import ru.nsu.ccfit.zuev.osu.game.Spinner

class AutoCursor : CursorEntity(), ISliderListener {

    private var currentModifier: MoveModifier? = null
    private var currentObjectId = -1
    private val easeFunction: IEaseFunction = EaseQuadOut.getInstance()

    init {
        setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        setShowing(true)
    }

    private fun doEasingAutoMove(pX: Float, pY: Float, durationS: Float) {
        unregisterEntityModifier(currentModifier)
        currentModifier = MoveModifier(durationS, x, pX, y, pY, easeFunction)
        registerEntityModifier(currentModifier!!)
    }

    private fun doAutoMove(pX: Float, pY: Float, durationS: Float, listener: GameObjectListener) {
        if (durationS <= 0) {
            setPosition(pX, pY, listener)
            click()
        } else {
            doEasingAutoMove(pX, pY, durationS)
        }
        listener.onUpdatedAutoCursor(pX, pY)
    }

    fun setPosition(pX: Float, pY: Float, listener: GameObjectListener) {
        setPosition(pX, pY)
        listener.onUpdatedAutoCursor(pX, pY)
    }

    fun moveToObject(`object`: GameObject, secPassed: Float, listener: GameObjectListener) {
        if (`object` == null || currentObjectId == `object`.getId()) return

        var movePositionX = `object`.getPos().x
        var movePositionY = `object`.getPos().y
        var deltaT = `object`.getHitTime() - secPassed

        if (`object` is Spinner) {
            movePositionX = `object`.getPos().x
            movePositionY = `object`.getPos().y + 50
        }

        currentObjectId = `object`.getId()

        if (deltaT < 0.085f && `object` !is Spinner) {
            deltaT = 0.085f
        }

        doAutoMove(movePositionX, movePositionY, deltaT, listener)
    }

    override fun onSliderStart() {
        cursorSprite.onSliderStart()
    }

    override fun onSliderTracking() {
        cursorSprite.onSliderTracking()
    }

    override fun onSliderEnd() {
        cursorSprite.onSliderEnd()
    }
}
