package com.edlplan.osu.support.slider

import com.edlplan.framework.math.line.LinePath
import org.anddev.andengine.entity.scene.Scene

abstract class AbstractSliderBody(protected var path: LinePath) {

    open fun setSliderBodyBaseAlpha(sliderBodyBaseAlpha: Float) {}

    abstract fun onUpdate()

    abstract fun setBodyWidth(width: Float)

    abstract fun setBorderWidth(width: Float)

    abstract fun setBodyColor(r: Float, g: Float, b: Float)

    abstract fun setBorderColor(r: Float, g: Float, b: Float)

    abstract fun setStartLength(length: Float)

    abstract fun setEndLength(length: Float)

    abstract fun applyToScene(scene: Scene, emptyOnStart: Boolean)

    abstract fun removeFromScene(scene: Scene)
}
