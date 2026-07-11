package ru.nsu.ccfit.zuev.osu.online

import android.content.Context
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import java.io.File
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object SeasonalBackgroundManager {

    private const val REFRESH_INTERVAL_MINUTES = 5L
    private const val BG_COUNT = 6

    private val refreshExecutor = Executors.newSingleThreadScheduledExecutor()
    private var refreshTask: ScheduledFuture<*>? = null
    private var onRefresh: (() -> Unit)? = null

    private var currentSeason: String = ""
    private var currentBgIndex: Int = -1
    private var lastRefreshMinuteOfDay: Int = -1

    fun getSeasonName(): String {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        return when (month) {
            1 -> "newyear"
            2 -> "valentines"
            3, 4, 5 -> "spring"
            6, 7, 8 -> "summer"
            9 -> "autumn"
            10 -> "halloween"
            11 -> "autumn"
            12 -> "christmas"
            else -> "summer"
        }
    }

    fun getSeasonalBgUrl(season: String, index: Int): String {
        return "https://${OnlineManager.hostname}/seasonal/$season/$index.png"
    }

    fun getCacheDir(): File {
        val dir = File(Config.getCachePath(), "seasonal")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCacheFile(season: String, index: Int): File {
        return File(getCacheDir(), "${season}_${index}.png")
    }

    fun pickRandomBg(): Pair<String, Int> {
        val season = getSeasonName()
        val index = (Math.random() * BG_COUNT).toInt() + 1
        return Pair(season, index)
    }

    fun isSeasonalActive(): Boolean {
        return OnlineManager.getInstance().isStayOnline && Config.isSeasonalBg()
    }

    fun downloadAndCache(season: String, index: Int): File? {
        val file = getCacheFile(season, index)
        if (file.exists() && file.length() > 0) {
            return file
        }
        val url = getSeasonalBgUrl(season, index)
        return if (OnlineFileOperator.downloadFile(url, file.absolutePath)) {
            if (file.exists() && file.length() > 0) file else null
        } else null
    }

    fun shouldRefresh(): Boolean {
        val now = Calendar.getInstance()
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        if (lastRefreshMinuteOfDay == -1) return true
        return (minuteOfDay - lastRefreshMinuteOfDay) >= REFRESH_INTERVAL_MINUTES
    }

    fun startPeriodicRefresh(onRefreshCallback: Runnable) {
        onRefresh = { onRefreshCallback.run() }
        stopPeriodicRefresh()
        refreshTask = refreshExecutor.scheduleAtFixedRate({
            try {
                if (!isSeasonalActive()) return@scheduleAtFixedRate
                refreshSeasonalBg()
                onRefresh?.invoke()
            } catch (e: Exception) {
                Debug.e("SeasonalBackgroundManager refresh error: ${e.message}", e)
            }
        }, REFRESH_INTERVAL_MINUTES, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES)
    }

    fun stopPeriodicRefresh() {
        refreshTask?.cancel(false)
        refreshTask = null
    }

    fun refreshSeasonalBg() {
        if (!isSeasonalActive()) return
        val (season, index) = pickRandomBg()
        val file = downloadAndCache(season, index)
        if (file != null) {
            currentSeason = season
            currentBgIndex = index
            val now = Calendar.getInstance()
            lastRefreshMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            Debug.i("SeasonalBackgroundManager: refreshed to $season/$index")
        } else {
            Debug.e("SeasonalBackgroundManager: failed to download $season/$index")
        }
    }

    fun getCurrentCacheFile(): File? {
        if (currentSeason.isEmpty() || currentBgIndex < 0) return null
        val file = getCacheFile(currentSeason, currentBgIndex)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun initAndLoadFirst() {
        if (!isSeasonalActive()) return
        val (season, index) = pickRandomBg()
        currentSeason = season
        currentBgIndex = index
        downloadAndCache(season, index)
        val now = Calendar.getInstance()
        lastRefreshMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }
}
