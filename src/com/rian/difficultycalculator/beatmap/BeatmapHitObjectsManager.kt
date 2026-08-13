package com.rian.difficultycalculator.beatmap

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import java.util.Collections

class BeatmapHitObjectsManager {
    internal val objects: ArrayList<HitObject> = ArrayList()

    internal var circleCount: Int = 0

    internal var sliderCount: Int = 0

    internal var spinnerCount: Int = 0

    constructor()

    private constructor(source: BeatmapHitObjectsManager) {
        circleCount = source.circleCount
        sliderCount = source.sliderCount
        spinnerCount = source.spinnerCount

        for (obj in source.objects) {
            objects.add(obj.deepClone()!!)
        }
    }

    fun add(objects: Iterable<HitObject>) {
        for (obj in objects) {
            add(obj)
        }
    }

    fun add(obj: HitObject) {
        objects.add(findInsertionIndex(obj.getStartTime()), obj)

        when (obj) {
            is HitCircle -> ++circleCount
            is Slider -> ++sliderCount
            else -> ++spinnerCount
        }
    }

    fun remove(obj: HitObject): Boolean {
        return objects.remove(obj)
    }

    fun remove(index: Int): HitObject? {
        if (index < 0 || index > objects.size - 1) {
            return null
        }

        val obj = objects.removeAt(index)

        when (obj) {
            is HitCircle -> --circleCount
            is Slider -> --sliderCount
            else -> --spinnerCount
        }

        return obj
    }

    fun clear() {
        objects.clear()
        circleCount = 0
        sliderCount = 0
        spinnerCount = 0
    }

    fun resetStacking() {
        for (obj in objects) {
            obj.setStackHeight(0)
        }
    }

    fun getObjects(): List<HitObject> {
        return Collections.unmodifiableList(objects)
    }

    fun deepClone(): BeatmapHitObjectsManager {
        return BeatmapHitObjectsManager(this)
    }

    fun getCircleCount(): Int {
        return circleCount
    }

    fun getSliderCount(): Int {
        return sliderCount
    }

    fun getSpinnerCount(): Int {
        return spinnerCount
    }

    private fun findInsertionIndex(startTime: Double): Int {
        if (objects.isEmpty() || startTime < objects[0].getStartTime()) {
            return 0
        }

        if (startTime >= objects[objects.size - 1].getStartTime()) {
            return objects.size
        }

        var l = 0
        var r = objects.size - 2

        while (l <= r) {
            val pivot = l + ((r - l) shr 1)
            val obj = objects[pivot]
            val objectStartTime = obj.getStartTime()

            if (objectStartTime < startTime) {
                l = pivot + 1
            } else if (objectStartTime > startTime) {
                r = pivot - 1
            } else {
                return pivot
            }
        }

        return l
    }
}
