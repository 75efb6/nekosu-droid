package com.rian.difficultycalculator.beatmap.hitobject

import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHead
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHitObject
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderRepeat
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderTail
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderTick
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint
import com.rian.difficultycalculator.math.MathUtils
import com.rian.difficultycalculator.math.Vector2
import java.util.Collections

class Slider : HitObjectWithDuration {
    @JvmField
    internal val repeatCount: Int

    @JvmField
    internal val path: SliderPath

    @JvmField
    internal val nestedHitObjects: ArrayList<SliderHitObject> = ArrayList()

    @JvmField
    internal val velocity: Double

    @JvmField
    internal val head: SliderHead

    @JvmField
    internal val tail: SliderTail

    @JvmField
    internal var lazyEndPosition: Vector2? = null

    @JvmField
    internal var lazyTravelDistance: Float = 0f

    @JvmField
    internal var lazyTravelTime: Double = 0.0

    @JvmField
    internal var spanDuration: Double = 0.0

    constructor(
        startTime: Double, x: Float, y: Float,
        timingControlPoint: TimingControlPoint,
        difficultyControlPoint: DifficultyControlPoint,
        repeatCount: Int, path: SliderPath,
        sliderVelocity: Double, tickRate: Double,
        tickDistanceMultiplier: Double, generateTicks: Boolean
    ) : this(
        startTime, Vector2(x, y), timingControlPoint, difficultyControlPoint,
        repeatCount, path, sliderVelocity, tickRate, tickDistanceMultiplier, generateTicks
    )

    constructor(
        startTime: Double, position: Vector2,
        timingControlPoint: TimingControlPoint,
        difficultyControlPoint: DifficultyControlPoint,
        repeatCount: Int, path: SliderPath,
        sliderVelocity: Double, tickRate: Double,
        tickDistanceMultiplier: Double, generateTicks: Boolean
    ) : super(startTime, startTime, position) {
        this.repeatCount = repeatCount
        this.path = path

        val scoringDistance = 100.0 * sliderVelocity * difficultyControlPoint.speedMultiplier
        velocity = scoringDistance / timingControlPoint.msPerBeat

        endTime = startTime + repeatCount * path.expectedDistance / velocity
        endPosition = position.add(path.positionAt((repeatCount % 2).toDouble()))

        spanDuration = getDuration() / repeatCount

        head = SliderHead(startTime, position)
        nestedHitObjects.add(head)

        val maxLength = 100000.0
        val length = Math.min(maxLength, path.expectedDistance)
        val tickDistance = MathUtils.clamp(scoringDistance / tickRate * tickDistanceMultiplier, 0.0, length)

        if (tickDistance != 0.0 && generateTicks) {
            val minDistanceFromEnd = velocity * 10

            for (span in 0 until repeatCount) {
                val spanStartTime = startTime + span * spanDuration
                val reversed = span % 2 == 1

                val sliderTicks = ArrayList<SliderTick>()

                var d = tickDistance
                while (d <= length) {
                    if (d >= length - minDistanceFromEnd) {
                        break
                    }

                    val distanceProgress = d / length
                    val timeProgress = if (reversed) 1 - distanceProgress else distanceProgress

                    val tickPosition = position.add(path.positionAt(distanceProgress))

                    sliderTicks.add(SliderTick(spanStartTime + timeProgress * spanDuration, tickPosition, span, spanStartTime))

                    d += tickDistance
                }

                if (reversed) {
                    sliderTicks.reverse()
                }

                nestedHitObjects.addAll(sliderTicks)

                if (span < repeatCount - 1) {
                    val repeatPosition = position.add(path.positionAt(((span + 1) % 2).toDouble()))
                    nestedHitObjects.add(SliderRepeat(spanStartTime + spanDuration, repeatPosition, span, spanStartTime))
                }
            }
        }

        val finalSpanIndex = repeatCount - 1
        val finalSpanStartTime = startTime + finalSpanIndex * spanDuration
        val finalSpanEndTime = Math.max(
            startTime + getDuration() / 2,
            finalSpanStartTime + spanDuration - legacyLastTickOffset
        )

        tail = SliderTail(finalSpanEndTime, endPosition, finalSpanIndex, finalSpanStartTime)
        nestedHitObjects.add(tail)
        nestedHitObjects.sortWith(compareBy { it.startTime })
    }

    private constructor(source: Slider) : super(source) {
        repeatCount = source.repeatCount
        endPosition = Vector2(source.endPosition.x, source.endPosition.y)
        path = source.path.deepClone()
        velocity = source.velocity
        head = source.head.deepClone()
        tail = source.tail.deepClone()
        lazyEndPosition = source.lazyEndPosition?.let { Vector2(it.x, it.y) }
        lazyTravelDistance = source.lazyTravelDistance
        lazyTravelTime = source.lazyTravelTime
        spanDuration = source.spanDuration

        nestedHitObjects.add(head)

        for (obj in source.nestedHitObjects.subList(1, source.nestedHitObjects.size - 1)) {
            nestedHitObjects.add(obj.deepClone() as SliderHitObject)
        }

        nestedHitObjects.add(tail)
        nestedHitObjects.sortWith(compareBy { it.startTime })
    }

    override fun deepClone(): Slider {
        return Slider(this)
    }

    fun getRepeatCount(): Int {
        return repeatCount
    }

    fun getPath(): SliderPath {
        return path
    }

    override fun setScale(scale: Float) {
        super.setScale(scale)

        for (obj in nestedHitObjects) {
            obj.setScale(scale)
        }
    }

    fun getNestedHitObjects(): List<SliderHitObject> {
        return Collections.unmodifiableList(nestedHitObjects)
    }

    fun getLazyEndPosition(): Vector2? {
        return lazyEndPosition
    }

    fun getLazyTravelDistance(): Double {
        return lazyTravelDistance.toDouble()
    }

    fun getLazyTravelTime(): Double {
        return lazyTravelTime
    }

    fun getVelocity(): Double {
        return velocity
    }

    fun getSpanDuration(): Double {
        return spanDuration
    }

    companion object {
        @JvmField
        val legacyLastTickOffset = 36
    }
}
