package ru.nsu.ccfit.zuev.osu.beatmap

import com.rian.difficultycalculator.attributes.DifficultyAttributes
import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager
import com.rian.difficultycalculator.beatmap.BeatmapHitObjectsManager
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint
import ru.nsu.ccfit.zuev.osu.BeatmapInfo
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapColor
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapEvents
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapGeneral
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapMetadata
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File

class BeatmapData {
    var general: BeatmapGeneral = BeatmapGeneral()
    var metadata: BeatmapMetadata = BeatmapMetadata()
    var difficulty: BeatmapDifficulty = BeatmapDifficulty()
    var events: BeatmapEvents = BeatmapEvents()
    var colors: BeatmapColor = BeatmapColor()
    var rawTimingPoints: ArrayList<String> = ArrayList()
    var timingPoints: BeatmapControlPointsManager = BeatmapControlPointsManager()
    var rawHitObjects: ArrayList<String> = ArrayList()
    var hitObjects: BeatmapHitObjectsManager = BeatmapHitObjectsManager()

    var folder: String? = null
        internal set
    var filename: String = ""
        internal set
    var md5: String? = null
        internal set
    var formatVersion: Int = 14
        internal set

    @JvmField
    var isCalculator: Boolean = false

    constructor()

    internal constructor(source: BeatmapData) {
        folder = source.folder
        filename = source.filename
        formatVersion = source.formatVersion
        md5 = source.md5

        general = source.general.deepClone()
        metadata = source.metadata.deepClone()
        difficulty = source.difficulty.deepClone()
        events = source.events.deepClone()
        colors = source.colors.deepClone()
        timingPoints = source.timingPoints.deepClone()
        hitObjects = source.hitObjects.deepClone()

        rawTimingPoints.addAll(source.rawTimingPoints)
        rawHitObjects.addAll(source.rawHitObjects)
    }

    fun deepClone(): BeatmapData = BeatmapData(this)

    fun getFolder(): String? = folder

    fun setFolder(path: String) {
        folder = path
    }

    fun getFilename(): String = filename

    fun setFilename(filename: String) {
        this.filename = filename
    }

    fun getMD5(): String? = md5

    fun setMD5(md5: String) {
        this.md5 = md5
    }

    fun getMaxCombo(): Int {
        var combo = 0

        for (object_ in hitObjects.objects) {
            ++combo

            if (object_ is Slider) {
                combo += object_.getNestedHitObjects().size - 1
            }
        }

        return combo
    }

    fun getFormatVersion(): Int = formatVersion

    fun setFormatVersion(formatVersion: Int) {
        this.formatVersion = formatVersion
    }

    fun getOffsetTime(time: Double): Double = time + if (formatVersion < 5) 24.0 else 0.0

    fun getOffsetTime(time: Int): Int = time + if (formatVersion < 5) 24 else 0

    fun getDuration(): Int {
        if (hitObjects.objects.isEmpty()) {
            return 0
        }

        val lastObject = hitObjects.objects[hitObjects.objects.size - 1]

        return if (lastObject is HitObjectWithDuration) {
            lastObject.endTime.toInt()
        } else {
            lastObject.startTime.toInt()
        }
    }

    @JvmOverloads
    fun populateMetadata(info: BeatmapInfo): Boolean {
        if (info.title == null) {
            info.title = metadata.title
        }
        if (info.titleUnicode == null) {
            val titleUnicode = metadata.titleUnicode
            if (titleUnicode.isNotEmpty()) {
                info.titleUnicode = titleUnicode
            }
        }
        if (info.artist == null) {
            info.artist = metadata.artist
        }
        if (info.artistUnicode == null) {
            val artistUnicode = metadata.artist
            if (artistUnicode.isNotEmpty()) {
                info.artistUnicode = artistUnicode
            }
        }
        if (info.source == null) {
            info.source = metadata.source
        }
        if (info.tags == null) {
            info.tags = metadata.tags
        }

        return true
    }

    @JvmOverloads
    fun populateMetadata(track: TrackInfo): Boolean {
        track.md5 = md5
        track.creator = metadata.creator
        track.mode = metadata.version
        track.publicName = "${metadata.artist} - ${metadata.title}"
        track.beatmapID = metadata.beatmapID
        track.beatmapSetID = metadata.beatmapSetID

        val musicFile = File(folder, general.audioFilename)
        if (!musicFile.exists()) {
            ToastLogger.showText(
                StringTable.format(
                    R.string.beatmap_parser_music_not_found,
                    filename.substring(0, Math.max(0, filename.length - 4))
                ), true
            )
            return false
        }

        track.audioFilename = musicFile.path
        track.previewTime = general.previewTime

        track.overallDifficulty = difficulty.od
        track.approachRate = difficulty.ar
        track.hpDrain = difficulty.hp
        track.circleSize = difficulty.cs

        track.background = "$folder/${events.backgroundFilename}"

        for (point in timingPoints.timing.controlPoints) {
            val bpm = point.getBPM().toFloat()

            track.bpmMin = if (track.bpmMin != Float.MAX_VALUE) Math.min(track.bpmMin, bpm) else bpm
            track.bpmMax = if (track.bpmMax != 0f) Math.max(track.bpmMax, bpm) else bpm
        }

        if (hitObjects.objects.isEmpty()) {
            return false
        }

        track.totalHitObjectCount = hitObjects.objects.size
        track.hitCircleCount = hitObjects.circleCount
        track.sliderCount = hitObjects.sliderCount
        track.spinnerCount = hitObjects.spinnerCount
        track.musicLength = getDuration()
        track.maxCombo = getMaxCombo()

        val attributes: DifficultyAttributes = BeatmapDifficultyCalculator.calculateDifficulty(this)
        track.difficulty = GameHelper.Round(attributes.starRating.toFloat(), 2)

        return true
    }
}
