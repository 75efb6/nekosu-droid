package ru.nsu.ccfit.zuev.osu.beatmap.parser

import android.util.Log
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.utils.HitObjectStackEvaluator
import okio.Okio
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapSection
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapColorParser
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapControlPointsParser
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapDifficultyParser
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapEventsParser
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapGeneralParser
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapHitObjectsParser
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapMetadataParser
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class BeatmapParser {
    private val file: File
    private var source: okio.BufferedSource? = null
    private var beatmapFormatVersion: Int = 14
    private var calculatorMode: Boolean = false

    @JvmField
    var isCalculator: Boolean = false

    constructor(file: File) {
        this.file = file
    }

    constructor(path: String) {
        file = File(path)
    }

    fun setCalculator(calculator: Boolean): BeatmapParser {
        calculatorMode = calculator
        return this
    }

    fun openFile(): Boolean {
        try {
            source = file.source().buffer()
        } catch (e: IOException) {
            Log.e("BeatmapParser.openFile", e.message ?: "Unknown error")
            source = null
            return false
        }

        try {
            val head = source?.readUtf8Line() ?: run {
                closeSource()
                return false
            }

            val matcher = FORMAT_PATTERN.matcher(head)
            if (!matcher.find()) {
                closeSource()
                return false
            }

            val formatPos = head.indexOf("file format v")
            beatmapFormatVersion = Utils.tryParseInt(head.substring(formatPos + 13), beatmapFormatVersion)
        } catch (e: Exception) {
            Log.e("BeatmapParser.openFile", e.message ?: "Unknown error")
        }

        return true
    }

    fun parse(withHitObjects: Boolean): BeatmapData? {
        val fileName = file.name.substring(0, file.name.length - 4)

        if (source == null && !openFile()) {
            ToastLogger.showText(
                StringTable.format(R.string.beatmap_parser_cannot_open_file, fileName),
                true
            )
            return null
        }

        var currentSection: BeatmapSection? = null
        val data = BeatmapData()
        data.isCalculator = calculatorMode

        data.setMD5(FileUtils.getMD5Checksum(file))
        data.setFolder(file.parent ?: "")
        data.setFilename(file.path)
        data.setFormatVersion(beatmapFormatVersion)

        try {
            var s: String?
            while (source?.readUtf8Line().also { s = it } != null) {
                s ?: break

                if (data.general.mode != 0) {
                    return null
                }

                if (s!!.startsWith(" ") || s!!.startsWith("_")) {
                    continue
                }

                val trimmed = s!!.trim()

                if (trimmed.startsWith("//") || trimmed.isEmpty()) {
                    continue
                }

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = BeatmapSection.parse(trimmed.substring(1, trimmed.length - 1))
                    continue
                }

                if (currentSection == null) {
                    continue
                }

                try {
                    when (currentSection) {
                        BeatmapSection.general -> generalParser.parse(data, trimmed)
                        BeatmapSection.metadata -> metadataParser.parse(data, trimmed)
                        BeatmapSection.difficulty -> difficultyParser.parse(data, trimmed)
                        BeatmapSection.events -> eventsParser.parse(data, trimmed)
                        BeatmapSection.timingPoints -> controlPointsParser.parse(data, trimmed)
                        BeatmapSection.colors -> colorParser.parse(data, trimmed)
                        BeatmapSection.hitObjects -> hitObjectsParser.parse(data, trimmed)
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e("BeatmapParser.parse", "Unable to parse line $trimmed", e)
                }
            }

            closeSource()

            if (withHitObjects) {
                populateObjectData(data)
            }
        } catch (e: IOException) {
            Log.e("BeatmapParser.parse", e.message ?: "Unknown error")
            return null
        }

        return data
    }

    companion object {
        private val FORMAT_PATTERN: Pattern = Pattern.compile("osu file format v(\\d+)")
        private val generalParser = BeatmapGeneralParser()
        private val metadataParser = BeatmapMetadataParser()
        private val difficultyParser = BeatmapDifficultyParser()
        private val eventsParser = BeatmapEventsParser()
        private val controlPointsParser = BeatmapControlPointsParser()
        private val colorParser = BeatmapColorParser()
        private val hitObjectsParser = BeatmapHitObjectsParser()

        @JvmStatic
        fun populateObjectData(data: BeatmapData) {
            val scale = (1 - 0.7f * (data.difficulty.cs - 5) / 5) / 2

            for (object_ in data.hitObjects.objects) {
                object_.setScale(scale)
                object_.setStackHeight(0)
            }

            HitObjectStackEvaluator.applyStacking(
                data.getFormatVersion(),
                data.hitObjects.objects,
                data.difficulty.ar,
                data.general.stackLeniency
            )
        }
    }

    private fun closeSource() {
        source?.let {
            try {
                it.close()
            } catch (_: IOException) {}
        }
        source = null
    }
}
