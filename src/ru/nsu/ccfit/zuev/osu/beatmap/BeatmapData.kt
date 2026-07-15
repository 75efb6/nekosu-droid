package ru.nsu.ccfit.zuev.osu.beatmap

import com.rian.difficultycalculator.attributes.DifficultyAttributes
import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager
import com.rian.difficultycalculator.beatmap.BeatmapHitObjectsManager
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration
import com.rian.difficultycalculator.beatmap.hitobject.Slider
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

class BeatmapData private constructor(
    val general: BeatmapGeneral,
    val metadata: BeatmapMetadata,
    val difficulty: BeatmapDifficulty,
    val events: BeatmapEvents,
    val colors: BeatmapColor,
    val rawTimingPoints: ArrayList<String>,
    val timingPoints: BeatmapControlPointsManager,
    val rawHitObjects: ArrayList<String>,
    val hitObjects: BeatmapHitObjectsManager,
) {
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

    constructor() : this(
        BeatmapGeneral(),
        BeatmapMetadata(),
        BeatmapDifficulty(),
        BeatmapEvents(),
        BeatmapColor(),
        ArrayList(),
        BeatmapControlPointsManager(),
        ArrayList(),
        BeatmapHitObjectsManager(),
    )

    internal constructor(source: BeatmapData) : this(
        source.general.deepClone(),
        source.metadata.deepClone(),
        source.difficulty.deepClone(),
        source.events.deepClone(),
        source.colors.deepClone(),
        ArrayList(source.rawTimingPoints),
        source.timingPoints.deepClone(),
        ArrayList(source.rawHitObjects),
        source.hitObjects.deepClone(),
    ) {
        folder = source.folder
        filename = source.filename
        formatVersion = source.formatVersion
        md5 = source.md5
    }

    fun deepClone(): BeatmapData = BeatmapData(this)

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
            val artistUnicode = metadata.artistUnicode
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

        track.background = if (events.backgroundFilename.isNullOrEmpty() || events.backgroundFilename == "null") null else "$folder/${events.backgroundFilename}"

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
        track.musicLength = getDuration().toLong()
        track.maxCombo = getMaxCombo()

        val attributes: DifficultyAttributes = BeatmapDifficultyCalculator.calculateDifficulty(this)
        track.difficulty = GameHelper.Round(attributes.starRating.toFloat(), 2).toFloat()

        return true
    }
}
