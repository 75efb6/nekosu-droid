package ru.nsu.ccfit.zuev.osu.editor

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint
import com.rian.difficultycalculator.math.Vector2
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

object BeatmapEncoder {

    @JvmStatic
    fun encode(beatmap: BeatmapData, file: File): Boolean {
        return encode(beatmap, file, null)
    }

    @JvmStatic
    fun encode(beatmap: BeatmapData, file: File, kiaiFlags: HashMap<Double, Boolean>?): Boolean {
        return try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                writer.write(encodeToString(beatmap, kiaiFlags))
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun encodeToString(beatmap: BeatmapData): String {
        return encodeToString(beatmap, null)
    }

    @JvmStatic
    fun encodeToString(beatmap: BeatmapData, kiaiFlags: HashMap<Double, Boolean>?): String {
        val sb = StringBuilder()

        sb.append("osu file format v").append(beatmap.formatVersion).append("\n")

        sb.append("\n[General]\n")
        sb.append("AudioFilename: ").append(beatmap.general.audioFilename).append("\n")
        sb.append("AudioLeadIn: ").append(beatmap.general.audioLeadIn).append("\n")
        sb.append("PreviewTime: ").append(beatmap.general.previewTime).append("\n")
        sb.append("Countdown: ").append(beatmap.general.countdown.ordinal).append("\n")
        sb.append("SampleSet: ").append(beatmap.general.sampleBank.name).append("\n")
        sb.append("StackLeniency: ").append(beatmap.general.stackLeniency).append("\n")
        sb.append("Mode: ").append(beatmap.general.mode).append("\n")
        sb.append("LetterboxInBreaks: ").append(boolToInt(beatmap.general.letterboxInBreaks)).append("\n")

        sb.append("\n[Metadata]\n")
        sb.append("Title:").append(beatmap.metadata.title).append("\n")
        sb.append("TitleUnicode:").append(beatmap.metadata.titleUnicode).append("\n")
        sb.append("Artist:").append(beatmap.metadata.artist).append("\n")
        sb.append("ArtistUnicode:").append(beatmap.metadata.artistUnicode).append("\n")
        sb.append("Creator:").append(beatmap.metadata.creator).append("\n")
        sb.append("Version:").append(beatmap.metadata.version).append("\n")
        sb.append("Source:").append(beatmap.metadata.source).append("\n")
        sb.append("Tags:").append(beatmap.metadata.tags).append("\n")
        sb.append("BeatmapID:").append(beatmap.metadata.beatmapID).append("\n")
        sb.append("BeatmapSetID:").append(beatmap.metadata.beatmapSetID).append("\n")

        sb.append("\n[Difficulty]\n")
        sb.append("HPDrainRate:").append(formatDouble(beatmap.difficulty.hp.toDouble())).append("\n")
        sb.append("CircleSize:").append(formatDouble(beatmap.difficulty.cs.toDouble())).append("\n")
        sb.append("OverallDifficulty:").append(formatDouble(beatmap.difficulty.od.toDouble())).append("\n")
        sb.append("ApproachRate:").append(formatDouble(beatmap.difficulty.ar.toDouble())).append("\n")
        sb.append("SliderMultiplier:").append(formatDouble(beatmap.difficulty.sliderMultiplier)).append("\n")
        sb.append("SliderTickRate:").append(formatDouble(beatmap.difficulty.sliderTickRate)).append("\n")

        sb.append("\n[Events]\n")
        if (!beatmap.events.backgroundFilename.isNullOrEmpty()) {
            sb.append("0,0,\"").append(beatmap.events.backgroundFilename).append("\",0,0,")
                .append(
                    beatmap.events.backgroundColor?.let {
                        String.format(
                            Locale.US, "%d,%d,%d",
                            (it.r() * 255).toInt(),
                            (it.g() * 255).toInt(),
                            (it.b() * 255).toInt()
                        )
                    } ?: "0,0,0"
                )
                .append("\n")
        }

        sb.append("\n[TimingPoints]\n")
        encodeTimingPoints(sb, beatmap, kiaiFlags)

        sb.append("\n[Colours]\n")
        encodeColors(sb, beatmap)

        sb.append("\n[HitObjects]\n")
        encodeHitObjects(sb, beatmap)

        return sb.toString()
    }

    private fun encodeTimingPoints(sb: StringBuilder, beatmap: BeatmapData, kiaiFlags: HashMap<Double, Boolean>?) {
        val timingPoints = beatmap.timingPoints.timing.controlPoints
        val difficultyPoints = beatmap.timingPoints.difficulty.controlPoints

        for (tp in timingPoints) {
            var effects = 0
            if (kiaiFlags != null && java.lang.Boolean.TRUE == kiaiFlags[tp.time]) {
                effects = effects or 1
            }
            sb.append(formatDouble(tp.time)).append(",")
                .append(formatDouble(tp.msPerBeat)).append(",")
                .append(tp.timeSignature).append(",")
                .append(beatmap.general.sampleVolume).append(",")
                .append("1").append(",")
                .append("0").append(",")
                .append("1").append(",")
                .append(effects).append("\n")
        }

        for (dp in difficultyPoints) {
            if (dp.time == 0.0 && dp.speedMultiplier == 1.0 && dp.generateTicks) {
                continue
            }
            val msPerBeat = if (dp.speedMultiplier != 0.0) 100.0 / dp.speedMultiplier else 100.0
            sb.append(formatDouble(dp.time)).append(",")
                .append(formatDouble(-msPerBeat)).append(",")
                .append("4").append(",")
                .append(beatmap.general.sampleVolume).append(",")
                .append("0").append(",")
                .append("0").append(",")
                .append("0").append("\n")
        }
    }

    private fun encodeColors(sb: StringBuilder, beatmap: BeatmapData) {
        val colors = beatmap.colors.comboColors
        for (i in colors.indices) {
            val color = colors[i]
            sb.append("Combo").append(i + 1).append(" : ")
                .append((color.r() * 255).toInt()).append(",")
                .append((color.g() * 255).toInt()).append(",")
                .append((color.b() * 255).toInt()).append("\n")
        }

        if (beatmap.colors.sliderBorderColor != null) {
            val bc = beatmap.colors.sliderBorderColor
            sb.append("SliderBorder : ")
                .append((bc!!.r() * 255).toInt()).append(",")
                .append((bc.g() * 255).toInt()).append(",")
                .append((bc.b() * 255).toInt()).append("\n")
        }
    }

    private fun encodeHitObjects(sb: StringBuilder, beatmap: BeatmapData) {
        val objects = beatmap.hitObjects.objects

        for (obj in objects) {
            when (obj) {
                is HitCircle -> encodeHitCircle(sb, obj)
                is Slider -> encodeSlider(sb, obj)
                is Spinner -> encodeSpinner(sb, obj)
            }
        }
    }

    private fun encodeHitCircle(sb: StringBuilder, circle: HitCircle) {
        val pos = circle.position
        sb.append(Math.round(pos.x)).append(",")
            .append(Math.round(pos.y)).append(",")
            .append(Math.round(circle.startTime)).append(",")
            .append("1").append(",")
            .append("1").append(",")
            .append("0").append(",")
            .append("0:0:0,0,0,0,0")
        sb.append("\n")
    }

    private fun encodeSlider(sb: StringBuilder, slider: Slider) {
        val pos = slider.position
        val path = slider.path

        val curveBuilder = StringBuilder()
        val pathType = path.pathType

        when (pathType) {
            com.rian.difficultycalculator.beatmap.hitobject.SliderPathType.Catmull -> curveBuilder.append("C")
            com.rian.difficultycalculator.beatmap.hitobject.SliderPathType.Linear -> curveBuilder.append("L")
            com.rian.difficultycalculator.beatmap.hitobject.SliderPathType.PerfectCurve -> curveBuilder.append("P")
            com.rian.difficultycalculator.beatmap.hitobject.SliderPathType.Bezier -> curveBuilder.append("B")
        }

        val controlPoints = path.controlPoints
        for (i in 1 until controlPoints.size) {
            val cp = controlPoints[i]
            curveBuilder.append("|")
                .append(Math.round(cp.x)).append(":")
                .append(Math.round(cp.y))
        }

        sb.append(Math.round(pos.x)).append(",")
            .append(Math.round(pos.y)).append(",")
            .append(Math.round(slider.startTime)).append(",")
            .append("2").append(",")
            .append("1").append(",")
            .append(curveBuilder.toString()).append(",")
            .append(slider.repeatCount).append(",")
            .append(formatDouble(path.expectedDistance)).append(",")
            .append("0").append(",")
            .append("0").append(",")
            .append("0:0:0,0,0,0,0")
        sb.append("\n")
    }

    private fun encodeSpinner(sb: StringBuilder, spinner: Spinner) {
        sb.append("256,")
            .append("192,")
            .append(Math.round(spinner.startTime)).append(",")
            .append("12").append(",")
            .append("1").append(",")
            .append(Math.round(spinner.endTime)).append(",")
            .append("0").append(",")
            .append("0:0:0,0,0,0,0")
        sb.append("\n")
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private fun boolToInt(value: Boolean): Int {
        return if (value) 1 else 0
    }
}
