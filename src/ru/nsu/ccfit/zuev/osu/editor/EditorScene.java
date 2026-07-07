package ru.nsu.ccfit.zuev.osu.editor;

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.math.Vector2;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

/**
 * In-game beatmap editor scene.
 * Renders hit objects on a playfield grid with timeline and audio playback controls.
 */
public class EditorScene implements IUpdateHandler {

    private static final float GRID_SIZE = 16;
    private static final float TIMELINE_HEIGHT = 0.15f;
    private static final float WAVEFORM_HEIGHT = 60f;
    private static final float PLAYBACK_BAR_WIDTH = 2f;

    private final Engine engine;
    private final Scene scene;

    private Scene bgScene;
    private Scene mgScene;
    private Scene fgScene;

    private BeatmapData beatmapData;
    private String beatmapPath;
    private String audioPath;

    // Playfield
    private Rectangle playfieldBg;
    private Rectangle playfieldBorder;
    private ArrayList<Rectangle> gridLines;
    private float playfieldOffsetX;
    private float playfieldOffsetY;
    private float playfieldWidth;
    private float playfieldHeight;

    // Timeline
    private Rectangle timelineBg;
    private Rectangle timelineBar;
    private Rectangle timelineCursor;
    private ChangeableText timeText;
    private ArrayList<Rectangle> beatSnapLines;
    private ArrayList<TimingMarker> timingMarkers;

    // Waveform
    private Rectangle waveformBg;
    private ArrayList<Rectangle> waveformBars;

    // Object sprites
    private ArrayList<EditorObjectSprite> objectSprites;

    // State
    private boolean isPlaying = false;
    private float currentTime = 0f;
    private float totalDuration = 0f;
    private float beatSnap = 1f; // 1/1 beat snap
    private int selectedObjectIndex = -1;
    private float scrollVelocity = 0f;

    // Editor mode
    private EditorTool currentTool = EditorTool.Select;

    public EditorScene(Engine engine) {
        this.engine = engine;

        scene = new Scene();
        bgScene = new Scene();
        mgScene = new Scene();
        fgScene = new Scene();

        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        scene.attachChild(fgScene);

        gridLines = new ArrayList<>();
        objectSprites = new ArrayList<>();
        timingMarkers = new ArrayList<>();
        beatSnapLines = new ArrayList<>();
        waveformBars = new ArrayList<>();

        setupTouchHandler();
    }

    /**
     * Loads a beatmap into the editor.
     */
    public void loadBeatmap(BeatmapData data, String path) {
        this.beatmapData = data;
        this.beatmapPath = path;

        if (data.general.audioFilename != null && !data.general.audioFilename.isEmpty()) {
            audioPath = data.getFolder() + "/" + data.general.audioFilename;
        }

        totalDuration = (float) data.getDuration();
        currentTime = 0f;

        calculatePlayfield();
        createPlayfield();
        createTimeline();
        createWaveform();
        createPlaybackControls();
        renderHitObjects();
        renderTimingMarkers();
    }

    private void calculatePlayfield() {
        Camera camera = engine.getCamera();
        float camWidth = camera.getWidth();
        float camHeight = camera.getHeight();

        playfieldHeight = Constants.MAP_ACTUAL_HEIGHT;
        playfieldWidth = Constants.MAP_ACTUAL_WIDTH;
        playfieldOffsetX = (camWidth - playfieldWidth) / 2f;
        playfieldOffsetY = (camHeight - playfieldHeight) / 2f;
    }

    private void createPlayfield() {
        Camera camera = engine.getCamera();

        // Background
        playfieldBg = new Rectangle(playfieldOffsetX, playfieldOffsetY, playfieldWidth, playfieldHeight);
        playfieldBg.setColor(0.1f, 0.1f, 0.1f);
        mgScene.attachChild(playfieldBg);

        // Border
        playfieldBorder = new Rectangle(playfieldOffsetX - 1, playfieldOffsetY - 1,
                playfieldWidth + 2, playfieldHeight + 2);
        playfieldBorder.setColor(0.4f, 0.4f, 0.4f);
        mgScene.attachChild(playfieldBorder);

        // Grid lines
        createGridLines();
    }

    private void createGridLines() {
        // Clear existing grid lines
        for (Rectangle line : gridLines) {
            mgScene.detachChild(line);
        }
        gridLines.clear();

        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;

        // Vertical lines
        for (int x = 0; x <= Constants.MAP_WIDTH; x += (int) GRID_SIZE) {
            float screenX = playfieldOffsetX + x * scaleX;
            Rectangle line = new Rectangle(screenX, playfieldOffsetY, 1, playfieldHeight);
            line.setColor(0.2f, 0.2f, 0.2f, 0.3f);
            mgScene.attachChild(line);
            gridLines.add(line);
        }

        // Horizontal lines
        for (int y = 0; y <= Constants.MAP_HEIGHT; y += (int) GRID_SIZE) {
            float screenY = playfieldOffsetY + y * scaleY;
            Rectangle line = new Rectangle(playfieldOffsetX, screenY, playfieldWidth, 1);
            line.setColor(0.2f, 0.2f, 0.2f, 0.3f);
            mgScene.attachChild(line);
            gridLines.add(line);
        }
    }

    private void createTimeline() {
        Camera camera = engine.getCamera();
        float camWidth = camera.getWidth();
        float timelineY = 0;
        float timelineH = WAVEFORM_HEIGHT + 30;

        // Timeline background
        timelineBg = new Rectangle(0, timelineY, camWidth, timelineH);
        timelineBg.setColor(0.15f, 0.15f, 0.15f);
        fgScene.attachChild(timelineBg);

        // Waveform area
        waveformBg = new Rectangle(0, timelineY, camWidth, WAVEFORM_HEIGHT);
        waveformBg.setColor(0.05f, 0.05f, 0.1f);
        fgScene.attachChild(waveformBg);

        // Beat snap lines (on timeline)
        renderBeatSnapLines();

        // Timeline cursor (vertical bar)
        timelineCursor = new Rectangle(0, timelineY, PLAYBACK_BAR_WIDTH, timelineH);
        timelineCursor.setColor(1f, 0.3f, 0.3f);
        fgScene.attachChild(timelineCursor);

        // Time text
        Font font = getFont();
        if (font != null) {
            timeText = new ChangeableText(camWidth - 150, timelineY + WAVEFORM_HEIGHT + 5, font,
                    "00:00.000 / 00:00.000", 30);
            timeText.setColor(0.8f, 0.8f, 0.8f);
            fgScene.attachChild(timeText);
        }
    }

    private void createWaveform() {
        // Clear existing waveform bars
        for (Rectangle bar : waveformBars) {
            fgScene.detachChild(bar);
        }
        waveformBars.clear();

        Camera camera = engine.getCamera();
        float camWidth = camera.getWidth();
        int barCount = (int) (camWidth / 3);

        for (int i = 0; i < barCount; i++) {
            float x = i * 3f;
            Rectangle bar = new Rectangle(x, 0, 2, 1);
            bar.setColor(0.3f, 0.5f, 0.8f);
            waveformBg.attachChild(bar);
            waveformBars.add(bar);
        }
    }

    private void renderBeatSnapLines() {
        for (Rectangle line : beatSnapLines) {
            fgScene.detachChild(line);
        }
        beatSnapLines.clear();

        Camera camera = engine.getCamera();
        float camWidth = camera.getWidth();

        if (beatmapData == null) return;

        // Get BPM from first timing point
        double bpm = 120;
        if (!beatmapData.timingPoints.timing.getControlPoints().isEmpty()) {
            bpm = beatmapData.timingPoints.timing.getControlPoints().get(0).getBPM();
        }

        double msPerBeat = 60000.0 / bpm;
        float snapMs = (float) (msPerBeat * beatSnap);

        for (float t = 0; t <= totalDuration; t += snapMs) {
            float x = (t / totalDuration) * camWidth;
            Rectangle line = new Rectangle(x, 0, 1, WAVEFORM_HEIGHT);
            line.setColor(0.3f, 0.3f, 0.3f, 0.5f);
            fgScene.attachChild(line);
            beatSnapLines.add(line);
        }
    }

    private void createPlaybackControls() {
        Camera camera = engine.getCamera();
        float camWidth = camera.getWidth();
        float camHeight = camera.getHeight();
        float controlsY = WAVEFORM_HEIGHT + 35;
        float btnSize = 40;
        float spacing = 10;
        float totalWidth = btnSize * 6 + spacing * 5;
        float startX = (camWidth - totalWidth) / 2f;

        Font font = getFont();

        // Back button
        Rectangle backBtn = new Rectangle(startX, controlsY, btnSize, btnSize * 0.6f) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.8f, 0.3f, 0.3f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(0.6f, 0.2f, 0.2f);
                    back();
                    return true;
                }
                return false;
            }
        };
        backBtn.setColor(0.6f, 0.2f, 0.2f);
        fgScene.attachChild(backBtn);
        scene.registerTouchArea(backBtn);

        Text backLabel = new Text(0, 0, font, "BACK");
        backLabel.setPosition(startX + (btnSize - backLabel.getWidth()) / 2f, controlsY + (btnSize * 0.6f - backLabel.getHeight()) / 2f);
        backLabel.setColor(1f, 1f, 1f);
        fgScene.attachChild(backLabel);

        // Play button
        Rectangle playBtnRect = new Rectangle(startX + btnSize + spacing, controlsY, btnSize, btnSize * 0.6f) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.3f, 0.6f, 0.3f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(0.3f, 0.5f, 0.3f);
                    play();
                    return true;
                }
                return false;
            }
        };
        playBtnRect.setColor(0.3f, 0.5f, 0.3f);
        fgScene.attachChild(playBtnRect);
        scene.registerTouchArea(playBtnRect);

        Text playLabel = new Text(0, 0, font, "PLAY");
        playLabel.setPosition(startX + btnSize + spacing + (btnSize - playLabel.getWidth()) / 2f, controlsY + (btnSize * 0.6f - playLabel.getHeight()) / 2f);
        playLabel.setColor(1f, 1f, 1f);
        fgScene.attachChild(playLabel);

        // Pause button
        Rectangle pauseBtnRect = new Rectangle(startX + (btnSize + spacing) * 2, controlsY, btnSize, btnSize * 0.6f) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.6f, 0.6f, 0.2f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(0.5f, 0.5f, 0.2f);
                    pause();
                    return true;
                }
                return false;
            }
        };
        pauseBtnRect.setColor(0.5f, 0.5f, 0.2f);
        pauseBtnRect.setVisible(false);
        fgScene.attachChild(pauseBtnRect);
        scene.registerTouchArea(pauseBtnRect);

        Text pauseLabel = new Text(0, 0, font, "PAUSE");
        pauseLabel.setPosition(startX + (btnSize + spacing) * 2 + (btnSize - pauseLabel.getWidth()) / 2f, controlsY + (btnSize * 0.6f - pauseLabel.getHeight()) / 2f);
        pauseLabel.setColor(1f, 1f, 1f);
        fgScene.attachChild(pauseLabel);

        // Stop button
        Rectangle stopBtnRect = new Rectangle(startX + (btnSize + spacing) * 3, controlsY, btnSize, btnSize * 0.6f) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.6f, 0.2f, 0.2f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(0.5f, 0.2f, 0.2f);
                    stop();
                    return true;
                }
                return false;
            }
        };
        stopBtnRect.setColor(0.5f, 0.2f, 0.2f);
        fgScene.attachChild(stopBtnRect);
        scene.registerTouchArea(stopBtnRect);

        Text stopLabel = new Text(0, 0, font, "STOP");
        stopLabel.setPosition(startX + (btnSize + spacing) * 3 + (btnSize - stopLabel.getWidth()) / 2f, controlsY + (btnSize * 0.6f - stopLabel.getHeight()) / 2f);
        stopLabel.setColor(1f, 1f, 1f);
        fgScene.attachChild(stopLabel);

        // Rewind button
        Rectangle rewindBtnRect = new Rectangle(startX + (btnSize + spacing) * 4, controlsY, btnSize, btnSize * 0.6f) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.4f, 0.4f, 0.6f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(0.3f, 0.3f, 0.5f);
                    seekTo(currentTime - 5000);
                    return true;
                }
                return false;
            }
        };
        rewindBtnRect.setColor(0.3f, 0.3f, 0.5f);
        fgScene.attachChild(rewindBtnRect);
        scene.registerTouchArea(rewindBtnRect);

        Text rewindLabel = new Text(0, 0, font, "<<");
        rewindLabel.setPosition(startX + (btnSize + spacing) * 4 + (btnSize - rewindLabel.getWidth()) / 2f, controlsY + (btnSize * 0.6f - rewindLabel.getHeight()) / 2f);
        rewindLabel.setColor(1f, 1f, 1f);
        fgScene.attachChild(rewindLabel);

        // Forward button
        Rectangle forwardBtnRect = new Rectangle(startX + (btnSize + spacing) * 5, controlsY, btnSize, btnSize * 0.6f) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.4f, 0.4f, 0.6f);
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    setColor(0.3f, 0.3f, 0.5f);
                    seekTo(currentTime + 5000);
                    return true;
                }
                return false;
            }
        };
        forwardBtnRect.setColor(0.3f, 0.3f, 0.5f);
        fgScene.attachChild(forwardBtnRect);
        scene.registerTouchArea(forwardBtnRect);

        Text forwardLabel = new Text(0, 0, font, ">>");
        forwardLabel.setPosition(startX + (btnSize + spacing) * 5 + (btnSize - forwardLabel.getWidth()) / 2f, controlsY + (btnSize * 0.6f - forwardLabel.getHeight()) / 2f);
        forwardLabel.setColor(1f, 1f, 1f);
        fgScene.attachChild(forwardLabel);
    }

    private void renderHitObjects() {
        // Clear existing sprites
        for (EditorObjectSprite spr : objectSprites) {
            mgScene.detachChild(spr.sprite);
            if (spr.endSprite != null) mgScene.detachChild(spr.endSprite);
            if (spr.bodyRect != null) mgScene.detachChild(spr.bodyRect);
        }
        objectSprites.clear();

        if (beatmapData == null) return;

        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;

        ArrayList<RGBColor> combos = getDefaultCombos();
        int comboIndex = 0;
        int objectIndex = 0;

        for (HitObject obj : objects) {
            Vector2 pos = obj.getStackedPosition();
            float screenX = playfieldOffsetX + pos.x * scaleX;
            float screenY = playfieldOffsetY + pos.y * scaleY;

            RGBColor color = combos.get(comboIndex % combos.size());

            if (obj instanceof HitCircle) {
                renderHitCircle(screenX, screenY, color, objectIndex);
            } else if (obj instanceof Slider) {
                renderSlider(screenX, screenY, (Slider) obj, color, objectIndex, scaleX, scaleY);
            } else if (obj instanceof Spinner) {
                renderSpinner(color, objectIndex);
            }

            // Check for new combo
            if (obj instanceof HitCircle || obj instanceof Slider) {
                // Simple combo increment every 5 objects
                if (objectIndex > 0 && objectIndex % 5 == 0) {
                    comboIndex++;
                }
            }

            objectIndex++;
        }
    }

    private void renderHitCircle(float x, float y, RGBColor color, int index) {
        float radius = getCircleRadius();

        Rectangle circle = new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
        circle.setColor(color.r(), color.g(), color.b());
        mgScene.attachChild(circle);

        // Inner circle (white)
        float innerRadius = radius * 0.7f;
        Rectangle inner = new Rectangle(x - innerRadius, y - innerRadius, innerRadius * 2, innerRadius * 2);
        inner.setColor(1f, 1f, 1f, 0.5f);
        mgScene.attachChild(inner);

        EditorObjectSprite spr = new EditorObjectSprite();
        spr.sprite = circle;
        spr.objectIndex = index;
        objectSprites.add(spr);
    }

    private void renderSlider(float x, float y, Slider slider, RGBColor color, int index,
                              float scaleX, float scaleY) {
        SliderPath path = slider.getPath();

        // Render slider body as a series of rectangles
        ArrayList<Vector2> calculatedPath = path.calculatedPath;
        if (calculatedPath.size() >= 2) {
            for (int i = 0; i < calculatedPath.size() - 1; i++) {
                Vector2 p1 = calculatedPath.get(i);
                Vector2 p2 = calculatedPath.get(i + 1);

                float sx1 = playfieldOffsetX + p1.x * scaleX;
                float sy1 = playfieldOffsetY + p1.y * scaleY;
                float sx2 = playfieldOffsetX + p2.x * scaleX;
                float sy2 = playfieldOffsetY + p2.y * scaleY;

                float dx = sx2 - sx1;
                float dy = sy2 - sy1;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = (float) Math.atan2(dy, dx);

                float bodyWidth = 6;
                Rectangle segment = new Rectangle(
                        Math.min(sx1, sx2) - bodyWidth / 2,
                        Math.min(sy1, sy2) - bodyWidth / 2,
                        Math.max(Math.abs(dx), bodyWidth),
                        Math.max(Math.abs(dy), bodyWidth)
                );
                segment.setColor(color.r(), color.g(), color.b(), 0.7f);
                mgScene.attachChild(segment);

                EditorObjectSprite spr = new EditorObjectSprite();
                spr.sprite = segment;
                spr.objectIndex = index;
                objectSprites.add(spr);
            }
        }

        // Render start circle
        float radius = getCircleRadius() * 0.6f;
        Rectangle startCircle = new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
        startCircle.setColor(color.r(), color.g(), color.b());
        mgScene.attachChild(startCircle);

        // Render end circle
        Vector2 endPos = slider.getStackedEndPosition();
        float endX = playfieldOffsetX + endPos.x * scaleX;
        float endY = playfieldOffsetY + endPos.y * scaleY;
        Rectangle endCircle = new Rectangle(endX - radius, endY - radius, radius * 2, radius * 2);
        endCircle.setColor(color.r(), color.g(), color.b(), 0.5f);
        mgScene.attachChild(endCircle);

        // Add reverse arrows if repeat > 1
        if (slider.getRepeatCount() > 1) {
            // Simplified: just mark the endpoints
            Rectangle reverseArrow = new Rectangle(x - 5, y - 12, 10, 10);
            reverseArrow.setColor(1f, 1f, 1f);
            mgScene.attachChild(reverseArrow);
        }
    }

    private void renderSpinner(RGBColor color, int index) {
        float cx = playfieldOffsetX + playfieldWidth / 2f;
        float cy = playfieldOffsetY + playfieldHeight / 2f;
        float radius = Math.min(playfieldWidth, playfieldHeight) * 0.3f;

        Rectangle spinnerCircle = new Rectangle(cx - radius, cy - radius, radius * 2, radius * 2);
        spinnerCircle.setColor(color.r(), color.g(), color.b(), 0.3f);
        mgScene.attachChild(spinnerCircle);

        EditorObjectSprite spr = new EditorObjectSprite();
        spr.sprite = spinnerCircle;
        spr.objectIndex = index;
        objectSprites.add(spr);
    }

    private void renderTimingMarkers() {
        for (TimingMarker marker : timingMarkers) {
            fgScene.detachChild(marker.line);
            fgScene.detachChild(marker.label);
        }
        timingMarkers.clear();

        if (beatmapData == null) return;

        Camera camera = engine.getCamera();
        float camWidth = camera.getWidth();
        Font font = getFont();

        List<com.rian.difficultycalculator.beatmap.timings.TimingControlPoint> timingPoints =
                beatmapData.timingPoints.timing.getControlPoints();

        for (com.rian.difficultycalculator.beatmap.timings.TimingControlPoint tp : timingPoints) {
            float t = (float) tp.time;
            float x = (t / totalDuration) * camWidth;

            Rectangle line = new Rectangle(x, 0, 2, WAVEFORM_HEIGHT + 30);
            line.setColor(1f, 0.8f, 0.2f, 0.8f);
            fgScene.attachChild(line);

            TimingMarker marker = new TimingMarker();
            marker.line = line;
            marker.time = t;
            marker.bpm = tp.getBPM();

            if (font != null) {
                Text label = new Text(x + 4, WAVEFORM_HEIGHT + 5, font,
                        String.format("%.0f BPM", tp.getBPM()));
                label.setColor(1f, 0.8f, 0.2f);
                fgScene.attachChild(label);
                marker.label = label;
            }

            timingMarkers.add(marker);
        }
    }

    private void setupTouchHandler() {
        scene.setOnAreaTouchListener(new Scene.IOnAreaTouchListener() {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, org.anddev.andengine.entity.scene.Scene.ITouchArea pTouchArea, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                return handleTouch(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
            }
        });
    }

    private boolean handleTouch(TouchEvent event, float x, float y) {
        Camera camera = engine.getCamera();

        // Timeline touch (scrubbing)
        if (y < WAVEFORM_HEIGHT + 30) {
            if (event.isActionDown() || event.isActionMove()) {
                currentTime = (x / camera.getWidth()) * totalDuration;
                currentTime = Math.max(0, Math.min(currentTime, totalDuration));
                updateTimelineCursor();
                updateTimeText();
                return true;
            }
        }

        // Playfield touch (select object)
        if (x >= playfieldOffsetX && x <= playfieldOffsetX + playfieldWidth &&
                y >= playfieldOffsetY && y <= playfieldOffsetY + playfieldHeight) {
            if (event.isActionDown()) {
                selectObjectAt(x, y);
                return true;
            }
        }

        return false;
    }

    private void selectObjectAt(float screenX, float screenY) {
        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;
        float trackX = (screenX - playfieldOffsetX) / scaleX;
        float trackY = (screenY - playfieldOffsetY) / scaleY;

        float hitRadius = 64; // osu! standard radius

        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        selectedObjectIndex = -1;

        for (int i = 0; i < objects.size(); i++) {
            HitObject obj = objects.get(i);
            Vector2 pos = obj.getPosition();
            float dx = trackX - pos.x;
            float dy = trackY - pos.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= hitRadius) {
                selectedObjectIndex = i;
                highlightObject(i);
                break;
            }
        }
    }

    private void highlightObject(int index) {
        // Reset all objects to default alpha
        for (EditorObjectSprite spr : objectSprites) {
            if (spr.sprite != null) {
                spr.sprite.setAlpha(spr.objectIndex == index ? 1.0f : 0.6f);
            }
        }
    }

    private void updateTimelineCursor() {
        if (timelineCursor != null) {
            Camera camera = engine.getCamera();
            float x = (currentTime / totalDuration) * camera.getWidth();
            timelineCursor.setPosition(x, timelineCursor.getY());
        }
    }

    private void updateTimeText() {
        if (timeText != null) {
            timeText.setText(formatTime(currentTime) + " / " + formatTime(totalDuration));
        }
    }

    private String formatTime(float ms) {
        int totalSec = (int) (ms / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        int millis = (int) (ms % 1000);
        return String.format("%02d:%02d.%03d", min, sec, millis);
    }

    private float getCircleRadius() {
        float cs = beatmapData != null ? beatmapData.difficulty.cs : 5;
        return Utils.toRes(128) * GameHelper.getScale() / 2;
    }

    private ArrayList<RGBColor> getDefaultCombos() {
        ArrayList<RGBColor> combos = new ArrayList<>();

        if (beatmapData != null && !beatmapData.colors.comboColors.isEmpty()) {
            for (int i = 0; i < beatmapData.colors.comboColors.size(); i++) {
                combos.add(new RGBColor(beatmapData.colors.comboColors.get(i)));
            }
        }

        if (combos.isEmpty()) {
            combos.add(new RGBColor(1f, 0.4f, 0.4f));
            combos.add(new RGBColor(0.4f, 1f, 0.4f));
            combos.add(new RGBColor(0.4f, 0.4f, 1f));
            combos.add(new RGBColor(1f, 1f, 0.4f));
            combos.add(new RGBColor(1f, 0.4f, 1f));
            combos.add(new RGBColor(0.4f, 1f, 1f));
        }

        return combos;
    }

    private Font getFont() {
        try {
            return ResourceManager.getInstance().getFont("smallFont");
        } catch (Exception e) {
            return null;
        }
    }

    // Public API

    public Scene getScene() {
        return scene;
    }

    public void play() {
        isPlaying = true;
    }

    public void pause() {
        isPlaying = false;
    }

    public void stop() {
        isPlaying = false;
        currentTime = 0f;
        updateTimelineCursor();
        updateTimeText();
    }

    public void seekTo(float timeMs) {
        currentTime = Math.max(0, Math.min(timeMs, totalDuration));
        updateTimelineCursor();
        updateTimeText();
    }

    public void setCurrentTool(EditorTool tool) {
        this.currentTool = tool;
    }

    public EditorTool getCurrentTool() {
        return currentTool;
    }

    public BeatmapData getBeatmapData() {
        return beatmapData;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public int getSelectedObjectIndex() {
        return selectedObjectIndex;
    }

    public void deleteSelectedObject() {
        if (selectedObjectIndex >= 0 && beatmapData != null) {
            beatmapData.hitObjects.remove(selectedObjectIndex);
            selectedObjectIndex = -1;
            renderHitObjects();
        }
    }

    public void refresh() {
        renderHitObjects();
        renderTimingMarkers();
        renderBeatSnapLines();
    }

    /**
     * Editor tool modes.
     */
    public enum EditorTool {
        Select,
        Circle,
        Slider,
        Spinner,
        Delete,
        TimingAdd,
        TimingDelete
    }

    /**
     * Internal representation of an editor object sprite.
     */
    private static class EditorObjectSprite {
        Rectangle sprite;
        Rectangle endSprite;
        Rectangle bodyRect;
        int objectIndex;
    }

    /**
     * Timing marker on the timeline.
     */
    private static class TimingMarker {
        Rectangle line;
        Text label;
        float time;
        double bpm;
    }

    // IUpdateHandler implementation

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (isPlaying) {
            currentTime += pSecondsElapsed * 1000f;
            if (currentTime >= totalDuration) {
                currentTime = totalDuration;
                stop();
            }
            updateTimelineCursor();
            updateTimeText();
        }
    }

    @Override
    public void reset() {
        currentTime = 0f;
        isPlaying = false;
    }

    /**
     * Shows the editor scene and registers the update handler.
     */
    public void show() {
        scene.registerUpdateHandler(this);
        engine.setScene(scene);
    }

    /**
     * Navigates back to the song menu.
     */
    public void back() {
        scene.unregisterUpdateHandler(this);
        stop();

        GlobalManager manager = GlobalManager.getInstance();
        if (manager.getSongMenu() != null) {
            manager.getSongMenu().show();
        }
    }
}
