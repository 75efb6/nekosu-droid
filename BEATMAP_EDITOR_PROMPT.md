# In-Game Beatmap Editor — Research Summary & Planning Prompt

## Goal
Build a full in-game beatmap editor with toolbar-based UI for the nekosu!droid osu! client. The editor should allow placing, moving, deleting hit objects, editing timing points, BPM, metadata, and saving back to .osu format.

## Architecture Summary

### Coordinate System
- **Track space**: osu! pixel coordinates (0-512 x 0-384), defined in `Constants.java`
- **Screen space**: virtual resolution (RES_WIDTH=1280, RES_HEIGHT=proportional)
- **Conversion**: `Utils.trackToRealCoords()` and `Utils.realToTrackCoords()` handle the bidirectional mapping
- **Playfield**: `MAP_ACTUAL_HEIGHT = RES_HEIGHT * 0.85`, `MAP_ACTUAL_WIDTH = MAP_ACTUAL_HEIGHT / 3 * 4` (4:3 ratio)
- HR mode flips the Y axis — editor should work in normal (non-HR) space

### Data Model (all in `com.rian.difficultycalculator.beatmap`)
- `BeatmapData` — central container: `general`, `metadata`, `difficulty`, `events`, `colors`, `rawTimingPoints`, `rawHitObjects`, `timingPoints` (BeatmapControlPointsManager), `hitObjects` (BeatmapHitObjectsManager)
- `HitObject` — base class: `startTime`, `position: Vector2`, `endPosition`, `stackHeight`, `scale`
- `HitCircle extends HitObject`
- `Slider extends HitObjectWithDuration` — adds `repeatCount`, `path: SliderPath`, `nestedHitObjects`, `velocity`, `spanDuration`
- `Spinner extends HitObjectWithDuration`
- `SliderPath` — `pathType: SliderPathType` (Catmull/Bezier/Linear/PerfectCurve), `controlPoints: ArrayList<Vector2>`, `expectedDistance`
- `TimingControlPoint` — `time`, `msPerBeat`, `timeSignature` (4/4 default)
- `DifficultyControlPoint` — `time`, `speedMultiplier`, `generateTicks`
- `BeatmapControlPointsManager` — holds `timing` and `difficulty` ControlPointManagers

### Parsing
- `BeatmapParser` — reads .osu file, delegates to section parsers
- `BeatmapHitObjectsParser.parse(BeatmapData, String line)` — parses hit object lines, creates HitCircle/Slider/Spinner
- `BeatmapControlPointsParser` — parses timing point lines
- `rawHitObjects` and `rawTimingPoints` ArrayLists are preserved during parsing — can be modified and re-serialized

### Rendering (for reference, editor reuses this)
- Scene hierarchy: `bgScene` → `mgScene` → `fgScene`
- Hit objects use `SpritePool` for sprites, `GameObjectPool` for HitCircle/Slider/Spinner instances
- Slider body: `SliderBody2D` with `TrianglePack` from `SpriteCache.trianglePackCache`
- Object scale: `(RES_HEIGHT/480) * (54.42 - CS*4.48) * 2/BASE_OBJECT_SIZE + 0.5*scaleMultiplier`

### Audio
- `BassAudioFunc` — BASS library for playback, seeking, tempo
- `getSpectrum()` returns FFT data (512 samples) —可用于 waveform rendering
- `jump(int ms)` for seeking, `getPosition()` for current time

### What Does NOT Exist (needs to be built)
- No beatmap encoder/writer — must build .osu serialization
- No waveform visualization in gameplay — need to build from FFT or raw audio
- No editor UI fragments or activity
- The `[Editor]` section in .osu format is parsed but silently ignored

## Requirements
1. **Toolbar-based UI** — Select tool (place/move/delete/select), then interact with playfield
2. **Hit object placement** — Tap to place circle, drag to create slider, long-press for spinner
3. **Hit object manipulation** — Move objects, delete objects, select objects
4. **Slider editing** — Edit curve points, repeat count, length
5. **Timing point editor** — BPM, offset, time signature, kiai toggle
6. **Difficulty settings** — CS, AR, OD, HP, slider multiplier
7. **Metadata editor** — Title, artist, creator, difficulty name, tags
8. **Timeline** — Waveform display with timing grid, snap-to-grid
9. **Audio preview** — Play from current position with hit sounds
10. **Save/export** — Serialize back to .osu format and write to file
11. **Combo editing** — New combo flag, combo color selection

## Staged Approach (suggested)
1. **Stage 1**: Basic editor scene — render existing beatmap objects on a grid, timeline with audio waveform, play/pause/seek
2. **Stage 2**: Object placement — tap to place circles, drag for sliders, basic undo
3. **Stage 3**: Object manipulation — move, delete, select, multi-select
4. **Stage 4**: Timing point editor — add/remove/edit timing points on timeline
5. **Stage 5**: Slider editing — edit curve points, repeats, length
6. **Stage 6**: Metadata & difficulty editor — settings panels
7. **Stage 7**: Save/export — .osu file serialization
8. **Stage 8**: Polish — undo/redo, copy/paste, grid snapping, test play from editor

## Key File Paths
| Component | Path |
|-----------|------|
| BeatmapData | `src/ru/nsu/ccfit/zuev/osu/beatmap/BeatmapData.java` |
| BeatmapParser | `src/ru/nsu/ccfit/zuev/osu/beatmap/parser/BeatmapParser.java` |
| HitObjectsParser | `src/ru/nsu/ccfit/zuev/osu/beatmap/parser/sections/BeatmapHitObjectsParser.java` |
| ControlPointsParser | `src/ru/nsu/ccfit/zuev/osu/beatmap/parser/sections/BeatmapControlPointsParser.java` |
| HitObject (base) | `src/com/rian/difficultycalculator/beatmap/hitobject/HitObject.java` |
| HitCircle | `src/com/rian/difficultycalculator/beatmap/hitobject/HitCircle.java` |
| Slider | `src/com/rian/difficultycalculator/beatmap/hitobject/Slider.java` |
| Spinner | `src/com/rian/difficultycalculator/beatmap/hitobject/Spinner.java` |
| SliderPath | `src/com/rian/difficultycalculator/beatmap/hitobject/SliderPath.java` |
| ControlPointsManager | `src/com/rian/difficultycalculator/beatmap/timings/ControlPointManager.java` |
| TimingControlPoint | `src/com/rian/difficultycalculator/beatmap/timings/TimingControlPoint.java` |
| DifficultyControlPoint | `src/com/rian/difficultycalculator/beatmap/timings/DifficultyControlPoint.java` |
| Utils (coordinates) | `src/ru/nsu/ccfit/zuev/osu/Utils.java` |
| Constants | `src/ru/nsu/ccfit/zuev/osu/Constants.java` |
| Config | `src/ru/nsu/ccfit/zuev/osu/Config.java` |
| GameHelper | `src/ru/nsu/ccfit/zuev/osu/game/GameHelper.java` |
| SpritePool | `src/ru/nsu/ccfit/zuev/osu/sprite/SpritePool.java` |
| GameObjectPool | `src/ru/nsu/ccfit/zuev/osu/game/GameObjectPool.java` |
| SliderBody2D | `src/com/edlplan/osu/support/slider/SliderBody2D.java` |
| BassAudioFunc | `src/ru/nsu/ccfit/zuev/audio/serviceAudio/BassAudioFunc.java` |
| OsuSkin | `src/ru/nsu/ccfit/zuev/skins/OsuSkin.java` |
| BeatmapSection enum | `src/ru/nsu/ccfit/zuev/osu/beatmap/constants/BeatmapSection.java` |
| HitObjectType | `src/ru/nsu/ccfit/zuev/osu/beatmap/constants/HitObjectType.java` |
