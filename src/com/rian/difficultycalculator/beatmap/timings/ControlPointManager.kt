package com.rian.difficultycalculator.beatmap.timings

import java.util.Collections

abstract class ControlPointManager<T : ControlPoint> {
    @JvmField
    val defaultControlPoint: T

    @JvmField
    internal val controlPoints: ArrayList<T> = ArrayList()

    constructor(defaultControlPoint: T) {
        this.defaultControlPoint = defaultControlPoint
    }

    abstract fun controlPointAt(time: Double): T

    fun add(controlPoint: T): Boolean {
        val existing = controlPointAt(controlPoint.time)

        if (controlPoint.isRedundant(existing)) {
            return false
        }

        var currentExisting = existing
        while (controlPoint.time == currentExisting.time) {
            if (!remove(currentExisting)) {
                break
            }
            currentExisting = controlPointAt(controlPoint.time)
        }

        controlPoints.add(findInsertionIndex(controlPoint.time), controlPoint)

        return true
    }

    fun remove(controlPoint: T): Boolean {
        return controlPoints.remove(controlPoint)
    }

    fun remove(index: Int): T? {
        if (index < 0 || index > controlPoints.size - 1) {
            return null
        }

        return controlPoints.removeAt(index)
    }

    fun getControlPoints(): List<T> {
        return Collections.unmodifiableList(controlPoints)
    }

    fun clear() {
        controlPoints.clear()
    }

    open fun deepClone(): ControlPointManager<T>? {
        return null
    }

    protected fun binarySearchWithFallback(time: Double): T {
        return binarySearchWithFallback(time, defaultControlPoint)
    }

    protected fun binarySearchWithFallback(time: Double, fallback: T): T {
        val controlPoint = binarySearch(time)

        return controlPoint ?: fallback
    }

    protected fun binarySearch(time: Double): T? {
        if (controlPoints.isEmpty() || time < controlPoints[0].time) {
            return null
        }

        if (time >= controlPoints[controlPoints.size - 1].time) {
            return controlPoints[controlPoints.size - 1]
        }

        var l = 0
        var r = controlPoints.size - 2

        while (l <= r) {
            val pivot = l + ((r - l) shr 1)
            val controlPoint = controlPoints[pivot]

            if (controlPoint.time < time) {
                l = pivot + 1
            } else if (controlPoint.time > time) {
                r = pivot - 1
            } else {
                return controlPoint
            }
        }

        return controlPoints[l - 1]
    }

    private fun findInsertionIndex(time: Double): Int {
        if (controlPoints.isEmpty() || time < controlPoints[0].time) {
            return 0
        }

        if (time >= controlPoints[controlPoints.size - 1].time) {
            return controlPoints.size
        }

        var l = 0
        var r = controlPoints.size - 2

        while (l <= r) {
            val pivot = l + ((r - l) shr 1)
            val controlPoint = controlPoints[pivot]

            if (controlPoint.time < time) {
                l = pivot + 1
            } else if (controlPoint.time > time) {
                r = pivot - 1
            } else {
                return pivot
            }
        }

        return l
    }
}
