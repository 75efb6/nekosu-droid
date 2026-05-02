package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

/**
 * A parser for parsing beatmap sections that store properties in a key-value pair.
 */
public abstract class BeatmapKeyValueSectionParser extends BeatmapSectionParser {
    /**
     * Obtains the property of a line.
     * <br><br>
     * For example, <code>ApproachRate:9</code> will be split into <code>["ApproachRate", "9"]</code>.
     * <br><br>
     * Will return <code>null</code> for invalid lines.
     *
     * @param line The line.
     */
    protected String[] splitProperty(final String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return new String[] { line.trim(), "" };
        }
        return new String[] {
                line.substring(0, colon).trim(),
                line.substring(colon + 1).trim()
        };
    }
}
