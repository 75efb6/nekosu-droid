package ru.nsu.ccfit.zuev.osu.editor;

import android.util.Log;

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPathType;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

public class EditorScene implements IUpdateHandler {

    private static final float GRID_SIZE = 16;
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
    private float beatSnap = 1f;
    private int selectedObjectIndex = -1;
    private EditorTool currentTool = EditorTool.Select;

    // Drag state
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float dragObjStartX, dragObjStartY;

    // Undo/Redo
    private final Stack<EditorAction> undoStack = new Stack<>();
    private final Stack<EditorAction> redoStack = new Stack<>();

    // Slider creation
    private boolean isCreatingSlider = false;
    private float sliderStartX, sliderStartY;

    // Grid snap
    private boolean gridSnapEnabled = true;
    private Rectangle gridSnapBtn;
    private Text gridSnapLabel;

    // Multi-select
    private final HashSet<Integer> selectedObjects = new HashSet<>();
    private boolean isMultiSelecting = false;

    // Copy/Paste clipboard
    private ArrayList<HitObject> clipboard = new ArrayList<>();
    private Rectangle clipboardSprite;

    // Last known spectrum for static waveform display
    private float[] lastSpectrum;

    // Combo tracking (newCombo flag per object index)
    private final HashMap<Integer, Boolean> newComboFlags = new HashMap<>();
    private int comboColorCount = 0;

    // Kiai tracking (per timing point time)
    private final HashMap<Double, Boolean> kiaiFlags = new HashMap<>();

    // Slider editing
    private boolean isEditingSlider = false;
    private int editingSliderPointIndex = -1;

    // Grid visibility
    private boolean gridVisible = true;
    private Rectangle gridToggleBtn;

    // Beat snap cycling
    private static final float[] BEAT_SNAPS = {1f, 0.5f, 1f/3f, 0.25f, 1f/6f, 0.125f};
    private static final String[] BEAT_SNAP_LABELS = {"1/1", "1/2", "1/3", "1/4", "1/6", "1/8"};
    private int beatSnapIndex = 0;
    private Rectangle beatSnapBtn;
    private Text beatSnapLabel;

    // Beat skip
    private Rectangle prevBeatBtn;
    private Rectangle nextBeatBtn;

    // Object time nudge
    private Rectangle nudgeBackBtn;
    private Rectangle nudgeFwdBtn;

    // Redo
    private Rectangle redoBtn;

    // Waveform zoom
    private float waveformZoom = 1f;
    private float waveformScrollX = 0f;
    private Rectangle zoomInBtn;
    private Rectangle zoomOutBtn;
    private Rectangle zoomResetBtn;

    // Toolbar
    private EditorToolbarFragment currentToolbar;
    private boolean toolbarVisible = true;
    private Rectangle toolbarToggleBtn;

    public EditorScene(Engine engine) {
        this.engine = engine;

        scene = new Scene();
        bgScene = new Scene();
        mgScene = new Scene();
        fgScene = new Scene();

        scene.attachChild(bgScene);
        scene.attachChild(mgScene);
        scene.attachChild(fgScene);

        bgScene.setBackgroundEnabled(false);
        mgScene.setBackgroundEnabled(false);
        fgScene.setBackgroundEnabled(false);

        gridLines = new ArrayList<>();
        objectSprites = new ArrayList<>();
        timingMarkers = new ArrayList<>();
        beatSnapLines = new ArrayList<>();
        waveformBars = new ArrayList<>();

        setupTouchHandler();
    }

    public void loadBeatmap(BeatmapData data, String path) {
        this.beatmapData = data;
        this.beatmapPath = path;
        undoStack.clear();
        redoStack.clear();
        newComboFlags.clear();
        kiaiFlags.clear();

        // Populate newComboFlags from raw hit object data
        if (data.rawHitObjects != null) {
            for (int i = 0; i < data.rawHitObjects.size(); i++) {
                String[] pars = data.rawHitObjects.get(i).split(",");
                if (pars.length >= 4) {
                    try {
                        int typeBits = Integer.parseInt(pars[3].trim());
                        if ((typeBits & 4) != 0) {
                            newComboFlags.put(i, true);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (data.general.audioFilename != null && !data.general.audioFilename.isEmpty()) {
            audioPath = data.getFolder() + "/" + data.general.audioFilename;
        }

        totalDuration = (float) data.getDuration();
        currentTime = 0f;

        if (audioPath != null) {
            var svc = GlobalManager.getInstance().getSongService();
            if (svc != null) {
                svc.preLoad(audioPath);
                svc.setVolume(Config.getBgmVolume());
                totalDuration = svc.getLength();
            }
        }

        calculatePlayfield();
        Log.i("EditorScene", "Playfield: " + playfieldWidth + "x" + playfieldHeight +
                " offset=" + playfieldOffsetX + "," + playfieldOffsetY);
        Log.i("EditorScene", "Objects: " + data.hitObjects.getObjects().size());
        createPlayfield();
        createTimeline();
        createWaveform();
        createPlaybackControls();
        createSettingsButton();
        createGridSnapButton();
        createBeatSnapButton();
        createBeatSkipButtons();
        createNudgeButtons();
        createZoomButtons();
        renderHitObjects();
        renderTimingMarkers();
        showToolbar();
    }

    private void calculatePlayfield() {
        Camera camera = engine.getCamera();
        float resH = Config.getRES_HEIGHT();
        playfieldHeight = resH > 0 ? resH * 0.85f : Constants.MAP_ACTUAL_HEIGHT;
        playfieldWidth = playfieldHeight / 3f * 4f;
        if (playfieldWidth <= 0 || playfieldHeight <= 0) {
            playfieldWidth = camera.getWidth() * 0.8f;
            playfieldHeight = playfieldWidth * 3f / 4f;
        }
        playfieldOffsetX = (camera.getWidth() - playfieldWidth) / 2f;
        playfieldOffsetY = (camera.getHeight() - playfieldHeight) / 2f;
    }

    private void createPlayfield() {
        playfieldBg = new Rectangle(playfieldOffsetX, playfieldOffsetY, playfieldWidth, playfieldHeight);
        playfieldBg.setColor(0.1f, 0.1f, 0.1f);
        mgScene.attachChild(playfieldBg);

        playfieldBorder = new Rectangle(playfieldOffsetX - 1, playfieldOffsetY - 1,
                playfieldWidth + 2, playfieldHeight + 2);
        playfieldBorder.setColor(0.4f, 0.4f, 0.4f);
        mgScene.attachChild(playfieldBorder);

        createGridLines();
    }

    private void createGridLines() {
        for (Rectangle line : gridLines) mgScene.detachChild(line);
        gridLines.clear();

        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;

        for (int x = 0; x <= Constants.MAP_WIDTH; x += (int) GRID_SIZE) {
            float screenX = playfieldOffsetX + x * scaleX;
            Rectangle line = new Rectangle(screenX, playfieldOffsetY, 1, playfieldHeight);
            line.setColor(0.2f, 0.2f, 0.2f, 0.3f);
            mgScene.attachChild(line);
            gridLines.add(line);
        }
        for (int y = 0; y <= Constants.MAP_HEIGHT; y += (int) GRID_SIZE) {
            float screenY = playfieldOffsetY + y * scaleY;
            Rectangle line = new Rectangle(playfieldOffsetX, screenY, playfieldWidth, 1);
            line.setColor(0.2f, 0.2f, 0.2f, 0.3f);
            mgScene.attachChild(line);
            gridLines.add(line);
        }
    }

    private void createTimeline() {
        float camWidth = engine.getCamera().getWidth();
        float timelineY = 0;
        float timelineH = WAVEFORM_HEIGHT + 30;

        timelineBg = new Rectangle(0, timelineY, camWidth, timelineH);
        timelineBg.setColor(0.15f, 0.15f, 0.15f);
        fgScene.attachChild(timelineBg);

        waveformBg = new Rectangle(0, timelineY, camWidth, WAVEFORM_HEIGHT);
        waveformBg.setColor(0.05f, 0.05f, 0.1f);
        fgScene.attachChild(waveformBg);

        renderBeatSnapLines();

        timelineCursor = new Rectangle(0, timelineY, PLAYBACK_BAR_WIDTH, timelineH);
        timelineCursor.setColor(1f, 0.3f, 0.3f);
        fgScene.attachChild(timelineCursor);

        Font font = getFont();
        if (font != null) {
            timeText = new ChangeableText(camWidth - 150, timelineY + WAVEFORM_HEIGHT + 5, font,
                    "00:00.000 / 00:00.000", 30);
            timeText.setColor(0.8f, 0.8f, 0.8f);
            fgScene.attachChild(timeText);
        }
    }

    private void createWaveform() {
        for (Rectangle bar : waveformBars) fgScene.detachChild(bar);
        waveformBars.clear();

        float camWidth = engine.getCamera().getWidth();
        int barCount = (int) (camWidth / 3);

        for (int i = 0; i < barCount; i++) {
            Rectangle bar = new Rectangle(i * 3f, 0, 2, 1);
            bar.setColor(0.3f, 0.5f, 0.8f);
            waveformBg.attachChild(bar);
            waveformBars.add(bar);
        }

        // Generate default waveform pattern (sine-based placeholder)
        generateDefaultWaveform();
    }

    private void generateDefaultWaveform() {
        int barCount = waveformBars.size();
        for (int i = 0; i < barCount; i++) {
            float t = (float) i / barCount;
            float amplitude = (float) (Math.sin(t * Math.PI * 8) * 0.3 + 0.5) * WAVEFORM_HEIGHT * 0.4f;
            amplitude = Math.max(amplitude, 2f);
            Rectangle bar = waveformBars.get(i);
            bar.setHeight(amplitude);
            bar.setPosition(bar.getX(), WAVEFORM_HEIGHT / 2f - amplitude / 2f);
        }
    }

    private void updateWaveform() {
        var svc = GlobalManager.getInstance().getSongService();
        float[] fft = svc != null ? svc.getSpectrum() : null;
        if (fft != null && fft.length > 0) {
            lastSpectrum = fft;
        }

        float[] displaySpectrum = lastSpectrum;
        if (displaySpectrum == null) return;

        for (int i = 0; i < waveformBars.size() && i < displaySpectrum.length; i++) {
            float amplitude = Math.min(Math.abs(displaySpectrum[i]) * 200f, WAVEFORM_HEIGHT);
            amplitude = Math.max(amplitude, 1f);
            Rectangle bar = waveformBars.get(i);
            bar.setPosition(bar.getX(), WAVEFORM_HEIGHT / 2f - amplitude / 2f);
            bar.setHeight(amplitude);
        }
    }

    private void renderBeatSnapLines() {
        for (Rectangle line : beatSnapLines) fgScene.detachChild(line);
        beatSnapLines.clear();

        if (beatmapData == null) return;

        double bpm = 120;
        if (!beatmapData.timingPoints.timing.getControlPoints().isEmpty()) {
            bpm = beatmapData.timingPoints.timing.getControlPoints().get(0).getBPM();
        }

        float snapMs = (float) (60000.0 / bpm * beatSnap);
        float camWidth = engine.getCamera().getWidth();
        float zoomedWidth = camWidth * waveformZoom;

        for (float t = 0; t <= totalDuration; t += snapMs) {
            float normalizedX = totalDuration > 0 ? t / totalDuration : 0;
            float x = (normalizedX * zoomedWidth) - waveformScrollX;
            if (x < -5 || x > camWidth + 5) continue; // Skip off-screen lines
            Rectangle line = new Rectangle(x, 0, 1, WAVEFORM_HEIGHT);
            line.setColor(0.3f, 0.3f, 0.3f, 0.5f);
            fgScene.attachChild(line);
            beatSnapLines.add(line);
        }
    }

    private void createPlaybackControls() {
        float camWidth = engine.getCamera().getWidth();
        float controlsY = WAVEFORM_HEIGHT + 35;
        float btnSize = 40;
        float spacing = 8;
        float totalWidth = btnSize * 8 + spacing * 7;
        float startX = (camWidth - totalWidth) / 2f;
        Font font = getFont();

        // Back
        Rectangle backBtn = new Rectangle(startX, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.8f, 0.3f, 0.3f); return true; }
                if (t.isActionUp()) { setColor(0.6f, 0.2f, 0.2f); back(); return true; }
                return false;
            }
        };
        backBtn.setColor(0.6f, 0.2f, 0.2f); addLabel(backBtn, "BACK", font, controlsY, startX, btnSize);
        fgScene.attachChild(backBtn); scene.registerTouchArea(backBtn);

        // Play
        Rectangle playBtn = new Rectangle(startX + (btnSize + spacing), controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.4f, 0.7f, 0.4f); return true; }
                if (t.isActionUp()) { setColor(0.3f, 0.5f, 0.3f); play(); return true; }
                return false;
            }
        };
        playBtn.setColor(0.3f, 0.5f, 0.3f); addLabel(playBtn, "PLAY", font, controlsY, startX + (btnSize + spacing), btnSize);
        fgScene.attachChild(playBtn); scene.registerTouchArea(playBtn);

        // Pause
        Rectangle pauseBtn = new Rectangle(startX + (btnSize + spacing) * 2, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.7f, 0.7f, 0.3f); return true; }
                if (t.isActionUp()) { setColor(0.5f, 0.5f, 0.2f); pause(); return true; }
                return false;
            }
        };
        pauseBtn.setColor(0.5f, 0.5f, 0.2f); addLabel(pauseBtn, "PAUSE", font, controlsY, startX + (btnSize + spacing) * 2, btnSize);
        fgScene.attachChild(pauseBtn); scene.registerTouchArea(pauseBtn);

        // Stop
        Rectangle stopBtn = new Rectangle(startX + (btnSize + spacing) * 3, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.7f, 0.3f, 0.3f); return true; }
                if (t.isActionUp()) { setColor(0.5f, 0.2f, 0.2f); stop(); return true; }
                return false;
            }
        };
        stopBtn.setColor(0.5f, 0.2f, 0.2f); addLabel(stopBtn, "STOP", font, controlsY, startX + (btnSize + spacing) * 3, btnSize);
        fgScene.attachChild(stopBtn); scene.registerTouchArea(stopBtn);

        // Rewind
        Rectangle rwBtn = new Rectangle(startX + (btnSize + spacing) * 4, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.5f, 0.5f, 0.7f); return true; }
                if (t.isActionUp()) { setColor(0.3f, 0.3f, 0.5f); seekTo(currentTime - 5000); return true; }
                return false;
            }
        };
        rwBtn.setColor(0.3f, 0.3f, 0.5f); addLabel(rwBtn, "<<", font, controlsY, startX + (btnSize + spacing) * 4, btnSize);
        fgScene.attachChild(rwBtn); scene.registerTouchArea(rwBtn);

        // Forward
        Rectangle fwBtn = new Rectangle(startX + (btnSize + spacing) * 5, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.5f, 0.5f, 0.7f); return true; }
                if (t.isActionUp()) { setColor(0.3f, 0.3f, 0.5f); seekTo(currentTime + 5000); return true; }
                return false;
            }
        };
        fwBtn.setColor(0.3f, 0.3f, 0.5f); addLabel(fwBtn, ">>", font, controlsY, startX + (btnSize + spacing) * 5, btnSize);
        fgScene.attachChild(fwBtn); scene.registerTouchArea(fwBtn);

        // Undo
        Rectangle undoBtn = new Rectangle(startX + (btnSize + spacing) * 6, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.6f, 0.6f, 0.6f); return true; }
                if (t.isActionUp()) { setColor(0.4f, 0.4f, 0.4f); undo(); return true; }
                return false;
            }
        };
        undoBtn.setColor(0.4f, 0.4f, 0.4f); addLabel(undoBtn, "UNDO", font, controlsY, startX + (btnSize + spacing) * 6, btnSize);
        fgScene.attachChild(undoBtn); scene.registerTouchArea(undoBtn);

        // Redo
        redoBtn = new Rectangle(startX + (btnSize + spacing) * 7, controlsY, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.6f, 0.6f, 0.6f); return true; }
                if (t.isActionUp()) { setColor(0.4f, 0.4f, 0.4f); redo(); return true; }
                return false;
            }
        };
        redoBtn.setColor(0.4f, 0.4f, 0.4f); addLabel(redoBtn, "REDO", font, controlsY, startX + (btnSize + spacing) * 7, btnSize);
        fgScene.attachChild(redoBtn); scene.registerTouchArea(redoBtn);
    }

    private void addLabel(Rectangle btn, String label, Font font, float controlsY, float btnX, float btnSize) {
        if (font == null) return;
        Text text = new Text(0, 0, font, label);
        text.setPosition(btnX + (btnSize - text.getWidth()) / 2f, controlsY + (btnSize * 0.6f - text.getHeight()) / 2f);
        text.setColor(1f, 1f, 1f);
        fgScene.attachChild(text);
    }

    private void createSettingsButton() {
        float camWidth = engine.getCamera().getWidth();
        float btnSize = 50;
        Font font = getFont();

        // Toolbar toggle button
        toolbarToggleBtn = new Rectangle(camWidth - btnSize * 2 - 20, 10, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.3f, 0.5f, 0.7f); return true; }
                if (t.isActionUp()) {
                    toggleToolbar();
                    return true;
                }
                return false;
            }
        };
        toolbarToggleBtn.setColor(0.3f, 0.5f, 0.7f);
        fgScene.attachChild(toolbarToggleBtn);
        scene.registerTouchArea(toolbarToggleBtn);

        if (font != null) {
            Text toolText = new Text(0, 0, font, "TOOL");
            toolText.setPosition(camWidth - btnSize * 2 - 20 + (btnSize - toolText.getWidth()) / 2f,
                    10 + (btnSize * 0.6f - toolText.getHeight()) / 2f);
            toolText.setColor(1f, 1f, 1f);
            fgScene.attachChild(toolText);
        }

        // Settings button
        Rectangle settingsBtn = new Rectangle(camWidth - btnSize - 10, 10, btnSize, btnSize * 0.6f) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.7f, 0.3f, 0.7f); return true; }
                if (t.isActionUp()) {
                    setColor(0.5f, 0.2f, 0.5f);
                    openSettings();
                    return true;
                }
                return false;
            }
        };
        settingsBtn.setColor(0.5f, 0.2f, 0.5f);
        fgScene.attachChild(settingsBtn);
        scene.registerTouchArea(settingsBtn);

        if (font != null) {
            Text text = new Text(0, 0, font, "MENU");
            text.setPosition(camWidth - btnSize - 10 + (btnSize - text.getWidth()) / 2f, 10 + (btnSize * 0.6f - text.getHeight()) / 2f);
            text.setColor(1f, 1f, 1f);
            fgScene.attachChild(text);
        }
    }

    private void openSettings() {
        final EditorScene self = this;
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditorSettingsFragment fragment = new EditorSettingsFragment();
                fragment.withEditor(self);
                fragment.show();
            }
        });
    }

    private void createGridSnapButton() {
        Font font = getFont();

        gridSnapBtn = new Rectangle(10, 10, 60, 30) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.3f, 0.5f, 0.3f); return true; }
                if (t.isActionUp()) {
                    gridSnapEnabled = !gridSnapEnabled;
                    toggleGridVisible();
                    updateGridSnapButton();
                    return true;
                }
                return false;
            }
        };
        gridSnapBtn.setColor(0.3f, 0.5f, 0.3f);
        fgScene.attachChild(gridSnapBtn);
        scene.registerTouchArea(gridSnapBtn);

        if (font != null) {
            gridSnapLabel = new Text(15, 15, font, "SNAP: ON");
            gridSnapLabel.setColor(1f, 1f, 1f);
            fgScene.attachChild(gridSnapLabel);
        }
    }

    private void updateGridSnapButton() {
        if (gridSnapBtn != null) {
            gridSnapBtn.setColor(gridSnapEnabled ? 0.3f : 0.5f,
                    gridSnapEnabled ? 0.5f : 0.3f,
                    gridSnapEnabled ? 0.3f : 0.3f);
        }
        if (gridSnapLabel != null) {
            fgScene.detachChild(gridSnapLabel);
            Font font = getFont();
            if (font != null) {
                gridSnapLabel = new Text(15, 15, font, "SNAP: " + (gridSnapEnabled ? "ON" : "OFF"));
                gridSnapLabel.setColor(1f, 1f, 1f);
                fgScene.attachChild(gridSnapLabel);
            }
        }
    }

    private void createBeatSnapButton() {
        float camWidth = engine.getCamera().getWidth();
        Font font = getFont();

        beatSnapBtn = new Rectangle(camWidth - 75, 48, 65, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.3f, 0.4f, 0.6f); return true; }
                if (t.isActionUp()) {
                    cycleBeatSnap();
                    return true;
                }
                return false;
            }
        };
        beatSnapBtn.setColor(0.3f, 0.4f, 0.6f);
        fgScene.attachChild(beatSnapBtn);
        scene.registerTouchArea(beatSnapBtn);

        if (font != null) {
            beatSnapLabel = new Text(camWidth - 72, 52, font, "1/1");
            beatSnapLabel.setColor(1f, 1f, 1f);
            fgScene.attachChild(beatSnapLabel);
        }
    }

    private void cycleBeatSnap() {
        beatSnapIndex = (beatSnapIndex + 1) % BEAT_SNAPS.length;
        beatSnap = BEAT_SNAPS[beatSnapIndex];
        renderBeatSnapLines();
        if (beatSnapLabel != null) {
            fgScene.detachChild(beatSnapLabel);
            Font font = getFont();
            if (font != null) {
                float camWidth = engine.getCamera().getWidth();
                beatSnapLabel = new Text(camWidth - 72, 52, font, BEAT_SNAP_LABELS[beatSnapIndex]);
                beatSnapLabel.setColor(1f, 1f, 1f);
                fgScene.attachChild(beatSnapLabel);
            }
        }
    }

    private void createBeatSkipButtons() {
        float camWidth = engine.getCamera().getWidth();
        Font font = getFont();

        prevBeatBtn = new Rectangle(80, 10, 40, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.5f, 0.5f, 0.3f); return true; }
                if (t.isActionUp()) { setColor(0.3f, 0.3f, 0.2f); skipToPrevBeat(); return true; }
                return false;
            }
        };
        prevBeatBtn.setColor(0.3f, 0.3f, 0.2f);
        fgScene.attachChild(prevBeatBtn);
        scene.registerTouchArea(prevBeatBtn);
        if (font != null) addLabel(prevBeatBtn, "|<", font, 10, 80, 40);

        nextBeatBtn = new Rectangle(125, 10, 40, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.5f, 0.5f, 0.3f); return true; }
                if (t.isActionUp()) { setColor(0.3f, 0.3f, 0.2f); skipToNextBeat(); return true; }
                return false;
            }
        };
        nextBeatBtn.setColor(0.3f, 0.3f, 0.2f);
        fgScene.attachChild(nextBeatBtn);
        scene.registerTouchArea(nextBeatBtn);
        if (font != null) addLabel(nextBeatBtn, ">|", font, 10, 125, 40);
    }

    private void createNudgeButtons() {
        Font font = getFont();

        nudgeBackBtn = new Rectangle(170, 10, 40, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.4f, 0.3f, 0.5f); return true; }
                if (t.isActionUp()) { setColor(0.2f, 0.2f, 0.3f); nudgeSelected(-1); return true; }
                return false;
            }
        };
        nudgeBackBtn.setColor(0.2f, 0.2f, 0.3f);
        fgScene.attachChild(nudgeBackBtn);
        scene.registerTouchArea(nudgeBackBtn);
        if (font != null) addLabel(nudgeBackBtn, "-T", font, 10, 170, 40);

        nudgeFwdBtn = new Rectangle(215, 10, 40, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.4f, 0.3f, 0.5f); return true; }
                if (t.isActionUp()) { setColor(0.2f, 0.2f, 0.3f); nudgeSelected(1); return true; }
                return false;
            }
        };
        nudgeFwdBtn.setColor(0.2f, 0.2f, 0.3f);
        fgScene.attachChild(nudgeFwdBtn);
        scene.registerTouchArea(nudgeFwdBtn);
        if (font != null) addLabel(nudgeFwdBtn, "+T", font, 10, 215, 40);
    }

    private void skipToPrevBeat() {
        if (beatmapData == null) return;
        double bpm = 120;
        if (!beatmapData.timingPoints.timing.getControlPoints().isEmpty()) {
            bpm = beatmapData.timingPoints.timing.getControlPoints().get(0).getBPM();
        }
        float snapMs = (float) (60000.0 / bpm * beatSnap);
        float prevBeat = Math.max(0, (float) (Math.floor(currentTime / snapMs) * snapMs - snapMs));
        seekTo(prevBeat);
    }

    private void skipToNextBeat() {
        if (beatmapData == null) return;
        double bpm = 120;
        if (!beatmapData.timingPoints.timing.getControlPoints().isEmpty()) {
            bpm = beatmapData.timingPoints.timing.getControlPoints().get(0).getBPM();
        }
        float snapMs = (float) (60000.0 / bpm * beatSnap);
        float nextBeat = (float) (Math.ceil(currentTime / snapMs) * snapMs + snapMs);
        seekTo(nextBeat);
    }

    private void nudgeSelected(int direction) {
        if (selectedObjectIndex < 0 || beatmapData == null) return;
        float nudgeMs = direction * (float) (60000.0 / 120.0 * beatSnap);
        List<HitObject> objects = beatmapData.hitObjects.getObjects();

        for (int idx : selectedObjects) {
            if (idx >= 0 && idx < objects.size()) {
                HitObject obj = objects.get(idx);
                // Create new object with shifted time
                HitObject shifted = cloneWithTimeOffset(obj, nudgeMs);
                if (shifted != null) {
                    beatmapData.hitObjects.remove(idx);
                    beatmapData.hitObjects.add(shifted);
                }
            }
        }
        // Handle single-selected object if not in multi-select set
        if (selectedObjectIndex >= 0 && selectedObjectIndex < objects.size()
                && !selectedObjects.contains(selectedObjectIndex)) {
            HitObject obj = objects.get(selectedObjectIndex);
            HitObject shifted = cloneWithTimeOffset(obj, nudgeMs);
            if (shifted != null) {
                beatmapData.hitObjects.remove(selectedObjectIndex);
                beatmapData.hitObjects.add(shifted);
            }
        }
        renderHitObjects();
    }

    // --- Redo ---

    private void redo() {
        if (redoStack.isEmpty()) return;

        EditorAction action = redoStack.pop();
        List<HitObject> objects = beatmapData.hitObjects.getObjects();

        switch (action.type) {
            case Add:
                if (action.hitObject != null) {
                    beatmapData.hitObjects.add(action.hitObject);
                    undoStack.push(new EditorAction(EditorAction.Type.Add, -1, action.hitObject));
                }
                break;
            case Delete:
                if (action.index >= 0 && action.index < objects.size()) {
                    HitObject removed = beatmapData.hitObjects.remove(action.index);
                    undoStack.push(new EditorAction(EditorAction.Type.Delete, action.index, removed));
                }
                break;
            case Move:
                if (action.index >= 0 && action.index < objects.size()) {
                    HitObject obj = objects.get(action.index);
                    float curX = (float) obj.getPosition().x;
                    float curY = (float) obj.getPosition().y;
                    obj.getPosition().x = action.oldX;
                    obj.getPosition().y = action.oldY;
                    EditorAction undo = new EditorAction(EditorAction.Type.Move, action.index, null);
                    undo.oldX = curX;
                    undo.oldY = curY;
                    undoStack.push(undo);
                }
                break;
        }

        selectedObjectIndex = -1;
        renderHitObjects();
    }

    // --- Grid Visibility Toggle ---

    private void toggleGridVisible() {
        gridVisible = !gridVisible;
        for (Rectangle line : gridLines) {
            line.setVisible(gridVisible);
        }
    }

    private void createZoomButtons() {
        Font font = getFont();

        zoomInBtn = new Rectangle(260, 10, 30, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.3f, 0.6f, 0.3f); return true; }
                if (t.isActionUp()) {
                    setColor(0.2f, 0.4f, 0.2f);
                    waveformZoom = Math.min(waveformZoom * 1.5f, 8f);
                    updateWaveformZoom();
                    return true;
                }
                return false;
            }
        };
        zoomInBtn.setColor(0.2f, 0.4f, 0.2f);
        fgScene.attachChild(zoomInBtn);
        scene.registerTouchArea(zoomInBtn);
        if (font != null) addLabel(zoomInBtn, "Z+", font, 10, 260, 30);

        zoomOutBtn = new Rectangle(295, 10, 30, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.6f, 0.3f, 0.3f); return true; }
                if (t.isActionUp()) {
                    setColor(0.4f, 0.2f, 0.2f);
                    waveformZoom = Math.max(waveformZoom / 1.5f, 0.5f);
                    updateWaveformZoom();
                    return true;
                }
                return false;
            }
        };
        zoomOutBtn.setColor(0.4f, 0.2f, 0.2f);
        fgScene.attachChild(zoomOutBtn);
        scene.registerTouchArea(zoomOutBtn);
        if (font != null) addLabel(zoomOutBtn, "Z-", font, 10, 295, 30);

        zoomResetBtn = new Rectangle(330, 10, 30, 28) {
            public boolean onAreaTouched(TouchEvent t, float lx, float ly) {
                if (t.isActionDown()) { setColor(0.5f, 0.5f, 0.5f); return true; }
                if (t.isActionUp()) {
                    setColor(0.3f, 0.3f, 0.3f);
                    waveformZoom = 1f;
                    waveformScrollX = 0f;
                    updateWaveformZoom();
                    return true;
                }
                return false;
            }
        };
        zoomResetBtn.setColor(0.3f, 0.3f, 0.3f);
        fgScene.attachChild(zoomResetBtn);
        scene.registerTouchArea(zoomResetBtn);
        if (font != null) addLabel(zoomResetBtn, "Z=", font, 10, 330, 30);
    }

    private void updateWaveformZoom() {
        // Re-render beat snap lines with zoom
        renderBeatSnapLines();
        // Re-render timing markers with zoom
        renderTimingMarkers();
    }

    private void showToolbar() {
        final EditorScene self = this;
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentToolbar = new EditorToolbarFragment();
                    currentToolbar.setEditorScene(self);
                    currentToolbar.show();
                    toolbarVisible = true;
                } catch (Exception e) {
                    // Toolbar may fail if fragment manager not ready
                }
            }
        });
    }

    private void hideToolbar() {
        if (currentToolbar != null && currentToolbar.isAdded()) {
            currentToolbar.dismiss();
            currentToolbar = null;
        }
        toolbarVisible = false;
    }

    private void toggleToolbar() {
        if (toolbarVisible) {
            hideToolbar();
        } else {
            showToolbar();
        }
    }

    // --- Hit Object Rendering ---

    private void renderHitObjects() {
        for (EditorObjectSprite spr : objectSprites) {
            if (spr.sprite != null) mgScene.detachChild(spr.sprite);
            if (spr.endSprite != null) mgScene.detachChild(spr.endSprite);
            for (Rectangle seg : spr.bodySegments) mgScene.detachChild(seg);
        }
        objectSprites.clear();

        if (beatmapData == null) return;

        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        Log.i("EditorScene", "Rendering " + objects.size() + " objects, radius=" + getCircleRadius());
        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;

        ArrayList<RGBColor> combos = getDefaultCombos();
        int comboIndex = 0;

        for (int i = 0; i < objects.size(); i++) {
            HitObject obj = objects.get(i);
            Vector2 pos = obj.getPosition();
            float screenX = playfieldOffsetX + (float) pos.x * scaleX;
            float screenY = playfieldOffsetY + (float) pos.y * scaleY;

            // Check for new combo flag
            Boolean isNewCombo = newComboFlags.get(i);
            if (isNewCombo != null && isNewCombo) {
                comboIndex++;
            }

            RGBColor color = combos.get(comboIndex % combos.size());

            if (obj instanceof HitCircle) {
                renderHitCircle(screenX, screenY, color, i, (float) obj.getStartTime());
            } else if (obj instanceof Slider) {
                renderSlider(screenX, screenY, (Slider) obj, color, i, scaleX, scaleY);
            } else if (obj instanceof Spinner) {
                renderSpinner(color, i, (float) obj.getStartTime());
            }

            // Draw new combo indicator (small triangle)
            if (isNewCombo != null && isNewCombo) {
                float size = 8;
                Rectangle indicator = new Rectangle(screenX - size / 2, screenY - getCircleRadius() - size - 4, size, size);
                indicator.setColor(1f, 1f, 0.3f);
                mgScene.attachChild(indicator);
            }
        }

        highlightObject(selectedObjectIndex);
    }

    private static final float OBJECT_VISIBLE_WINDOW_MS = 2000f;

    private void updateObjectVisibility() {
        float halfWindow = OBJECT_VISIBLE_WINDOW_MS / 2f;
        for (EditorObjectSprite spr : objectSprites) {
            float diff = Math.abs(spr.startTime - currentTime);
            boolean visible = diff <= halfWindow;
            for (Rectangle entity : spr.allEntities) {
                if (entity != null) entity.setVisible(visible);
            }
        }
    }

    private void renderHitCircle(float x, float y, RGBColor color, int index, float startTime) {
        float radius = getCircleRadius();

        // White outline
        float outlineRadius = radius + 2f;
        Rectangle outline = new Rectangle(x - outlineRadius, y - outlineRadius, outlineRadius * 2, outlineRadius * 2);
        outline.setColor(1f, 1f, 1f);
        mgScene.attachChild(outline);

        // Colored body
        Rectangle circle = new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
        circle.setColor(color.r(), color.g(), color.b());
        mgScene.attachChild(circle);

        // Inner approach circle
        float innerRadius = radius * 0.6f;
        Rectangle inner = new Rectangle(x - innerRadius, y - innerRadius, innerRadius * 2, innerRadius * 2);
        inner.setColor(1f, 1f, 1f, 0.5f);
        mgScene.attachChild(inner);

        EditorObjectSprite spr = new EditorObjectSprite();
        spr.sprite = circle;
        spr.objectIndex = index;
        spr.startTime = startTime;
        spr.allEntities.add(outline);
        spr.allEntities.add(circle);
        spr.allEntities.add(inner);
        objectSprites.add(spr);
    }

    private void renderSlider(float x, float y, Slider slider, RGBColor color, int index,
                              float scaleX, float scaleY) {
        SliderPath path = slider.getPath();
        EditorObjectSprite spr = new EditorObjectSprite();
        spr.objectIndex = index;
        spr.startTime = (float) slider.getStartTime();

        ArrayList<Vector2> calculatedPath = path.calculatedPath;
        float bodyRadius = getCircleRadius() * 0.35f;
        float outlineRadius = bodyRadius + 2f;

        // Render slider body as overlapping circles along the path
        for (int i = 0; i < calculatedPath.size(); i++) {
            Vector2 p = calculatedPath.get(i);
            float sx = playfieldOffsetX + (float) p.x * scaleX;
            float sy = playfieldOffsetY + (float) p.y * scaleY;

            // White outline circle
            Rectangle outlineDot = new Rectangle(sx - outlineRadius, sy - outlineRadius,
                    outlineRadius * 2, outlineRadius * 2);
            outlineDot.setColor(1f, 1f, 1f, 0.8f);
            mgScene.attachChild(outlineDot);
            spr.allEntities.add(outlineDot);

            // Colored body circle
            Rectangle bodyDot = new Rectangle(sx - bodyRadius, sy - bodyRadius,
                    bodyRadius * 2, bodyRadius * 2);
            bodyDot.setColor(color.r(), color.g(), color.b(), 0.85f);
            mgScene.attachChild(bodyDot);
            spr.bodySegments.add(bodyDot);
            spr.allEntities.add(bodyDot);
        }

        // Head circle with outline
        float radius = getCircleRadius() * 0.6f;
        float headOutlineRadius = radius + 2f;
        Rectangle headOutline = new Rectangle(x - headOutlineRadius, y - headOutlineRadius, headOutlineRadius * 2, headOutlineRadius * 2);
        headOutline.setColor(1f, 1f, 1f);
        mgScene.attachChild(headOutline);
        spr.allEntities.add(headOutline);

        Rectangle startCircle = new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
        startCircle.setColor(color.r(), color.g(), color.b());
        mgScene.attachChild(startCircle);
        spr.sprite = startCircle;
        spr.allEntities.add(startCircle);

        // End circle with outline
        Vector2 endPos = slider.getStackedEndPosition();
        float endX = playfieldOffsetX + (float) endPos.x * scaleX;
        float endY = playfieldOffsetY + (float) endPos.y * scaleY;
        Rectangle endOutline = new Rectangle(endX - headOutlineRadius, endY - headOutlineRadius, headOutlineRadius * 2, headOutlineRadius * 2);
        endOutline.setColor(1f, 1f, 1f, 0.7f);
        mgScene.attachChild(endOutline);
        spr.allEntities.add(endOutline);

        Rectangle endCircle = new Rectangle(endX - radius, endY - radius, radius * 2, radius * 2);
        endCircle.setColor(color.r(), color.g(), color.b(), 0.7f);
        mgScene.attachChild(endCircle);
        spr.endSprite = endCircle;
        spr.allEntities.add(endCircle);

        objectSprites.add(spr);
    }

    private void renderSpinner(RGBColor color, int index, float startTime) {
        float cx = playfieldOffsetX + playfieldWidth / 2f;
        float cy = playfieldOffsetY + playfieldHeight / 2f;
        float radius = Math.min(playfieldWidth, playfieldHeight) * 0.3f;

        // Outline
        Rectangle outlineCircle = new Rectangle(cx - radius - 2, cy - radius - 2, (radius + 2) * 2, (radius + 2) * 2);
        outlineCircle.setColor(1f, 1f, 1f, 0.6f);
        mgScene.attachChild(outlineCircle);

        Rectangle spinnerCircle = new Rectangle(cx - radius, cy - radius, radius * 2, radius * 2);
        spinnerCircle.setColor(color.r(), color.g(), color.b(), 0.4f);
        mgScene.attachChild(spinnerCircle);

        EditorObjectSprite spr = new EditorObjectSprite();
        spr.sprite = spinnerCircle;
        spr.objectIndex = index;
        spr.startTime = startTime;
        spr.allEntities.add(outlineCircle);
        spr.allEntities.add(spinnerCircle);
        objectSprites.add(spr);
    }

    private void renderTimingMarkers() {
        for (TimingMarker marker : timingMarkers) {
            fgScene.detachChild(marker.line);
            if (marker.label != null) fgScene.detachChild(marker.label);
        }
        timingMarkers.clear();

        if (beatmapData == null) return;

        float camWidth = engine.getCamera().getWidth();
        float zoomedWidth = camWidth * waveformZoom;
        Font font = getFont();

        List<TimingControlPoint> timingPoints = beatmapData.timingPoints.timing.getControlPoints();

        for (TimingControlPoint tp : timingPoints) {
            float t = (float) tp.time;
            float normalizedX = totalDuration > 0 ? t / totalDuration : 0;
            float x = (normalizedX * zoomedWidth) - waveformScrollX;
            if (x < -50 || x > camWidth + 50) continue;

            boolean isKiai = Boolean.TRUE.equals(kiaiFlags.get(tp.time));

            Rectangle line = new Rectangle(x, 0, isKiai ? 3 : 2, WAVEFORM_HEIGHT + 30);
            line.setColor(isKiai ? 1f : 1f, isKiai ? 0.3f : 0.8f, isKiai ? 0.3f : 0.2f, isKiai ? 1f : 0.8f);
            fgScene.attachChild(line);

            TimingMarker marker = new TimingMarker();
            marker.line = line;
            marker.time = t;
            marker.bpm = tp.getBPM();

            if (font != null) {
                String kiaiStr = isKiai ? " [KIAI]" : "";
                Text label = new Text(x + 4, WAVEFORM_HEIGHT + 5, font,
                        String.format("%.0f BPM%s", tp.getBPM(), kiaiStr));
                label.setColor(isKiai ? 1f : 1f, isKiai ? 0.4f : 0.8f, isKiai ? 0.4f : 0.2f);
                fgScene.attachChild(label);
                marker.label = label;
            }

            timingMarkers.add(marker);
        }
    }

    // --- Touch Handling ---

    private void setupTouchHandler() {
        scene.setOnAreaTouchListener((touchEvent, touchArea, x, y) -> handleTouch(touchEvent, x, y));
    }

    private boolean handleTouch(TouchEvent event, float x, float y) {
        // Timeline scrubbing
        if (y < WAVEFORM_HEIGHT + 30) {
            if (event.isActionDown() || event.isActionMove()) {
                currentTime = Math.max(0, Math.min((x / engine.getCamera().getWidth()) * totalDuration, totalDuration));
                var svc = GlobalManager.getInstance().getSongService();
                if (svc != null && svc.getStatus() == Status.PLAYING) {
                    svc.seekTo((int) currentTime);
                }
                updateTimelineCursor();
                updateTimeText();
                return true;
            }
            return false;
        }

        // Playfield
        if (x >= playfieldOffsetX && x <= playfieldOffsetX + playfieldWidth &&
                y >= playfieldOffsetY && y <= playfieldOffsetY + playfieldHeight) {
            switch (currentTool) {
                case Select: return handleSelectTool(event, x, y);
                case Circle: return handleCircleTool(event, x, y);
                case Slider: return handleSliderTool(event, x, y);
                case Spinner: return handleSpinnerTool(event, x, y);
                case Delete: return handleDeleteTool(event, x, y);
                case TimingAdd: return handleTimingAddTool(event, x, y);
                case TimingDelete: return handleTimingDeleteTool(event, x, y);
            }
        }
        return false;
    }

    private boolean handleSelectTool(TouchEvent event, float x, float y) {
        if (event.isActionDown()) {
            int idx = findObjectAt(x, y);
            if (idx >= 0) {
                if (isMultiSelecting) {
                    // Toggle object in selection
                    if (selectedObjects.contains(idx)) {
                        selectedObjects.remove(idx);
                    } else {
                        selectedObjects.add(idx);
                    }
                    selectedObjectIndex = idx;
                } else {
                    selectedObjects.clear();
                    selectedObjects.add(idx);
                    selectedObjectIndex = idx;
                }
                isDragging = true;
                dragStartX = x;
                dragStartY = y;
                HitObject obj = beatmapData.hitObjects.getObjects().get(idx);
                dragObjStartX = (float) obj.getPosition().x;
                dragObjStartY = (float) obj.getPosition().y;
                highlightObject(idx);
            } else {
                selectedObjects.clear();
                selectedObjectIndex = -1;
                highlightObject(-1);
            }
            return true;
        }

        if (event.isActionMove() && isDragging && selectedObjectIndex >= 0) {
            float scaleX = playfieldWidth / Constants.MAP_WIDTH;
            float scaleY = playfieldHeight / Constants.MAP_HEIGHT;
            float dx = (x - dragStartX) / scaleX;
            float dy = (y - dragStartY) / scaleY;

            // Move all selected objects
            for (int idx : selectedObjects) {
                HitObject obj = beatmapData.hitObjects.getObjects().get(idx);
                float startX = (float) obj.getPosition().x;
                float startY = (float) obj.getPosition().y;
                if (idx == selectedObjectIndex) {
                    obj.getPosition().x = Math.max(0, Math.min(dragObjStartX + dx, Constants.MAP_WIDTH));
                    obj.getPosition().y = Math.max(0, Math.min(dragObjStartY + dy, Constants.MAP_HEIGHT));
                }
            }
            renderHitObjects();
            return true;
        }

        if (event.isActionUp() && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    private float clampTrackX(float x) { return Math.max(0, Math.min(x, Constants.MAP_WIDTH)); }
    private float clampTrackY(float y) { return Math.max(0, Math.min(y, Constants.MAP_HEIGHT)); }

    private boolean handleCircleTool(TouchEvent event, float x, float y) {
        if (event.isActionUp()) {
            float scaleX = playfieldWidth / Constants.MAP_WIDTH;
            float scaleY = playfieldHeight / Constants.MAP_HEIGHT;
            float trackX = snapToGrid(clampTrackX((x - playfieldOffsetX) / scaleX));
            float trackY = snapToGrid(clampTrackY((y - playfieldOffsetY) / scaleY));

            HitCircle circle = new HitCircle(currentTime, trackX, trackY);
            beatmapData.hitObjects.add(circle);
            pushUndo(new EditorAction(EditorAction.Type.Add, -1, circle));
            selectedObjectIndex = beatmapData.hitObjects.getObjects().size() - 1;
            renderHitObjects();
            return true;
        }
        return event.isActionDown();
    }

    private boolean handleSliderTool(TouchEvent event, float x, float y) {
        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;
        float trackX = clampTrackX((x - playfieldOffsetX) / scaleX);
        float trackY = clampTrackY((y - playfieldOffsetY) / scaleY);

        if (event.isActionDown()) {
            isCreatingSlider = true;
            sliderStartX = snapToGrid(trackX);
            sliderStartY = snapToGrid(trackY);
            return true;
        }

        if (event.isActionUp() && isCreatingSlider) {
            isCreatingSlider = false;
            float endX = snapToGrid(trackX);
            float endY = snapToGrid(trackY);

            float dx = endX - sliderStartX;
            float dy = endY - sliderStartY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 10) return true;

            ArrayList<Vector2> controlPoints = new ArrayList<>();
            controlPoints.add(new Vector2(sliderStartX, sliderStartY));
            controlPoints.add(new Vector2(endX, endY));

            SliderPath path = new SliderPath(SliderPathType.Linear, controlPoints, dist);

            TimingControlPoint timing = getTimingControlPoint(currentTime);
            DifficultyControlPoint difficulty = getDifficultyControlPoint(currentTime);

            double sliderVelocity = beatmapData.difficulty.sliderMultiplier;
            double tickRate = beatmapData.difficulty.sliderTickRate;

            Slider slider = new Slider(currentTime, new Vector2(sliderStartX, sliderStartY),
                    timing, difficulty, 1, path, sliderVelocity, tickRate, 1.0, true);

            beatmapData.hitObjects.add(slider);
            pushUndo(new EditorAction(EditorAction.Type.Add, -1, slider));
            selectedObjectIndex = beatmapData.hitObjects.getObjects().size() - 1;
            renderHitObjects();
            return true;
        }
        return false;
    }

    private boolean handleSpinnerTool(TouchEvent event, float x, float y) {
        if (event.isActionUp()) {
            Spinner spinner = new Spinner(currentTime, currentTime + 5000);
            beatmapData.hitObjects.add(spinner);
            pushUndo(new EditorAction(EditorAction.Type.Add, -1, spinner));
            selectedObjectIndex = beatmapData.hitObjects.getObjects().size() - 1;
            renderHitObjects();
            return true;
        }
        return event.isActionDown();
    }

    private boolean handleDeleteTool(TouchEvent event, float x, float y) {
        if (event.isActionUp()) {
            int idx = findObjectAt(x, y);
            if (idx >= 0) {
                HitObject removed = beatmapData.hitObjects.remove(idx);
                if (removed != null) {
                    pushUndo(new EditorAction(EditorAction.Type.Delete, idx, removed));
                }
                selectedObjectIndex = -1;
                renderHitObjects();
            }
            return true;
        }
        return event.isActionDown();
    }

    private boolean handleTimingAddTool(TouchEvent event, float x, float y) {
        if (event.isActionUp()) {
            // Add a new timing point at the current time
            TimingControlPoint existing = getTimingControlPoint(currentTime);
            double msPerBeat = existing != null ? existing.msPerBeat : 60000.0 / 120.0;
            int sig = existing != null ? existing.timeSignature : 4;

            TimingControlPoint tp = new TimingControlPoint(currentTime, msPerBeat, sig);
            beatmapData.timingPoints.timing.add(tp);

            DifficultyControlPoint dp = new DifficultyControlPoint(currentTime, 1.0, true);
            beatmapData.timingPoints.difficulty.add(dp);

            renderTimingMarkers();
            return true;
        }
        return event.isActionDown();
    }

    private boolean handleTimingDeleteTool(TouchEvent event, float x, float y) {
        if (event.isActionUp()) {
            // Find the nearest timing point and remove it
            var points = beatmapData.timingPoints.timing.getControlPoints();
            if (points.size() <= 1) return true; // Always keep at least one

            int nearestIdx = -1;
            float nearestDist = Float.MAX_VALUE;
            for (int i = 0; i < points.size(); i++) {
                float t = (float) points.get(i).time;
                float screenX = totalDuration > 0 ? (t / totalDuration) * engine.getCamera().getWidth() : 0;
                float dx = x - screenX;
                if (Math.abs(dx) < nearestDist) {
                    nearestDist = Math.abs(dx);
                    nearestIdx = i;
                }
            }

            if (nearestIdx >= 0 && nearestDist < 30) {
                beatmapData.timingPoints.timing.remove(points.get(nearestIdx));
                renderTimingMarkers();
            }
            return true;
        }
        return event.isActionDown();
    }

    private int findObjectAt(float screenX, float screenY) {
        float scaleX = playfieldWidth / Constants.MAP_WIDTH;
        float scaleY = playfieldHeight / Constants.MAP_HEIGHT;
        float trackX = (screenX - playfieldOffsetX) / scaleX;
        float trackY = (screenY - playfieldOffsetY) / scaleY;

        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        for (int i = objects.size() - 1; i >= 0; i--) {
            Vector2 pos = objects.get(i).getPosition();
            float dx = trackX - (float) pos.x;
            float dy = trackY - (float) pos.y;
            if (Math.sqrt(dx * dx + dy * dy) <= 64) return i;
        }
        return -1;
    }

    private float snapToGrid(float value) {
        if (!gridSnapEnabled) return value;
        return Math.round(value / GRID_SIZE) * GRID_SIZE;
    }

    private TimingControlPoint getTimingControlPoint(double time) {
        var points = beatmapData.timingPoints.timing.getControlPoints();
        for (int i = points.size() - 1; i >= 0; i--) {
            if (time >= points.get(i).time) return points.get(i);
        }
        return points.isEmpty() ? new TimingControlPoint(0, 60000.0 / 120.0, 4) : points.get(0);
    }

    private DifficultyControlPoint getDifficultyControlPoint(double time) {
        var points = beatmapData.timingPoints.difficulty.getControlPoints();
        for (int i = points.size() - 1; i >= 0; i--) {
            if (time >= points.get(i).time) return points.get(i);
        }
        return points.isEmpty() ? new DifficultyControlPoint(0, 1.0, true) : points.get(0);
    }

    private void highlightObject(int index) {
        for (EditorObjectSprite spr : objectSprites) {
            if (spr.sprite != null) {
                if (selectedObjects.contains(spr.objectIndex)) {
                    spr.sprite.setAlpha(1.0f);
                    spr.sprite.setColor(1f, 1f, 0.5f);
                } else if (spr.objectIndex == index) {
                    spr.sprite.setAlpha(1.0f);
                } else {
                    spr.sprite.setAlpha(1.0f);
                }
            }
        }
    }

    // --- Undo/Redo ---

    private void pushUndo(EditorAction action) {
        undoStack.push(action);
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;

        EditorAction action = undoStack.pop();
        List<HitObject> objects = beatmapData.hitObjects.getObjects();

        switch (action.type) {
            case Add:
                if (!objects.isEmpty()) {
                    HitObject removed = beatmapData.hitObjects.remove(objects.size() - 1);
                    redoStack.push(new EditorAction(EditorAction.Type.Add, -1, removed));
                }
                break;
            case Delete:
                if (action.index >= 0 && action.index <= objects.size()) {
                    beatmapData.hitObjects.add(action.hitObject);
                    redoStack.push(new EditorAction(EditorAction.Type.Delete, action.index, null));
                }
                break;
            case Move:
                if (action.index >= 0 && action.index < objects.size()) {
                    HitObject obj = objects.get(action.index);
                    float curX = (float) obj.getPosition().x;
                    float curY = (float) obj.getPosition().y;
                    obj.getPosition().x = action.oldX;
                    obj.getPosition().y = action.oldY;
                    EditorAction redo = new EditorAction(EditorAction.Type.Move, action.index, null);
                    redo.oldX = curX;
                    redo.oldY = curY;
                    redoStack.push(redo);
                }
                break;
        }

        selectedObjectIndex = -1;
        renderHitObjects();
    }

    // --- Timeline ---

    private void updateTimelineCursor() {
        if (timelineCursor != null) {
            float camWidth = engine.getCamera().getWidth();
            float zoomedWidth = camWidth * waveformZoom;
            float normalizedX = totalDuration > 0 ? currentTime / totalDuration : 0;
            float x = (normalizedX * zoomedWidth) - waveformScrollX;
            x = Math.max(0, Math.min(x, camWidth));
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
        return String.format("%02d:%02d.%03d", totalSec / 60, totalSec % 60, (int) (ms % 1000));
    }

    private float getCircleRadius() {
        float cs = beatmapData != null ? beatmapData.difficulty.cs : 5;
        return playfieldWidth / 16f * (5f / cs);
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
        try { return ResourceManager.getInstance().getFont("smallFont"); }
        catch (Exception e) { return null; }
    }

    // --- Public API ---

    public Scene getScene() { return scene; }

    public void play() {
        var svc = GlobalManager.getInstance().getSongService();
        if (svc != null) {
            if (svc.getStatus() == Status.PAUSED) {
                svc.play();
            } else {
                svc.play();
                svc.setVolume(Config.getBgmVolume());
            }
            totalDuration = svc.getLength();
        }
        isPlaying = true;
    }

    public void pause() {
        var svc = GlobalManager.getInstance().getSongService();
        if (svc != null && svc.getStatus() == Status.PLAYING) svc.pause();
        isPlaying = false;
    }

    public void stop() {
        var svc = GlobalManager.getInstance().getSongService();
        if (svc != null) svc.stop();
        isPlaying = false;
        currentTime = 0f;
        updateTimelineCursor();
        updateTimeText();
    }

    public void seekTo(float timeMs) {
        currentTime = Math.max(0, Math.min(timeMs, totalDuration));
        var svc = GlobalManager.getInstance().getSongService();
        if (svc != null && svc.getStatus() != Status.STOPPED) svc.seekTo((int) currentTime);
        updateTimelineCursor();
        updateTimeText();
    }

    public void setCurrentTool(EditorTool tool) { this.currentTool = tool; }
    public EditorTool getCurrentTool() { return currentTool; }
    public BeatmapData getBeatmapData() { return beatmapData; }
    public String getBeatmapPath() { return beatmapPath; }
    public float getCurrentTime() { return currentTime; }
    public int getSelectedObjectIndex() { return selectedObjectIndex; }

    public void deleteSelectedObject() {
        if (selectedObjectIndex >= 0 && beatmapData != null) {
            HitObject removed = beatmapData.hitObjects.remove(selectedObjectIndex);
            if (removed != null) {
                pushUndo(new EditorAction(EditorAction.Type.Delete, selectedObjectIndex, removed));
            }
            selectedObjectIndex = -1;
            renderHitObjects();
        }
    }

    public void refresh() {
        renderHitObjects();
        renderTimingMarkers();
        renderBeatSnapLines();
    }

    public enum EditorTool {
        Select, Circle, Slider, Spinner, Delete, TimingAdd, TimingDelete
    }

    private static class EditorObjectSprite {
        Rectangle sprite;
        Rectangle endSprite;
        ArrayList<Rectangle> bodySegments = new ArrayList<>();
        ArrayList<Rectangle> allEntities = new ArrayList<>();
        int objectIndex;
        float startTime;
    }

    private static class TimingMarker {
        Rectangle line;
        Text label;
        float time;
        double bpm;
    }

    private static class EditorAction {
        enum Type { Add, Delete, Move }
        Type type;
        int index;
        HitObject hitObject;
        float oldX, oldY;
        // For multi-select move: store old positions of all selected objects
        ArrayList<int[]> multiMoveOldPositions; // [index, oldX, oldY]

        EditorAction(Type type, int index, HitObject hitObject) {
            this.type = type;
            this.index = index;
            this.hitObject = hitObject;
        }
    }

    // --- IUpdateHandler ---

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (isPlaying) {
            var svc = GlobalManager.getInstance().getSongService();
            if (svc != null && svc.getStatus() == Status.PLAYING) {
                currentTime = svc.getPosition();
            } else {
                currentTime += pSecondsElapsed * 1000f;
                if (currentTime >= totalDuration) { currentTime = totalDuration; stop(); }
            }
            updateTimelineCursor();
            updateTimeText();
        }
        updateObjectVisibility();
        // Always update waveform (preserves last FFT data when paused)
        updateWaveform();
    }

    @Override
    public void reset() {
        currentTime = 0f;
        isPlaying = false;
    }

    public void show() {
        scene.registerUpdateHandler(this);
        engine.setScene(scene);
    }

    public void deleteSelectedObjects() {
        if (selectedObjects.isEmpty() && selectedObjectIndex < 0) return;
        List<HitObject> objects = beatmapData.hitObjects.getObjects();

        // Collect indices to delete (sort descending to avoid index shifting)
        ArrayList<Integer> toDelete = new ArrayList<>(selectedObjects);
        if (selectedObjectIndex >= 0 && !toDelete.contains(selectedObjectIndex)) {
            toDelete.add(selectedObjectIndex);
        }
        java.util.Collections.sort(toDelete, java.util.Collections.reverseOrder());

        for (int idx : toDelete) {
            if (idx >= 0 && idx < objects.size()) {
                HitObject removed = beatmapData.hitObjects.remove(idx);
                if (removed != null) {
                    pushUndo(new EditorAction(EditorAction.Type.Delete, idx, removed));
                }
            }
        }
        selectedObjects.clear();
        selectedObjectIndex = -1;
        renderHitObjects();
    }

    public void copySelected() {
        clipboard.clear();
        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        for (int idx : selectedObjects) {
            if (idx >= 0 && idx < objects.size()) {
                clipboard.add(objects.get(idx).deepClone());
            }
        }
        if (selectedObjectIndex >= 0 && selectedObjectIndex < objects.size()) {
            boolean alreadyCopied = false;
            for (HitObject obj : clipboard) {
                if (obj == objects.get(selectedObjectIndex)) { alreadyCopied = true; break; }
            }
            if (!alreadyCopied) {
                clipboard.add(objects.get(selectedObjectIndex).deepClone());
            }
        }
    }

    public void pasteClipboard() {
        if (clipboard.isEmpty()) return;
        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        float maxTime = 0;
        for (HitObject obj : objects) {
            if (obj.getStartTime() > maxTime) maxTime = (float) obj.getStartTime();
        }
        float offset = maxTime + 100 - (clipboard.isEmpty() ? 0 : (float) clipboard.get(0).getStartTime());

        for (HitObject obj : clipboard) {
            // Create new object at shifted time
            HitObject clone = cloneWithTimeOffset(obj, offset);
            if (clone != null) {
                beatmapData.hitObjects.add(clone);
            }
        }
        renderHitObjects();
    }

    private HitObject cloneWithTimeOffset(HitObject obj, float timeOffset) {
        if (obj instanceof HitCircle) {
            return new HitCircle(obj.getStartTime() + timeOffset, obj.getPosition().x, obj.getPosition().y);
        } else if (obj instanceof Slider) {
            Slider s = (Slider) obj;
            ArrayList<Vector2> cps = new ArrayList<>(s.getPath().controlPoints);
            SliderPath path = new SliderPath(s.getPath().pathType, cps, s.getPath().expectedDistance);
            TimingControlPoint timing = getTimingControlPoint(obj.getStartTime() + timeOffset);
            DifficultyControlPoint diff = getDifficultyControlPoint(obj.getStartTime() + timeOffset);
            return new Slider(obj.getStartTime() + timeOffset, new Vector2(obj.getPosition().x, obj.getPosition().y),
                    timing, diff, s.getRepeatCount(), path, beatmapData.difficulty.sliderMultiplier,
                    beatmapData.difficulty.sliderTickRate, 1.0, true);
        } else if (obj instanceof Spinner) {
            return new Spinner(obj.getStartTime() + timeOffset, ((Spinner) obj).getEndTime() + timeOffset);
        }
        return null;
    }

    public void toggleMultiSelect() {
        isMultiSelecting = !isMultiSelecting;
        if (!isMultiSelecting) {
            selectedObjects.clear();
        }
    }

    public void selectAll() {
        if (beatmapData == null) return;
        selectedObjects.clear();
        List<HitObject> objects = beatmapData.hitObjects.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            selectedObjects.add(i);
        }
        if (!objects.isEmpty()) selectedObjectIndex = objects.size() - 1;
        renderHitObjects();
    }

    public void deselectAll() {
        selectedObjects.clear();
        selectedObjectIndex = -1;
        renderHitObjects();
    }

    public void openComboEditor() {
        final EditorScene self = this;
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditorComboFragment fragment = new EditorComboFragment();
                fragment.withEditor(self);
                fragment.show();
            }
        });
    }

    public void openSliderEditor() {
        if (selectedObjectIndex < 0 || beatmapData == null) return;
        HitObject obj = beatmapData.hitObjects.getObjects().get(selectedObjectIndex);
        if (!(obj instanceof Slider)) return;

        final EditorScene self = this;
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditorSliderFragment fragment = new EditorSliderFragment();
                fragment.withEditor(self);
                fragment.show();
            }
        });
    }

    public void openSpinnerEditor() {
        if (selectedObjectIndex < 0 || beatmapData == null) return;
        HitObject obj = beatmapData.hitObjects.getObjects().get(selectedObjectIndex);
        if (!(obj instanceof Spinner)) return;

        final EditorScene self = this;
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditorSpinnerFragment fragment = new EditorSpinnerFragment();
                fragment.withEditor(self);
                fragment.show();
            }
        });
    }

    public boolean isGridSnapEnabled() { return gridSnapEnabled; }
    public void setGridSnapEnabled(boolean enabled) { this.gridSnapEnabled = enabled; updateGridSnapButton(); }
    public void toggleGridSnap() { gridSnapEnabled = !gridSnapEnabled; updateGridSnapButton(); }
    public boolean isMultiSelectMode() { return isMultiSelecting; }
    public HashSet<Integer> getSelectedObjects() { return selectedObjects; }
    public HashMap<Integer, Boolean> getNewComboFlags() { return newComboFlags; }
    public HashMap<Double, Boolean> getKiaiFlags() { return kiaiFlags; }
    public void setComboColorCount(int count) { this.comboColorCount = count; }
    public int getComboColorCount() { return comboColorCount; }

    public void testPlay() {
        if (beatmapData == null || beatmapPath == null) return;

        final EditorScene self = this;
        GlobalManager.getInstance().getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Save current state to disk first
                    java.io.File file = new java.io.File(beatmapPath);
                    BeatmapEncoder.encode(beatmapData, file, kiaiFlags);

                    ru.nsu.ccfit.zuev.osu.menu.SongMenu menu = GlobalManager.getInstance().getSongMenu();
                    if (menu != null) {
                        ru.nsu.ccfit.zuev.osu.TrackInfo track = menu.getSelectedTrack();
                        if (track == null) {
                            track = GlobalManager.getInstance().getSelectedTrack();
                        }
                        if (track != null) {
                            stop();
                            scene.unregisterUpdateHandler(self);
                            menu.game.startGame(track, null);
                        } else {
                            ToastLogger.showText("No track selected for test play", true);
                        }
                    }
                } catch (Exception e) {
                    ToastLogger.showText("Test play failed: " + e.getMessage(), true);
                }
            }
        });
    }

    public void back() {
        scene.unregisterUpdateHandler(this);
        stop();
        GlobalManager manager = GlobalManager.getInstance();
        hideToolbar();
        manager.setEditorScene(null);
        if (manager.getSongMenu() != null) {
            manager.getSongMenu().isEditorMode = true;
            manager.getSongMenu().show();
        } else {
            manager.getMainScene().show();
        }
    }
}
