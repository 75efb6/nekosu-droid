package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import java.util.BitSet
import ru.nsu.ccfit.zuev.osu.RGBColor

interface GameObjectListener {

    companion object {
        const val SLIDER_START = 1
        const val SLIDER_REPEAT = 2
        const val SLIDER_END = 3
        const val SLIDER_TICK = 4
    }

    fun onCircleHit(id: Int, accuracy: Float, pos: PointF, endCombo: Boolean, forcedScore: Byte, color: RGBColor)
    fun onSliderHit(id: Int, score: Int, start: PointF?, end: PointF?, endCombo: Boolean, color: RGBColor, type: Int)
    fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet)
    fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int)
    fun playSound(name: String, sampleSet: Int, addition: Int)
    fun stopSound(name: String)
    fun addObject(`object`: GameObject)
    fun removeObject(`object`: GameObject)
    fun addPassiveObject(`object`: GameObject)
    fun removePassiveObject(`object`: GameObject)
    fun getMousePos(index: Int): PointF
    fun isMouseDown(index: Int): Boolean
    fun isMousePressed(`object`: GameObject, index: Int): Boolean
    fun downFrameOffset(index: Int): Double
    fun getCursorsCount(): Int
    fun registerAccuracy(acc: Double)
    fun updateAutoBasedPos(pX: Float, pY: Float)
    fun onTrackingSliders(isTrackingSliders: Boolean)
    fun onUpdatedAutoCursor(pX: Float, pY: Float)
    fun onSliderReverse(pos: PointF, ang: Float, color: RGBColor)

    fun passed(obj: GameObject, last: Boolean) {}
    fun objectClicked(obj: GameObject, pos: PointF, id: Int) {}
    fun onSpinnerStart(obj: Spinner) {}
    fun onSpinnerEnd(obj: Spinner, complete: Boolean, score: Float) {}
}
