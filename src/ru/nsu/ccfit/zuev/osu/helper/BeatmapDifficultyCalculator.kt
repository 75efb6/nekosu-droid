package ru.nsu.ccfit.zuev.osu.helper

import com.rian.difficultycalculator.attributes.DifficultyAttributes
import com.rian.difficultycalculator.attributes.PerformanceAttributes
import com.rian.difficultycalculator.attributes.TimedDifficultyAttributes
import com.rian.difficultycalculator.beatmap.BeatmapDifficultyManager
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap
import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters
import com.rian.difficultycalculator.calculator.DifficultyCalculator
import com.rian.difficultycalculator.calculator.PerformanceCalculationParameters
import com.rian.difficultycalculator.calculator.PerformanceCalculator
import com.rian.difficultycalculator.utils.LRUCache
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.util.*

object BeatmapDifficultyCalculator {

    private val difficultyCalculator = DifficultyCalculator()

    private val difficultyCacheManager = LRUCache<String, BeatmapDifficultyCacheManager>(10)

    @JvmStatic
    fun constructDifficultyParameters(stat: StatisticV2?): DifficultyCalculationParameters? {
        if (stat == null) return null
        val parameters = DifficultyCalculationParameters()
        parameters.mods = stat.mod.clone()
        parameters.customSpeedMultiplier = stat.changeSpeed
        if (stat.isCustomCS()) parameters.customCS = stat.customCS!!
        if (stat.isCustomAR()) parameters.customAR = stat.customAR!!
        if (stat.isCustomOD()) parameters.customOD = stat.customOD!!
        return parameters
    }

    @JvmStatic
    fun constructPerformanceParameters(stat: StatisticV2?): PerformanceCalculationParameters? {
        if (stat == null) return null
        val parameters = PerformanceCalculationParameters()
        parameters.maxCombo = stat.maxCombo
        parameters.countGreat = stat.hit300
        parameters.countOk = stat.hit100
        parameters.countMeh = stat.hit50
        parameters.countMiss = stat.misses
        return parameters
    }

    @JvmStatic
    fun calculateDifficulty(beatmap: BeatmapData): DifficultyAttributes =
        calculateDifficulty(beatmap, null as DifficultyCalculationParameters?)

    @JvmStatic
    fun calculateDifficulty(beatmap: BeatmapData, stat: StatisticV2): DifficultyAttributes =
        calculateDifficulty(beatmap, constructDifficultyParameters(stat))

    @JvmStatic
    fun calculateDifficulty(beatmap: BeatmapData, parameters: DifficultyCalculationParameters?): DifficultyAttributes {
        val cacheManager = difficultyCacheManager[beatmap.md5!!]
        if (cacheManager != null) {
            val attributes = cacheManager.getDifficultyCache(parameters)
            if (attributes != null) return attributes
        }
        val attributes = difficultyCalculator.calculate(constructDifficultyBeatmap(beatmap), parameters)
        addCache(beatmap, parameters, attributes)
        return attributes
    }

    @JvmStatic
    fun calculateTimedDifficulty(beatmap: BeatmapData): List<TimedDifficultyAttributes> =
        calculateTimedDifficulty(beatmap, null as DifficultyCalculationParameters?)

    @JvmStatic
    fun calculateTimedDifficulty(beatmap: BeatmapData, stat: StatisticV2): List<TimedDifficultyAttributes> =
        calculateTimedDifficulty(beatmap, constructDifficultyParameters(stat))

    @JvmStatic
    fun calculateTimedDifficulty(beatmap: BeatmapData, parameters: DifficultyCalculationParameters?): List<TimedDifficultyAttributes> {
        val cacheManager = difficultyCacheManager[beatmap.md5!!]
        if (cacheManager != null) {
            val attributes = cacheManager.getTimedDifficultyCache(parameters)
            if (attributes != null) return attributes
        }
        val attributes = difficultyCalculator.calculateTimed(constructDifficultyBeatmap(beatmap), parameters)
        addCache(beatmap, parameters, attributes)
        return attributes
    }

    @JvmStatic
    fun calculatePerformance(attributes: DifficultyAttributes): PerformanceAttributes =
        calculatePerformance(attributes, null as PerformanceCalculationParameters?)

    @JvmStatic
    fun calculatePerformance(attributes: DifficultyAttributes, stat: StatisticV2): PerformanceAttributes =
        calculatePerformance(attributes, constructPerformanceParameters(stat))

    @JvmStatic
    fun calculatePerformance(attributes: DifficultyAttributes, parameters: PerformanceCalculationParameters?): PerformanceAttributes =
        PerformanceCalculator(attributes).calculate(parameters ?: PerformanceCalculationParameters())

    @JvmStatic
    fun invalidateExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val iterator = difficultyCacheManager.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next().value
            entry.invalidateExpiredCache(currentTime)
            if (entry.isEmpty) iterator.remove()
        }
    }

    private fun constructDifficultyBeatmap(data: BeatmapData): DifficultyBeatmap {
        val difficultyManager = BeatmapDifficultyManager()
        difficultyManager.setCS(data.difficulty.cs)
        difficultyManager.setAR(data.difficulty.ar)
        difficultyManager.setOD(data.difficulty.od)
        difficultyManager.setHP(data.difficulty.hp)
        difficultyManager.setSliderMultiplier(data.difficulty.sliderMultiplier)
        difficultyManager.setSliderTickRate(data.difficulty.sliderTickRate)
        val beatmap = DifficultyBeatmap(difficultyManager, data.hitObjects)
        beatmap.setFormatVersion(data.formatVersion)
        beatmap.setStackLeniency(data.general.stackLeniency)
        return beatmap
    }

    private fun addCache(beatmap: BeatmapData, parameters: DifficultyCalculationParameters?, attributes: DifficultyAttributes) {
        val md5 = beatmap.md5!!
        var cacheManager = difficultyCacheManager[md5]
        if (cacheManager == null) {
            cacheManager = BeatmapDifficultyCacheManager()
            difficultyCacheManager.put(md5, cacheManager)
        }
        cacheManager.addCache(parameters, attributes, 60 * 1000L)
    }

    private fun addCache(beatmap: BeatmapData, parameters: DifficultyCalculationParameters?, attributes: List<TimedDifficultyAttributes>) {
        val md5 = beatmap.md5!!
        var cacheManager = difficultyCacheManager[md5]
        if (cacheManager == null) {
            cacheManager = BeatmapDifficultyCacheManager()
            difficultyCacheManager.put(md5, cacheManager)
        }
        cacheManager.addCache(parameters, attributes, minOf(beatmap.getDuration().toLong(), 5 * 60 * 1000L))
    }

    private class BeatmapDifficultyCacheManager {
        private val attributeCache = LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<DifficultyAttributes>>(5)
        private val timedAttributeCache = LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<List<TimedDifficultyAttributes>>>(3)

        fun addCache(parameters: DifficultyCalculationParameters?, attributes: DifficultyAttributes, timeToLive: Long) {
            addCache(parameters, attributes, attributeCache, timeToLive)
        }

        fun addCache(parameters: DifficultyCalculationParameters?, attributes: List<TimedDifficultyAttributes>, timeToLive: Long) {
            addCache(parameters, attributes, timedAttributeCache, timeToLive)
        }

        fun getDifficultyCache(parameters: DifficultyCalculationParameters?): DifficultyAttributes? = getCache(parameters, attributeCache)

        fun getTimedDifficultyCache(parameters: DifficultyCalculationParameters?): List<TimedDifficultyAttributes>? = getCache(parameters, timedAttributeCache)

        val isEmpty: Boolean get() = attributeCache.isEmpty() && timedAttributeCache.isEmpty()

        fun invalidateExpiredCache(currentTime: Long) {
            invalidateExpiredCache(currentTime, attributeCache)
            invalidateExpiredCache(currentTime, timedAttributeCache)
        }

        private fun <T> invalidateExpiredCache(currentTime: Long, cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>) {
            val iterator = cacheMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next().value
                if (entry.isExpired(currentTime)) {
                    iterator.remove()
                }
            }
        }

        private fun <T> addCache(parameters: DifficultyCalculationParameters?, cache: T, cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>, timeToLive: Long) {
            var params = parameters
            if (params != null) {
                params = params.copy()
                params.mods.retainAll(difficultyCalculator.difficultyAdjustmentMods)
            } else {
                params = DifficultyCalculationParameters()
            }
            cacheMap[params] = BeatmapDifficultyCache(cache, timeToLive)
        }

        private fun <T> getCache(parameters: DifficultyCalculationParameters?, cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>): T? {
            var params = parameters
            if (params != null) {
                params = params.copy()
                params.mods.retainAll(difficultyCalculator.difficultyAdjustmentMods)
            } else {
                params = DifficultyCalculationParameters()
            }
            for (cache in cacheMap.entries) {
                if (cache.key == params) {
                    return cache.value.cache
                }
            }
            return null
        }
    }

    private class BeatmapDifficultyCache<T>(val cache: T, val timeToLive: Long) {
        val generatedTime: Long = System.currentTimeMillis()

        fun isExpired(time: Long): Boolean = generatedTime + timeToLive < time
    }
}
