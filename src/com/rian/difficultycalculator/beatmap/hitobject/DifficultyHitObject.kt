package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderRepeat
import com.rian.difficultycalculator.math.MathUtils
import com.rian.difficultycalculator.math.Vector2

class DifficultyHitObject {
    @JvmField
    val `object`: HitObject

    @JvmField
    var index: Int

    @JvmField
    var baseTimePreempt: Double

    @JvmField
    var timePreempt: Double

    @JvmField
    var timeFadeIn: Double

    @JvmField
    var aimStrainWithSliders: Double = 0.0

    @JvmField
    var aimStrainWithoutSliders: Double = 0.0

    @JvmField
    var speedStrain: Double = 0.0

    @JvmField
    var rhythmMultiplier: Double = 0.0

    @JvmField
    var flashlightStrain: Double = 0.0

    @JvmField
    var lazyJumpDistance: Double = 0.0

    @JvmField
    var minimumJumpDistance: Double = 0.0

    @JvmField
    var minimumJumpTime: Double = 0.0

    @JvmField
    var travelDistance: Double = 0.0

    @JvmField
    var travelTime: Double = 0.0

    @JvmField
    var angle: Double = Double.NaN

    @JvmField
    var deltaTime: Double = 0.0

    @JvmField
    var strainTime: Double = 0.0

    @JvmField
    var startTime: Double = 0.0

    @JvmField
    var endTime: Double = 0.0

    private val difficultyHitObjects: ArrayList<DifficultyHitObject>

    private val assumedSliderRadius: Float = normalizedRadius * 1.8f

    private val lastObject: HitObject
    private val lastLastObject: HitObject?

    constructor(
        obj: HitObject, lastObject: HitObject, lastLastObject: HitObject?,
        clockRate: Double, difficultyHitObjects: ArrayList<DifficultyHitObject>,
        index: Int, timePreempt: Double, isForceAR: Boolean
    ) {
        this.`object` = obj
        this.lastObject = lastObject
        this.lastLastObject = lastLastObject
        this.index = index
        this.difficultyHitObjects = difficultyHitObjects
        baseTimePreempt = timePreempt
        this.timePreempt = timePreempt

        timeFadeIn = 400 * Math.min(1.0, timePreempt / 450)

        if (!isForceAR) {
            this.timePreempt /= clockRate
        }

        deltaTime = (obj.startTime - lastObject.getStartTime()) / clockRate
        startTime = obj.startTime / clockRate

        if (obj is HitObjectWithDuration) {
            endTime = obj.endTime / clockRate
        } else {
            endTime = startTime
        }

        strainTime = Math.max(deltaTime, minDeltaTime.toDouble())

        setDistances(clockRate)
    }

    fun previous(backwardsIndex: Int): DifficultyHitObject? {
        return try {
            difficultyHitObjects[index - (backwardsIndex + 1)]
        } catch (ignored: IndexOutOfBoundsException) {
            null
        }
    }

    fun next(forwardsIndex: Int): DifficultyHitObject? {
        return try {
            difficultyHitObjects[index + forwardsIndex + 1]
        } catch (ignored: IndexOutOfBoundsException) {
            null
        }
    }

    fun opacityAt(time: Double, isHidden: Boolean): Double {
        if (time > `object`.startTime) {
            return 0.0
        }

        val fadeInStartTime = `object`.startTime - baseTimePreempt
        val fadeInDuration = if (isHidden) baseTimePreempt * 0.4 else timeFadeIn

        val nonHiddenOpacity = MathUtils.clamp((time - fadeInStartTime) / fadeInDuration, 0.0, 1.0)

        if (isHidden) {
            val fadeOutStartTime = fadeInStartTime + fadeInDuration
            val fadeOutDuration = baseTimePreempt * 0.3

            return Math.min(nonHiddenOpacity, 1 - MathUtils.clamp((time - fadeOutStartTime) / fadeOutDuration, 0.0, 1.0))
        }

        return nonHiddenOpacity
    }

    private fun setDistances(clockRate: Double) {
        if (`object` is Slider) {
            val slider = `object` as Slider
            computeSliderCursorPosition(slider)

            travelDistance = slider.lazyTravelDistance.toDouble()
            travelDistance *= Math.pow(1.0 + (slider.repeatCount - 1) / 2.5, 1.0 / 2.5)

            travelTime = Math.max(slider.lazyTravelTime / clockRate, minDeltaTime.toDouble())
        }

        if (`object` is Spinner || lastObject is Spinner) {
            return
        }

        val scalingFactor = getScalingFactor()
        val lastCursorPosition = getEndCursorPosition(lastObject)

        val lazyJumpVector = `object`.getStackedPosition()
            .scale(scalingFactor)
            .subtract(lastCursorPosition.scale(scalingFactor))
        lazyJumpDistance = lazyJumpVector.getLength().toDouble()
        minimumJumpTime = strainTime
        minimumJumpDistance = lazyJumpDistance

        if (lastObject is Slider) {
            minimumJumpTime = Math.max(strainTime - lastObject.lazyTravelTime / clockRate, minDeltaTime.toDouble())

            val tailJumpDistance = lastObject.tail
                .getStackedPosition()
                .subtract(`object`.getStackedPosition())
                .getLength().toDouble() * scalingFactor

            val maximumSliderRadius = normalizedRadius * 2.4f
            minimumJumpDistance = Math.max(
                0.0,
                Math.min(
                    lazyJumpDistance - (maximumSliderRadius - assumedSliderRadius),
                    tailJumpDistance - maximumSliderRadius
                )
            )
        }

        if (lastLastObject != null && lastLastObject !is Spinner) {
            val lastLastCursorPosition = getEndCursorPosition(lastLastObject)
            val v1 = lastLastCursorPosition.subtract(lastObject.getStackedPosition())
            val v2 = `object`.getStackedPosition().subtract(lastCursorPosition)
            val dot = v1.dot(v2)
            val det = v1.x * v2.y - v1.y * v2.x

            angle = Math.abs(Math.atan2(det.toDouble(), dot.toDouble()))
        }
    }

    private fun computeSliderCursorPosition(slider: Slider) {
        if (slider.lazyEndPosition != null) {
            return
        }

        slider.lazyTravelTime = slider.nestedHitObjects[slider.nestedHitObjects.size - 1].startTime - slider.startTime

        var endTimeMin = slider.lazyTravelTime / slider.spanDuration
        if (endTimeMin % 2 >= 1) {
            endTimeMin = 1 - endTimeMin % 1
        } else {
            endTimeMin %= 1.0
        }

        slider.lazyEndPosition = slider.getStackedPosition().add(slider.path.positionAt(endTimeMin))

        var currentCursorPosition = slider.getStackedPosition()
        val scalingFactor = normalizedRadius.toDouble() / slider.getRadius()

        for (i in 1 until slider.nestedHitObjects.size) {
            val currentMovementObject = slider.nestedHitObjects[i]

            var currentMovement = currentMovementObject
                .getStackedPosition()
                .subtract(currentCursorPosition)
            var currentMovementLength = scalingFactor * currentMovement.getLength()

            var requiredMovement = assumedSliderRadius.toDouble()

            if (i == slider.nestedHitObjects.size - 1) {
                val lazyMovement = slider.lazyEndPosition!!.subtract(currentCursorPosition)

                if (lazyMovement.getLength() < currentMovement.getLength()) {
                    currentMovement = lazyMovement
                }

                currentMovementLength = scalingFactor * currentMovement.getLength()
            } else if (currentMovementObject is SliderRepeat) {
                requiredMovement = normalizedRadius.toDouble()
            }

            if (currentMovementLength > requiredMovement) {
                currentCursorPosition = currentCursorPosition.add(
                    currentMovement.scale(((currentMovementLength - requiredMovement) / currentMovementLength).toFloat())
                )
                currentMovementLength *= (currentMovementLength - requiredMovement) / currentMovementLength
                slider.lazyTravelDistance += currentMovementLength.toFloat()
            }

            if (i == slider.nestedHitObjects.size - 1) {
                slider.lazyEndPosition = currentCursorPosition
            }
        }
    }

    private fun getScalingFactor(): Float {
        val radius = `object`.getRadius().toFloat()
        var scalingFactor = normalizedRadius / radius

        if (radius < 30) {
            scalingFactor *= 1 + Math.min(30 - radius, 5f) / 50
        }

        return scalingFactor
    }

    private fun getEndCursorPosition(obj: HitObject): Vector2 {
        var pos = obj.getStackedPosition()

        if (obj is Slider) {
            computeSliderCursorPosition(obj)
            pos = obj.lazyEndPosition ?: pos
        }

        return pos
    }

    companion object {
        private const val normalizedRadius = 50
        private const val minDeltaTime = 25
    }
}
