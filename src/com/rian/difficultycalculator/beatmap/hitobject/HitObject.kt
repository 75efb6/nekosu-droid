package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.math.Vector2

abstract class HitObject {
    @JvmField
    var startTime: Double

    @JvmField
    var position: Vector2

    @JvmField
    var endPosition: Vector2

    private var stackHeight: Int = 0
    private var scale: Float = 0f

    constructor(startTime: Double, position: Vector2) {
        this.startTime = startTime
        this.position = position
        endPosition = position
    }

    constructor(startTime: Double, x: Float, y: Float) : this(startTime, Vector2(x, y))

    protected constructor(source: HitObject) {
        startTime = source.startTime
        position = Vector2(source.position.x, source.position.y)
        endPosition = Vector2(source.endPosition.x, source.endPosition.y)
        stackHeight = source.stackHeight
        scale = source.scale
    }

    open fun getScale(): Float {
        return scale
    }

    open fun setScale(scale: Float) {
        this.scale = scale
    }

    fun getStartTime(): Double {
        return startTime
    }

    fun getStackHeight(): Int {
        return stackHeight
    }

    fun setStackHeight(stackHeight: Int) {
        this.stackHeight = stackHeight
    }

    fun getPosition(): Vector2 {
        return position
    }

    fun getEndPosition(): Vector2 {
        return endPosition
    }

    fun getRadius(): Double {
        return (OBJECT_RADIUS * scale).toDouble()
    }

    fun getStackOffset(): Vector2 {
        return Vector2(stackHeight * scale * -6.4f)
    }

    fun getStackedPosition(): Vector2 {
        return evaluateStackedPosition(position)
    }

    fun getStackedEndPosition(): Vector2 {
        return evaluateStackedPosition(endPosition)
    }

    protected open fun evaluateStackedPosition(position: Vector2): Vector2 {
        return position.add(getStackOffset())
    }

    open fun deepClone(): HitObject? {
        return null
    }

    companion object {
        @JvmField
        val OBJECT_RADIUS = 64f
    }
}
