package com.rian.difficultycalculator.math

import android.graphics.PointF

class Vector2 {
    @JvmField
    var x: Float = 0f

    @JvmField
    var y: Float = 0f

    @JvmOverloads
    constructor(value: Float) : this(value, value)

    constructor(pointF: PointF) : this(pointF.x, pointF.y)

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun multiply(vec: Vector2): Vector2 {
        return Vector2(x * vec.x, y * vec.y)
    }

    fun divide(divideFactor: Float): Vector2 {
        if (divideFactor == 0f) {
            throw ArithmeticException("Division by 0")
        }
        return Vector2(x / divideFactor, y / divideFactor)
    }

    fun add(vec: Vector2): Vector2 {
        return Vector2(x + vec.x, y + vec.y)
    }

    fun subtract(vec: Vector2): Vector2 {
        return Vector2(x - vec.x, y - vec.y)
    }

    fun getLength(): Float {
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    fun getLengthSquared(): Float {
        return x * x + y * y
    }

    fun dot(vec: Vector2): Float {
        return x * vec.x + y * vec.y
    }

    fun scale(scaleFactor: Float): Vector2 {
        return Vector2(x * scaleFactor, y * scaleFactor)
    }

    fun getDistance(vec: Vector2): Float {
        return Math.sqrt(((vec.x - x) * (vec.x - x) + (vec.y - y) * (vec.y - y)).toDouble()).toFloat()
    }

    fun normalize() {
        val length = getLength()
        x /= length
        y /= length
    }

    fun equals(other: Vector2): Boolean {
        return x == other.x && y == other.y
    }
}
