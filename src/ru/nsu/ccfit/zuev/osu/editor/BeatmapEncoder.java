package ru.nsu.ccfit.zuev.osu.editor;

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.BeatmapHitObjectsManager;
import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPathType;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
import com.rian.difficultycalculator.math.Vector2;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.ComboColor;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.HitObjectType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Serializes BeatmapData back to .osu file format.
 */
public class BeatmapEncoder {

    /**
     * Encodes a BeatmapData to .osu format and writes it to the given file.
     *
     * @param beatmap The beatmap data to encode.
     * @param file    The file to write to.
     * @return Whether the encoding was successful.
     */
    public static boolean encode(BeatmapData beatmap, File file) {
        return encode(beatmap, file, null);
    }

    public static boolean encode(BeatmapData beatmap, File file, java.util.HashMap<Double, Boolean> kiaiFlags) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            writer.write(encodeToString(beatmap, kiaiFlags));
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Encodes a BeatmapData to .osu format string.
     *
     * @param beatmap The beatmap data to encode.
     * @return The .osu file content as a string.
     */
    public static String encodeToString(BeatmapData beatmap) {
        return encodeToString(beatmap, null);
    }

    public static String encodeToString(BeatmapData beatmap, java.util.HashMap<Double, Boolean> kiaiFlags) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("osu file format v").append(beatmap.getFormatVersion()).append("\n");

        // General section
        sb.append("\n[General]\n");
        sb.append("AudioFilename: ").append(beatmap.general.audioFilename).append("\n");
        sb.append("AudioLeadIn: ").append(beatmap.general.audioLeadIn).append("\n");
        sb.append("PreviewTime: ").append(beatmap.general.previewTime).append("\n");
        sb.append("Countdown: ").append(beatmap.general.countdown.ordinal()).append("\n");
        sb.append("SampleSet: ").append(beatmap.general.sampleBank.name()).append("\n");
        sb.append("StackLeniency: ").append(beatmap.general.stackLeniency).append("\n");
        sb.append("Mode: ").append(beatmap.general.mode).append("\n");
        sb.append("LetterboxInBreaks: ").append(boolToInt(beatmap.general.letterboxInBreaks)).append("\n");

        // Metadata section
        sb.append("\n[Metadata]\n");
        sb.append("Title:").append(beatmap.metadata.title).append("\n");
        sb.append("TitleUnicode:").append(beatmap.metadata.titleUnicode).append("\n");
        sb.append("Artist:").append(beatmap.metadata.artist).append("\n");
        sb.append("ArtistUnicode:").append(beatmap.metadata.artistUnicode).append("\n");
        sb.append("Creator:").append(beatmap.metadata.creator).append("\n");
        sb.append("Version:").append(beatmap.metadata.version).append("\n");
        sb.append("Source:").append(beatmap.metadata.source).append("\n");
        sb.append("Tags:").append(beatmap.metadata.tags).append("\n");
        sb.append("BeatmapID:").append(beatmap.metadata.beatmapID).append("\n");
        sb.append("BeatmapSetID:").append(beatmap.metadata.beatmapSetID).append("\n");

        // Difficulty section
        sb.append("\n[Difficulty]\n");
        sb.append("HPDrainRate:").append(formatDouble(beatmap.difficulty.hp)).append("\n");
        sb.append("CircleSize:").append(formatDouble(beatmap.difficulty.cs)).append("\n");
        sb.append("OverallDifficulty:").append(formatDouble(beatmap.difficulty.od)).append("\n");
        sb.append("ApproachRate:").append(formatDouble(beatmap.difficulty.ar)).append("\n");
        sb.append("SliderMultiplier:").append(formatDouble(beatmap.difficulty.sliderMultiplier)).append("\n");
        sb.append("SliderTickRate:").append(formatDouble(beatmap.difficulty.sliderTickRate)).append("\n");

        // Events section
        sb.append("\n[Events]\n");
        if (beatmap.events.backgroundFilename != null && !beatmap.events.backgroundFilename.isEmpty()) {
            sb.append("0,0,\"").append(beatmap.events.backgroundFilename).append("\",0,0,")
                    .append(beatmap.events.backgroundColor != null ?
                            String.format(Locale.US, "%d,%d,%d",
                                    (int) (beatmap.events.backgroundColor.r() * 255),
                                    (int) (beatmap.events.backgroundColor.g() * 255),
                                    (int) (beatmap.events.backgroundColor.b() * 255))
                            : "0,0,0")
                    .append("\n");
        }

        // Timing Points section
        sb.append("\n[TimingPoints]\n");
        encodeTimingPoints(sb, beatmap, kiaiFlags);

        // Colors section
        sb.append("\n[Colours]\n");
        encodeColors(sb, beatmap);

        // Hit Objects section
        sb.append("\n[HitObjects]\n");
        encodeHitObjects(sb, beatmap);

        return sb.toString();
    }

    private static void encodeTimingPoints(StringBuilder sb, BeatmapData beatmap, java.util.HashMap<Double, Boolean> kiaiFlags) {
        List<TimingControlPoint> timingPoints = beatmap.timingPoints.timing.getControlPoints();
        List<DifficultyControlPoint> difficultyPoints = beatmap.timingPoints.difficulty.getControlPoints();

        for (TimingControlPoint tp : timingPoints) {
            int effects = 0;
            if (kiaiFlags != null && Boolean.TRUE.equals(kiaiFlags.get(tp.time))) {
                effects |= 1; // kiai time bit
            }
            sb.append(formatDouble(tp.time)).append(",")
                    .append(formatDouble(tp.msPerBeat)).append(",")
                    .append(tp.timeSignature).append(",")
                    .append(beatmap.general.sampleVolume).append(",")
                    .append("1").append(",")
                    .append("0").append(",")
                    .append("1").append(",")
                    .append(effects).append("\n");
        }

        for (DifficultyControlPoint dp : difficultyPoints) {
            // Skip the default difficulty point at time 0
            if (dp.time == 0 && dp.speedMultiplier == 1 && dp.generateTicks) {
                continue;
            }
            double msPerBeat = dp.speedMultiplier != 0 ? 100.0 / dp.speedMultiplier : 100;
            sb.append(formatDouble(dp.time)).append(",")
                    .append(formatDouble(-msPerBeat)).append(",")
                    .append("4").append(",")
                    .append(beatmap.general.sampleVolume).append(",")
                    .append("0").append(",")
                    .append("0").append(",")
                    .append("0").append("\n");
        }
    }

    private static void encodeColors(StringBuilder sb, BeatmapData beatmap) {
        ArrayList<ComboColor> colors = beatmap.colors.comboColors;
        for (int i = 0; i < colors.size(); i++) {
            ComboColor color = colors.get(i);
            sb.append("Combo").append(i + 1).append(" : ")
                    .append((int) (color.r() * 255)).append(",")
                    .append((int) (color.g() * 255)).append(",")
                    .append((int) (color.b() * 255)).append("\n");
        }

        if (beatmap.colors.sliderBorderColor != null) {
            RGBColor bc = beatmap.colors.sliderBorderColor;
            sb.append("SliderBorder : ")
                    .append((int) (bc.r() * 255)).append(",")
                    .append((int) (bc.g() * 255)).append(",")
                    .append((int) (bc.b() * 255)).append("\n");
        }
    }

    private static void encodeHitObjects(StringBuilder sb, BeatmapData beatmap) {
        List<HitObject> objects = beatmap.hitObjects.getObjects();

        for (HitObject obj : objects) {
            if (obj instanceof HitCircle) {
                encodeHitCircle(sb, (HitCircle) obj);
            } else if (obj instanceof Slider) {
                encodeSlider(sb, (Slider) obj);
            } else if (obj instanceof Spinner) {
                encodeSpinner(sb, (Spinner) obj);
            }
        }
    }

    private static void encodeHitCircle(StringBuilder sb, HitCircle circle) {
        Vector2 pos = circle.getPosition();
        sb.append(Math.round(pos.x)).append(",")
                .append(Math.round(pos.y)).append(",")
                .append(Math.round(circle.getStartTime())).append(",")
                .append("1").append(",")    // type: normal hit circle
                .append("1").append(",")    // hitSound: normal
                .append("0").append(",")    // objectParams: none
                .append("0:0:0,0,0,0,0");  // hitSample
        sb.append("\n");
    }

    private static void encodeSlider(StringBuilder sb, Slider slider) {
        Vector2 pos = slider.getPosition();
        SliderPath path = slider.getPath();

        StringBuilder curveBuilder = new StringBuilder();
        SliderPathType pathType = path.pathType;

        // Path type character
        switch (pathType) {
            case Catmull:
                curveBuilder.append("C");
                break;
            case Linear:
                curveBuilder.append("L");
                break;
            case PerfectCurve:
                curveBuilder.append("P");
                break;
            case Bezier:
            default:
                curveBuilder.append("B");
                break;
        }

        // Control points (skip the first one as it's the position)
        ArrayList<Vector2> controlPoints = path.controlPoints;
        for (int i = 1; i < controlPoints.size(); i++) {
            Vector2 cp = controlPoints.get(i);
            curveBuilder.append("|")
                    .append(Math.round(cp.x)).append(":")
                    .append(Math.round(cp.y));
        }

        sb.append(Math.round(pos.x)).append(",")
                .append(Math.round(pos.y)).append(",")
                .append(Math.round(slider.getStartTime())).append(",")
                .append("2").append(",")    // type: slider
                .append("1").append(",")    // hitSound: normal
                .append(curveBuilder.toString()).append(",")
                .append(slider.getRepeatCount()).append(",")
                .append(formatDouble(path.expectedDistance)).append(",")
                .append("0").append(",")    // edgeSounds
                .append("0").append(",")    // edgeAddition
                .append("0:0:0,0,0,0,0");  // hitSample
        sb.append("\n");
    }

    private static void encodeSpinner(StringBuilder sb, Spinner spinner) {
        sb.append("256,")                   // x (center)
                .append("192,")              // y (center)
                .append(Math.round(spinner.getStartTime())).append(",")
                .append("12").append(",")    // type: spinner
                .append("1").append(",")     // hitSound
                .append(Math.round(spinner.getEndTime())).append(",")
                .append("0").append(",")     // objectParams
                .append("0:0:0,0,0,0,0");   // hitSample
        sb.append("\n");
    }

    private static String formatDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static int boolToInt(boolean value) {
        return value ? 1 : 0;
    }
}
