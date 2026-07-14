package com.rian.difficultycalculator.attributes

class TimedDifficultyAttributes @JvmOverloads constructor(
    @JvmField val time: Double,
    @JvmField val attributes: DifficultyAttributes
) : Comparable<TimedDifficultyAttributes> {

    override fun compareTo(other: TimedDifficultyAttributes): Int {
        return time.compareTo(other.time)
    }
}
