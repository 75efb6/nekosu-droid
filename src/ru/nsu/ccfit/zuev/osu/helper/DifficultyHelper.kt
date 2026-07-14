package ru.nsu.ccfit.zuev.osu.helper

abstract class DifficultyHelper {
    abstract fun hitWindowFor300(od: Float): Float
    abstract fun hitWindowFor100(od: Float): Float
    abstract fun hitWindowFor50(od: Float): Float

    companion object {
        @JvmField
        val StdDifficulty: DifficultyHelper = object : DifficultyHelper() {
            override fun hitWindowFor300(od: Float): Float = (75 + 25 * (5 - od) / 5) / 1000
            override fun hitWindowFor100(od: Float): Float = (150 + 50 * (5 - od) / 5) / 1000
            override fun hitWindowFor50(od: Float): Float = (250 + 50 * (5 - od) / 5) / 1000f
        }

        @JvmField
        var HighDifficulty: DifficultyHelper = object : DifficultyHelper() {
            override fun hitWindowFor300(od: Float): Float = (55 + 30 * (5 - od) / 5) / 1000f
            override fun hitWindowFor100(od: Float): Float = (120 + 40 * (5 - od) / 5) / 1000f
            override fun hitWindowFor50(od: Float): Float = (180 + 50 * (5 - od) / 5) / 1000f
        }
    }
}
