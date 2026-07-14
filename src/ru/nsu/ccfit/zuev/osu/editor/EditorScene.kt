package ru.nsu.ccfit.zuev.osu.editor

import android.util.Log
import com.rian.difficultycalculator.beatmap.hitobject.HitCircle
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath
import com.rian.difficultycalculator.beatmap.hitobject.SliderPathType
import com.rian.difficultycalculator.beatmap.hitobject.Spinner
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint
import com.rian.difficultycalculator.math.Vector2
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import java.util.Stack

class EditorScene(private val engine: Engine) : IUpdateHandler {

    private val scene: Scene = Scene()
    private lateinit var bgScene: Scene
    private lateinit var mgScene: Scene
    private lateinit var fgScene: Scene

    var beatmapData: BeatmapData? = null
        private set
    var beatmapPath: String? = null
        private set
    private var audioPath: String? = null

    private lateinit var playfieldBg: Rectangle
    private lateinit var playfieldBorder: Rectangle
    private val gridLines = ArrayList<Rectangle>()
    private var playfieldOffsetX = 0f
    private var playfieldOffsetY = 0f
    private var playfieldWidth = 0f
    private var playfieldHeight = 0f

    private lateinit var timelineBg: Rectangle
    private lateinit var timelineCursor: Rectangle
    private var timeText: ChangeableText? = null
    private val beatSnapLines = ArrayList<Rectangle>()
    private val timingMarkers = ArrayList<TimingMarker>()

    private lateinit var waveformBg: Rectangle
    private val waveformBars = ArrayList<Rectangle>()

    private val objectSprites = ArrayList<EditorObjectSprite>()

    private var isPlaying = false
    var currentTime = 0f
        private set
    private var totalDuration = 0f
    private var beatSnap = 1f
    var selectedObjectIndex = -1
        private set
    var currentTool: EditorTool = EditorTool.Select

    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f

    private val undoStack = Stack<EditorAction>()
    private val redoStack = Stack<EditorAction>()

    private var isCreatingSlider = false
    private var sliderStartX = 0f
    private var sliderStartY = 0f

    var isGridSnapEnabled = true
        private set
    fun toggleGridSnap() { isGridSnapEnabled = !isGridSnapEnabled; updateGridSnapButton() }
    private lateinit var gridSnapBtn: Rectangle
    private var gridSnapLabel: Text? = null

    val selectedObjects = HashSet<Int>()
    private var isMultiSelecting = false

    private val dragStartPositions = HashMap<Int, FloatArray>()

    private val clipboard = ArrayList<HitObject>()

    private var lastSpectrum: FloatArray? = null

    val newComboFlags = HashMap<Int, Boolean>()
    var comboColorCount = 0

    val kiaiFlags = HashMap<Double, Boolean>()

    private var isEditingSlider = false
    private var editingSliderPointIndex = -1

    private var gridVisible = true
    private lateinit var gridToggleBtn: Rectangle

    private var beatSnapIndex = 0
    private lateinit var beatSnapBtn: Rectangle
    private var beatSnapLabel: Text? = null

    private lateinit var prevBeatBtn: Rectangle
    private lateinit var nextBeatBtn: Rectangle
    private lateinit var nudgeBackBtn: Rectangle
    private lateinit var nudgeFwdBtn: Rectangle
    private lateinit var redoBtn: Rectangle

    private var waveformZoom = 1f
    private var waveformScrollX = 0f
    private lateinit var zoomInBtn: Rectangle
    private lateinit var zoomOutBtn: Rectangle
    private lateinit var zoomResetBtn: Rectangle

    private var currentToolbar: EditorToolbarFragment? = null
    private var toolbarVisible = true
    private lateinit var toolbarToggleBtn: Rectangle

    init {
        bgScene = Scene()
        mgScene = Scene()
        fgScene = Scene()

        scene.attachChild(bgScene)
        scene.attachChild(mgScene)
        scene.attachChild(fgScene)

        bgScene.setBackgroundEnabled(false)
        mgScene.setBackgroundEnabled(false)
        fgScene.setBackgroundEnabled(false)

        setupTouchHandler()
    }

    fun loadBeatmap(data: BeatmapData, path: String) {
        this.beatmapData = data
        this.beatmapPath = path
        undoStack.clear()
        redoStack.clear()
        newComboFlags.clear()
        kiaiFlags.clear()

        if (data.rawHitObjects != null) {
            for (i in data.rawHitObjects.indices) {
                val pars = data.rawHitObjects[i].split(",")
                if (pars.size >= 4) {
                    try {
                        val typeBits = pars[3].trim().toInt()
                        if (typeBits and 4 != 0) {
                            newComboFlags[i] = true
                        }
                    } catch (_: NumberFormatException) {}
                }
            }
        }

        if (!data.general.audioFilename.isNullOrEmpty()) {
            audioPath = "${data.folder}/${data.general.audioFilename}"
        }

        totalDuration = data.getDuration().toFloat()
        currentTime = 0f

        if (audioPath != null) {
            val svc = GlobalManager.getInstance().songService
            if (svc != null) {
                svc.preLoad(audioPath!!)
                svc.setVolume(Config.getBgmVolume())
                totalDuration = svc.length.toFloat()
            }
        }

        calculatePlayfield()
        Log.i("EditorScene", "Playfield: ${playfieldWidth}x$playfieldHeight offset=$playfieldOffsetX,$playfieldOffsetY")
        Log.i("EditorScene", "Objects: ${data.hitObjects.objects.size}")
        createPlayfield()
        createTimeline()
        createWaveform()
        createPlaybackControls()
        createSettingsButton()
        createGridSnapButton()
        createBeatSnapButton()
        createBeatSkipButtons()
        createNudgeButtons()
        createZoomButtons()
        renderHitObjects()
        renderTimingMarkers()
        showToolbar()
    }

    private fun calculatePlayfield() {
        val camera = engine.camera
        val camWidth = camera.width
        val camHeight = camera.height
        val resH = Config.getRES_HEIGHT()

        val topMargin = WAVEFORM_HEIGHT + 10
        val bottomMargin = 70f
        val availableHeight = camHeight - topMargin - bottomMargin

        playfieldHeight = if (resH > 0) minOf(resH * 0.85f, availableHeight) else availableHeight
        playfieldWidth = playfieldHeight / 3f * 4f

        if (playfieldWidth > camWidth * 0.95f) {
            playfieldWidth = camWidth * 0.95f
            playfieldHeight = playfieldWidth * 3f / 4f
        }

        if (playfieldWidth <= 0 || playfieldHeight <= 0) {
            playfieldWidth = camWidth * 0.8f
            playfieldHeight = playfieldWidth * 3f / 4f
        }

        playfieldOffsetX = (camWidth - playfieldWidth) / 2f
        playfieldOffsetY = topMargin + (availableHeight - playfieldHeight) / 2f
    }

    private fun createPlayfield() {
        playfieldBg = Rectangle(playfieldOffsetX, playfieldOffsetY, playfieldWidth, playfieldHeight)
        playfieldBg.setColor(0.1f, 0.1f, 0.1f)
        mgScene.attachChild(playfieldBg)

        playfieldBorder = Rectangle(playfieldOffsetX - 1, playfieldOffsetY - 1, playfieldWidth + 2, playfieldHeight + 2)
        playfieldBorder.setColor(0.4f, 0.4f, 0.4f)
        mgScene.attachChild(playfieldBorder)

        createGridLines()
    }

    private fun createGridLines() {
        for (line in gridLines) mgScene.detachChild(line)
        gridLines.clear()

        val scaleX = playfieldWidth / Constants.MAP_WIDTH
        val scaleY = playfieldHeight / Constants.MAP_HEIGHT

        var x = 0
        while (x <= Constants.MAP_WIDTH) {
            val screenX = playfieldOffsetX + x * scaleX
            val line = Rectangle(screenX, playfieldOffsetY, 1f, playfieldHeight)
            line.setColor(0.2f, 0.2f, 0.2f, 0.3f)
            mgScene.attachChild(line)
            gridLines.add(line)
            x += GRID_SIZE.toInt()
        }
        var y = 0
        while (y <= Constants.MAP_HEIGHT) {
            val screenY = playfieldOffsetY + y * scaleY
            val line = Rectangle(playfieldOffsetX, screenY, playfieldWidth, 1f)
            line.setColor(0.2f, 0.2f, 0.2f, 0.3f)
            mgScene.attachChild(line)
            gridLines.add(line)
            y += GRID_SIZE.toInt()
        }
    }

    private fun createTimeline() {
        val camWidth = engine.camera.width
        val timelineY = 0f
        val timelineH = WAVEFORM_HEIGHT + 30

        timelineBg = Rectangle(0f, timelineY, camWidth, timelineH)
        timelineBg.setColor(0.15f, 0.15f, 0.15f)
        fgScene.attachChild(timelineBg)

        waveformBg = Rectangle(0f, timelineY, camWidth, WAVEFORM_HEIGHT)
        waveformBg.setColor(0.05f, 0.05f, 0.1f)
        fgScene.attachChild(waveformBg)

        renderBeatSnapLines()

        timelineCursor = Rectangle(0f, timelineY, PLAYBACK_BAR_WIDTH, timelineH)
        timelineCursor.setColor(1f, 0.3f, 0.3f)
        fgScene.attachChild(timelineCursor)

        val font = getFont()
        if (font != null) {
            timeText = ChangeableText(camWidth - 150, timelineY + WAVEFORM_HEIGHT + 5, font, "00:00.000 / 00:00.000", 30)
            timeText!!.setColor(0.8f, 0.8f, 0.8f)
            fgScene.attachChild(timeText)
        }
    }

    private fun createWaveform() {
        for (bar in waveformBars) fgScene.detachChild(bar)
        waveformBars.clear()

        val camWidth = engine.camera.width
        val barCount = (camWidth / 3).toInt()

        for (i in 0 until barCount) {
            val bar = Rectangle(i * 3f, 0f, 2f, 1f)
            bar.setColor(0.3f, 0.5f, 0.8f)
            waveformBg.attachChild(bar)
            waveformBars.add(bar)
        }

        generateDefaultWaveform()
    }

    private fun generateDefaultWaveform() {
        val barCount = waveformBars.size
        for (i in 0 until barCount) {
            val t = i.toFloat() / barCount
            var amplitude = (Math.sin(t * Math.PI * 8) * 0.3 + 0.5).toFloat() * WAVEFORM_HEIGHT * 0.4f
            amplitude = maxOf(amplitude, 2f)
            val bar = waveformBars[i]
            bar.setHeight(amplitude)
            bar.setPosition(bar.x, WAVEFORM_HEIGHT / 2f - amplitude / 2f)
        }
    }

    private fun updateWaveform() {
        val svc = GlobalManager.getInstance().songService
        val fft = svc?.spectrum
        if (fft != null && fft.isNotEmpty()) {
            lastSpectrum = fft
        }

        val displaySpectrum = lastSpectrum ?: return

        for (i in waveformBars.indices) {
            if (i >= displaySpectrum.size) break
            var amplitude = minOf(Math.abs(displaySpectrum[i]) * 200f, WAVEFORM_HEIGHT)
            amplitude = maxOf(amplitude, 1f)
            val bar = waveformBars[i]
            bar.setPosition(bar.x, WAVEFORM_HEIGHT / 2f - amplitude / 2f)
            bar.setHeight(amplitude)
        }
    }

    private fun renderBeatSnapLines() {
        for (line in beatSnapLines) fgScene.detachChild(line)
        beatSnapLines.clear()

        if (beatmapData == null) return

        var bpm = 120.0
        if (beatmapData!!.timingPoints.timing.controlPoints.isNotEmpty()) {
            bpm = beatmapData!!.timingPoints.timing.controlPoints[0].getBPM()
        }

        val snapMs = (60000.0 / bpm * beatSnap).toFloat()
        val camWidth = engine.camera.width
        val zoomedWidth = camWidth * waveformZoom

        var t = 0f
        while (t <= totalDuration) {
            val normalizedX = if (totalDuration > 0) t / totalDuration else 0f
            val x = (normalizedX * zoomedWidth) - waveformScrollX
            if (x >= -5 && x <= camWidth + 5) {
                val line = Rectangle(x, 0f, 1f, WAVEFORM_HEIGHT)
                line.setColor(0.3f, 0.3f, 0.3f, 0.5f)
                fgScene.attachChild(line)
                beatSnapLines.add(line)
            }
            t += snapMs
        }
    }

    private fun createPlaybackControls() {
        val camWidth = engine.camera.width
        val controlsY = WAVEFORM_HEIGHT + 35
        val btnSize = 50f
        val btnHeight = btnSize * 0.6f
        val spacing = 10f
        val totalWidth = btnSize * 8 + spacing * 7
        val startX = (camWidth - totalWidth) / 2f
        val font = getFont()

        val backBtn = object : Rectangle(startX, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.8f, 0.3f, 0.3f); return true }
                if (event.isActionUp) { setColor(0.6f, 0.2f, 0.2f); back(); return true }
                return false
            }
        }
        backBtn.setColor(0.6f, 0.2f, 0.2f)
        addLabel(backBtn, "BACK", font)
        fgScene.attachChild(backBtn); scene.registerTouchArea(backBtn)

        val playBtn = object : Rectangle(startX + (btnSize + spacing), controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.4f, 0.7f, 0.4f); return true }
                if (event.isActionUp) { setColor(0.3f, 0.5f, 0.3f); play(); return true }
                return false
            }
        }
        playBtn.setColor(0.3f, 0.5f, 0.3f)
        addLabel(playBtn, "PLAY", font)
        fgScene.attachChild(playBtn); scene.registerTouchArea(playBtn)

        val pauseBtn = object : Rectangle(startX + (btnSize + spacing) * 2, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.7f, 0.7f, 0.3f); return true }
                if (event.isActionUp) { setColor(0.5f, 0.5f, 0.2f); pause(); return true }
                return false
            }
        }
        pauseBtn.setColor(0.5f, 0.5f, 0.2f)
        addLabel(pauseBtn, "PAUSE", font)
        fgScene.attachChild(pauseBtn); scene.registerTouchArea(pauseBtn)

        val stopBtn = object : Rectangle(startX + (btnSize + spacing) * 3, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.7f, 0.3f, 0.3f); return true }
                if (event.isActionUp) { setColor(0.5f, 0.2f, 0.2f); stop(); return true }
                return false
            }
        }
        stopBtn.setColor(0.5f, 0.2f, 0.2f)
        addLabel(stopBtn, "STOP", font)
        fgScene.attachChild(stopBtn); scene.registerTouchArea(stopBtn)

        val rwBtn = object : Rectangle(startX + (btnSize + spacing) * 4, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.5f, 0.5f, 0.7f); return true }
                if (event.isActionUp) { setColor(0.3f, 0.3f, 0.5f); seekTo(currentTime - 5000); return true }
                return false
            }
        }
        rwBtn.setColor(0.3f, 0.3f, 0.5f)
        addLabel(rwBtn, "<<", font)
        fgScene.attachChild(rwBtn); scene.registerTouchArea(rwBtn)

        val fwBtn = object : Rectangle(startX + (btnSize + spacing) * 5, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.5f, 0.5f, 0.7f); return true }
                if (event.isActionUp) { setColor(0.3f, 0.3f, 0.5f); seekTo(currentTime + 5000); return true }
                return false
            }
        }
        fwBtn.setColor(0.3f, 0.3f, 0.5f)
        addLabel(fwBtn, ">>", font)
        fgScene.attachChild(fwBtn); scene.registerTouchArea(fwBtn)

        val undoBtn = object : Rectangle(startX + (btnSize + spacing) * 6, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.6f, 0.6f, 0.6f); return true }
                if (event.isActionUp) { setColor(0.4f, 0.4f, 0.4f); undo(); return true }
                return false
            }
        }
        undoBtn.setColor(0.4f, 0.4f, 0.4f)
        addLabel(undoBtn, "UNDO", font)
        fgScene.attachChild(undoBtn); scene.registerTouchArea(undoBtn)

        redoBtn = object : Rectangle(startX + (btnSize + spacing) * 7, controlsY, btnSize, btnHeight) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.6f, 0.6f, 0.6f); return true }
                if (event.isActionUp) { setColor(0.4f, 0.4f, 0.4f); redo(); return true }
                return false
            }
        }
        redoBtn.setColor(0.4f, 0.4f, 0.4f)
        addLabel(redoBtn, "REDO", font)
        fgScene.attachChild(redoBtn); scene.registerTouchArea(redoBtn)
    }

    private fun addLabel(btn: Rectangle, label: String, font: Font?) {
        if (font == null) return
        val text = Text(0f, 0f, font, label)
        text.setPosition((btn.width - text.width) / 2f, (btn.height - text.height) / 2f)
        text.setColor(1f, 1f, 1f)
        btn.attachChild(text)
    }

    private fun createSettingsButton() {
        val camWidth = engine.camera.width
        val btnSize = 55f
        val font = getFont()

        toolbarToggleBtn = object : Rectangle(camWidth - btnSize * 2 - 20, 10f, btnSize, btnSize * 0.6f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.3f, 0.5f, 0.7f); return true }
                if (event.isActionUp) { toggleToolbar(); return true }
                return false
            }
        }
        toolbarToggleBtn.setColor(0.3f, 0.5f, 0.7f)
        fgScene.attachChild(toolbarToggleBtn)
        scene.registerTouchArea(toolbarToggleBtn)
        if (font != null) addLabel(toolbarToggleBtn, "TOOL", font)

        val settingsBtn = object : Rectangle(camWidth - btnSize - 10, 10f, btnSize, btnSize * 0.6f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.7f, 0.3f, 0.7f); return true }
                if (event.isActionUp) { setColor(0.5f, 0.2f, 0.5f); openSettings(); return true }
                return false
            }
        }
        settingsBtn.setColor(0.5f, 0.2f, 0.5f)
        fgScene.attachChild(settingsBtn)
        scene.registerTouchArea(settingsBtn)
        if (font != null) addLabel(settingsBtn, "MENU", font)
    }

    private fun openSettings() {
        GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
            val fragment = EditorSettingsFragment()
            fragment.withEditor(this)
            fragment.show()
        }
    }

    private fun createGridSnapButton() {
        val font = getFont()

        gridSnapBtn = object : Rectangle(10f, 10f, 80f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.3f, 0.5f, 0.3f); return true }
                if (event.isActionUp) {
                    isGridSnapEnabled = !isGridSnapEnabled
                    toggleGridVisible()
                    updateGridSnapButton()
                    return true
                }
                return false
            }
        }
        gridSnapBtn.setColor(0.3f, 0.5f, 0.3f)
        fgScene.attachChild(gridSnapBtn)
        scene.registerTouchArea(gridSnapBtn)

        if (font != null) {
            gridSnapLabel = Text(0f, 0f, font, "SNAP: ON")
            gridSnapLabel!!.setPosition((gridSnapBtn.width - gridSnapLabel!!.width) / 2f, (gridSnapBtn.height - gridSnapLabel!!.height) / 2f)
            gridSnapLabel!!.setColor(1f, 1f, 1f)
            gridSnapBtn.attachChild(gridSnapLabel)
        }
    }

    private fun updateGridSnapButton() {
        gridSnapBtn.setColor(
            if (isGridSnapEnabled) 0.3f else 0.5f,
            if (isGridSnapEnabled) 0.5f else 0.3f,
            if (isGridSnapEnabled) 0.3f else 0.3f
        )
        if (gridSnapLabel != null) {
            gridSnapBtn.detachChild(gridSnapLabel)
            val font = getFont()
            if (font != null) {
                gridSnapLabel = Text(0f, 0f, font, "SNAP: ${if (isGridSnapEnabled) "ON" else "OFF"}")
                gridSnapLabel!!.setPosition((gridSnapBtn.width - gridSnapLabel!!.width) / 2f, (gridSnapBtn.height - gridSnapLabel!!.height) / 2f)
                gridSnapLabel!!.setColor(1f, 1f, 1f)
                gridSnapBtn.attachChild(gridSnapLabel)
            }
        }
    }

    private fun createBeatSnapButton() {
        val camWidth = engine.camera.width
        val font = getFont()

        beatSnapBtn = object : Rectangle(camWidth - 80, 10f, 70f, 28f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.3f, 0.4f, 0.6f); return true }
                if (event.isActionUp) { cycleBeatSnap(); return true }
                return false
            }
        }
        beatSnapBtn.setColor(0.3f, 0.4f, 0.6f)
        fgScene.attachChild(beatSnapBtn)
        scene.registerTouchArea(beatSnapBtn)

        if (font != null) {
            beatSnapLabel = Text(0f, 0f, font, "1/1")
            beatSnapLabel!!.setPosition((beatSnapBtn.width - beatSnapLabel!!.width) / 2f, (beatSnapBtn.height - beatSnapLabel!!.height) / 2f)
            beatSnapLabel!!.setColor(1f, 1f, 1f)
            beatSnapBtn.attachChild(beatSnapLabel)
        }
    }

    private fun cycleBeatSnap() {
        beatSnapIndex = (beatSnapIndex + 1) % BEAT_SNAPS.size
        beatSnap = BEAT_SNAPS[beatSnapIndex]
        renderBeatSnapLines()
        if (beatSnapLabel != null) {
            beatSnapBtn.detachChild(beatSnapLabel)
            val font = getFont()
            if (font != null) {
                beatSnapLabel = Text(0f, 0f, font, BEAT_SNAP_LABELS[beatSnapIndex])
                beatSnapLabel!!.setPosition((beatSnapBtn.width - beatSnapLabel!!.width) / 2f, (beatSnapBtn.height - beatSnapLabel!!.height) / 2f)
                beatSnapLabel!!.setColor(1f, 1f, 1f)
                beatSnapBtn.attachChild(beatSnapLabel)
            }
        }
    }

    private fun createBeatSkipButtons() {
        val font = getFont()

        prevBeatBtn = object : Rectangle(80f, 10f, 50f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.5f, 0.5f, 0.3f); return true }
                if (event.isActionUp) { setColor(0.3f, 0.3f, 0.2f); skipToPrevBeat(); return true }
                return false
            }
        }
        prevBeatBtn.setColor(0.3f, 0.3f, 0.2f)
        fgScene.attachChild(prevBeatBtn)
        scene.registerTouchArea(prevBeatBtn)
        if (font != null) addLabel(prevBeatBtn, "|<", font)

        nextBeatBtn = object : Rectangle(135f, 10f, 50f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.5f, 0.5f, 0.3f); return true }
                if (event.isActionUp) { setColor(0.3f, 0.3f, 0.2f); skipToNextBeat(); return true }
                return false
            }
        }
        nextBeatBtn.setColor(0.3f, 0.3f, 0.2f)
        fgScene.attachChild(nextBeatBtn)
        scene.registerTouchArea(nextBeatBtn)
        if (font != null) addLabel(nextBeatBtn, ">|", font)
    }

    private fun createNudgeButtons() {
        val font = getFont()

        nudgeBackBtn = object : Rectangle(190f, 10f, 50f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.4f, 0.3f, 0.5f); return true }
                if (event.isActionUp) { setColor(0.2f, 0.2f, 0.3f); nudgeSelected(-1); return true }
                return false
            }
        }
        nudgeBackBtn.setColor(0.2f, 0.2f, 0.3f)
        fgScene.attachChild(nudgeBackBtn)
        scene.registerTouchArea(nudgeBackBtn)
        if (font != null) addLabel(nudgeBackBtn, "-T", font)

        nudgeFwdBtn = object : Rectangle(245f, 10f, 50f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.4f, 0.3f, 0.5f); return true }
                if (event.isActionUp) { setColor(0.2f, 0.2f, 0.3f); nudgeSelected(1); return true }
                return false
            }
        }
        nudgeFwdBtn.setColor(0.2f, 0.2f, 0.3f)
        fgScene.attachChild(nudgeFwdBtn)
        scene.registerTouchArea(nudgeFwdBtn)
        if (font != null) addLabel(nudgeFwdBtn, "+T", font)
    }

    private fun skipToPrevBeat() {
        if (beatmapData == null) return
        var bpm = 120.0
        if (beatmapData!!.timingPoints.timing.controlPoints.isNotEmpty()) {
            bpm = beatmapData!!.timingPoints.timing.controlPoints[0].getBPM()
        }
        val snapMs = (60000.0 / bpm * beatSnap).toFloat()
        val prevBeat = maxOf(0f, (Math.floor((currentTime / snapMs).toDouble()) * snapMs - snapMs).toFloat())
        seekTo(prevBeat)
    }

    private fun skipToNextBeat() {
        if (beatmapData == null) return
        var bpm = 120.0
        if (beatmapData!!.timingPoints.timing.controlPoints.isNotEmpty()) {
            bpm = beatmapData!!.timingPoints.timing.controlPoints[0].getBPM()
        }
        val snapMs = (60000.0 / bpm * beatSnap).toFloat()
        val nextBeat = (Math.ceil((currentTime / snapMs).toDouble()) * snapMs + snapMs).toFloat()
        seekTo(nextBeat)
    }

    private fun nudgeSelected(direction: Int) {
        if (selectedObjectIndex < 0 || beatmapData == null) return
        val nudgeMs = direction * (60000.0 / 120.0 * beatSnap).toFloat()
        val objects = beatmapData!!.hitObjects.objects

        for (idx in selectedObjects) {
            if (idx in objects.indices) {
                val obj = objects[idx]
                val shifted = cloneWithTimeOffset(obj, nudgeMs)
                if (shifted != null) {
                    beatmapData!!.hitObjects.remove(idx)
                    beatmapData!!.hitObjects.add(shifted)
                }
            }
        }
        if (selectedObjectIndex in objects.indices && !selectedObjects.contains(selectedObjectIndex)) {
            val obj = objects[selectedObjectIndex]
            val shifted = cloneWithTimeOffset(obj, nudgeMs)
            if (shifted != null) {
                beatmapData!!.hitObjects.remove(selectedObjectIndex)
                beatmapData!!.hitObjects.add(shifted)
            }
        }
        renderHitObjects()
    }

    private fun redo() {
        if (redoStack.isEmpty()) return

        val action = redoStack.pop()
        val objects = beatmapData!!.hitObjects.objects

        when (action.type) {
            EditorAction.Type.Add -> {
                if (action.hitObject != null) {
                    beatmapData!!.hitObjects.add(action.hitObject)
                    undoStack.push(EditorAction(EditorAction.Type.Add, -1, action.hitObject))
                }
            }
            EditorAction.Type.Delete -> {
                if (action.index in objects.indices) {
                    val removed = beatmapData!!.hitObjects.remove(action.index)
                    undoStack.push(EditorAction(EditorAction.Type.Delete, action.index, removed))
                }
            }
            EditorAction.Type.Move -> {
                if (action.index in objects.indices) {
                    val obj = objects[action.index]
                    val curX = obj.position.x
                    val curY = obj.position.y
                    obj.position.x = action.oldX
                    obj.position.y = action.oldY
                    val undo = EditorAction(EditorAction.Type.Move, action.index, null)
                    undo.oldX = curX
                    undo.oldY = curY
                    undoStack.push(undo)
                }
            }
        }

        selectedObjectIndex = -1
        renderHitObjects()
    }

    private fun toggleGridVisible() {
        gridVisible = !gridVisible
        for (line in gridLines) {
            line.isVisible = gridVisible
        }
    }

    private fun createZoomButtons() {
        val font = getFont()

        zoomInBtn = object : Rectangle(300f, 10f, 40f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.3f, 0.6f, 0.3f); return true }
                if (event.isActionUp) {
                    setColor(0.2f, 0.4f, 0.2f)
                    waveformZoom = minOf(waveformZoom * 1.5f, 8f)
                    updateWaveformZoom()
                    return true
                }
                return false
            }
        }
        zoomInBtn.setColor(0.2f, 0.4f, 0.2f)
        fgScene.attachChild(zoomInBtn)
        scene.registerTouchArea(zoomInBtn)
        if (font != null) addLabel(zoomInBtn, "Z+", font)

        zoomOutBtn = object : Rectangle(345f, 10f, 40f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.6f, 0.3f, 0.3f); return true }
                if (event.isActionUp) {
                    setColor(0.4f, 0.2f, 0.2f)
                    waveformZoom = maxOf(waveformZoom / 1.5f, 0.5f)
                    updateWaveformZoom()
                    return true
                }
                return false
            }
        }
        zoomOutBtn.setColor(0.4f, 0.2f, 0.2f)
        fgScene.attachChild(zoomOutBtn)
        scene.registerTouchArea(zoomOutBtn)
        if (font != null) addLabel(zoomOutBtn, "Z-", font)

        zoomResetBtn = object : Rectangle(390f, 10f, 40f, 30f) {
            override fun onAreaTouched(event: TouchEvent, lx: Float, ly: Float): Boolean {
                if (event.isActionDown) { setColor(0.5f, 0.5f, 0.5f); return true }
                if (event.isActionUp) {
                    setColor(0.3f, 0.3f, 0.3f)
                    waveformZoom = 1f
                    waveformScrollX = 0f
                    updateWaveformZoom()
                    return true
                }
                return false
            }
        }
        zoomResetBtn.setColor(0.3f, 0.3f, 0.3f)
        fgScene.attachChild(zoomResetBtn)
        scene.registerTouchArea(zoomResetBtn)
        if (font != null) addLabel(zoomResetBtn, "Z=", font)
    }

    private fun updateWaveformZoom() {
        renderBeatSnapLines()
        renderTimingMarkers()
    }

    private fun showToolbar() {
        GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
            try {
                if (this@EditorScene != GlobalManager.getInstance().editorScene) return@runOnUiThread
                currentToolbar = EditorToolbarFragment()
                currentToolbar!!.setEditorScene(this@EditorScene)
                currentToolbar!!.show()
                toolbarVisible = true
            } catch (_: Exception) {}
        }
    }

    private fun hideToolbar() {
        currentToolbar?.dismiss()
        currentToolbar = null
        toolbarVisible = false
    }

    private fun toggleToolbar() {
        if (toolbarVisible) hideToolbar() else showToolbar()
    }

    private fun renderHitObjects() {
        for (spr in objectSprites) {
            if (spr.sprite != null) mgScene.detachChild(spr.sprite)
            if (spr.endSprite != null) mgScene.detachChild(spr.endSprite)
            for (seg in spr.bodySegments) mgScene.detachChild(seg)
        }
        objectSprites.clear()

        if (beatmapData == null) return

        val objects = beatmapData!!.hitObjects.objects
        Log.i("EditorScene", "Rendering ${objects.size} objects, scale=${getObjectScale()}")
        val scaleX = playfieldWidth / Constants.MAP_WIDTH
        val scaleY = playfieldHeight / Constants.MAP_HEIGHT

        val combos = getDefaultCombos()
        var comboIndex = 0

        for (i in objects.indices) {
            val obj = objects[i]
            val pos = obj.position
            val screenX = playfieldOffsetX + pos.x.toFloat() * scaleX
            val screenY = playfieldOffsetY + pos.y.toFloat() * scaleY

            val isNewCombo = newComboFlags[i]
            if (isNewCombo == true) {
                comboIndex++
            }

            val color = combos[comboIndex % combos.size]

            when (obj) {
                is HitCircle -> renderHitCircle(screenX, screenY, color, i, obj.startTime.toFloat())
                is Slider -> renderSlider(screenX, screenY, obj, color, i, scaleX, scaleY)
                is Spinner -> renderSpinner(color, i, obj.startTime.toFloat())
            }

            if (isNewCombo == true) {
                val size = 8f
                val indicator = Rectangle(screenX - size / 2, screenY - getObjectScale() * 64 - size - 4, size, size)
                indicator.setColor(1f, 1f, 0.3f)
                mgScene.attachChild(indicator)
            }
        }

        highlightObject(selectedObjectIndex)
    }

    private fun updateObjectVisibility() {
        val halfWindow = OBJECT_VISIBLE_WINDOW_MS / 2f
        for (spr in objectSprites) {
            val diff = Math.abs(spr.startTime - currentTime)
            val visible = diff <= halfWindow
            for (entity in spr.allEntities) {
                entity?.isVisible = visible
            }
        }
    }

    private fun getObjectScale(): Float {
        val cs = beatmapData?.difficulty?.cs ?: 5f
        val csScale = (1f - 0.7f * (cs - 5f) / 5f) / 2f
        val editorScale = playfieldHeight / 384f
        return editorScale * csScale
    }

    private fun renderHitCircle(x: Float, y: Float, color: RGBColor, index: Int, startTime: Float) {
        val scale = getObjectScale()
        val spr = EditorObjectSprite()
        spr.objectIndex = index
        spr.startTime = startTime

        val approach = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("approachcircle"))
        approach.setScale(scale * 2)
        approach.setColor(color.r(), color.g(), color.b())
        Utils.putSpriteAnchorCenter(x, y, approach)
        mgScene.attachChild(approach)
        spr.allEntities.add(approach)

        val circle = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("hitcircle"))
        circle.setScale(scale)
        circle.setColor(color.r(), color.g(), color.b())
        Utils.putSpriteAnchorCenter(x, y, circle)
        mgScene.attachChild(circle)
        spr.sprite = circle
        spr.allEntities.add(circle)

        val overlay = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("hitcircleoverlay"))
        overlay.setScale(scale)
        Utils.putSpriteAnchorCenter(x, y, overlay)
        mgScene.attachChild(overlay)
        spr.allEntities.add(overlay)

        objectSprites.add(spr)
    }

    private fun renderSlider(x: Float, y: Float, slider: Slider, color: RGBColor, index: Int, scaleX: Float, scaleY: Float) {
        val path = slider.path
        val scale = getObjectScale()
        val spr = EditorObjectSprite()
        spr.objectIndex = index
        spr.startTime = slider.startTime.toFloat()

        val calculatedPath = path.calculatedPath

        for (p in calculatedPath) {
            val sx = playfieldOffsetX + p.x.toFloat() * scaleX
            val sy = playfieldOffsetY + p.y.toFloat() * scaleY
            val dot = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("hitcircle"))
            dot.setScale(scale * 0.35f)
            dot.setColor(color.r(), color.g(), color.b())
            dot.setAlpha(0.85f)
            Utils.putSpriteAnchorCenter(sx, sy, dot)
            mgScene.attachChild(dot)
            spr.bodySegments.add(dot)
            spr.allEntities.add(dot)
        }

        val startCircle = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("sliderstartcircle"))
        startCircle.setScale(scale)
        startCircle.setColor(color.r(), color.g(), color.b())
        Utils.putSpriteAnchorCenter(x, y, startCircle)
        mgScene.attachChild(startCircle)
        spr.sprite = startCircle
        spr.allEntities.add(startCircle)

        val startOverlay = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("sliderstartcircleoverlay"))
        startOverlay.setScale(scale)
        Utils.putSpriteAnchorCenter(x, y, startOverlay)
        mgScene.attachChild(startOverlay)
        spr.allEntities.add(startOverlay)

        val endPos = slider.endPosition
        val endX = playfieldOffsetX + endPos.x.toFloat() * scaleX
        val endY = playfieldOffsetY + endPos.y.toFloat() * scaleY

        val endCircle = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("sliderendcircle"))
        endCircle.setScale(scale)
        endCircle.setColor(color.r(), color.g(), color.b())
        Utils.putSpriteAnchorCenter(endX, endY, endCircle)
        mgScene.attachChild(endCircle)
        spr.endSprite = endCircle
        spr.allEntities.add(endCircle)

        val endOverlay = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("sliderendcircleoverlay"))
        endOverlay.setScale(scale)
        Utils.putSpriteAnchorCenter(endX, endY, endOverlay)
        mgScene.attachChild(endOverlay)
        spr.allEntities.add(endOverlay)

        val repeatCount = slider.repeatCount
        if (repeatCount > 1 && calculatedPath.size >= 2) {
            for (r in 1 until repeatCount) {
                val t = r.toFloat() / repeatCount
                var pathIdx = (t * (calculatedPath.size - 1)).toInt()
                pathIdx = maxOf(0, minOf(pathIdx, calculatedPath.size - 1))
                val rp = calculatedPath[pathIdx]
                val rsx = playfieldOffsetX + rp.x.toFloat() * scaleX
                val rsy = playfieldOffsetY + rp.y.toFloat() * scaleY

                val arrow = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("reversearrow"))
                arrow.setScale(scale)
                arrow.setColor(color.r(), color.g(), color.b())
                Utils.putSpriteAnchorCenter(rsx, rsy, arrow)
                mgScene.attachChild(arrow)
                spr.allEntities.add(arrow)
            }
        }

        objectSprites.add(spr)
    }

    private fun renderSpinner(color: RGBColor, index: Int, startTime: Float) {
        val cx = playfieldOffsetX + playfieldWidth / 2f
        val cy = playfieldOffsetY + playfieldHeight / 2f
        val scale = getObjectScale()
        val spr = EditorObjectSprite()
        spr.objectIndex = index
        spr.startTime = startTime

        val bg = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("spinner-background"))
        bg.setScale(playfieldHeight / bg.height)
        Utils.putSpriteAnchorCenter(cx, cy, bg)
        mgScene.attachChild(bg)
        spr.allEntities.add(bg)

        val approach = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("spinner-approachcircle"))
        approach.setScale(scale * 3)
        approach.setColor(color.r(), color.g(), color.b())
        Utils.putSpriteAnchorCenter(cx, cy, approach)
        mgScene.attachChild(approach)
        spr.allEntities.add(approach)

        val circle = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("spinner-circle"))
        circle.setScale(scale)
        Utils.putSpriteAnchorCenter(cx, cy, circle)
        mgScene.attachChild(circle)
        spr.sprite = circle
        spr.allEntities.add(circle)

        val spinText = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("spinner-spin"))
        spinText.setScale(scale * 0.8f)
        Utils.putSpriteAnchorCenter(cx, cy * 1.5f, spinText)
        mgScene.attachChild(spinText)
        spr.allEntities.add(spinText)

        objectSprites.add(spr)
    }

    private fun renderTimingMarkers() {
        for (marker in timingMarkers) {
            fgScene.detachChild(marker.line)
            if (marker.label != null) fgScene.detachChild(marker.label)
        }
        timingMarkers.clear()

        if (beatmapData == null) return

        val camWidth = engine.camera.width
        val zoomedWidth = camWidth * waveformZoom
        val font = getFont()
        val timingPoints = beatmapData!!.timingPoints.timing.controlPoints

        for (tp in timingPoints) {
            val t = tp.time.toFloat()
            val normalizedX = if (totalDuration > 0) t / totalDuration else 0f
            val x = (normalizedX * zoomedWidth) - waveformScrollX
            if (x < -50 || x > camWidth + 50) continue

            val isKiai = java.lang.Boolean.TRUE == kiaiFlags[tp.time]

            val line = Rectangle(x, 0f, if (isKiai) 3f else 2f, WAVEFORM_HEIGHT + 30)
            line.setColor(if (isKiai) 1f else 1f, if (isKiai) 0.3f else 0.8f, if (isKiai) 0.3f else 0.2f, if (isKiai) 1f else 0.8f)
            fgScene.attachChild(line)

            val marker = TimingMarker()
            marker.line = line
            marker.time = t
            marker.bpm = tp.getBPM()

            if (font != null) {
                val kiaiStr = if (isKiai) " [KIAI]" else ""
                val label = Text(x + 4, WAVEFORM_HEIGHT + 5, font, String.format("%.0f BPM%s", tp.getBPM(), kiaiStr))
                label.setColor(if (isKiai) 1f else 1f, if (isKiai) 0.4f else 0.8f, if (isKiai) 0.4f else 0.2f)
                fgScene.attachChild(label)
                marker.label = label
            }

            timingMarkers.add(marker)
        }
    }

    private fun setupTouchHandler() {
        scene.setOnAreaTouchListener { touchEvent, _, x, y -> handleTouch(touchEvent, x, y) }
    }

    private fun handleTouch(event: TouchEvent, x: Float, y: Float): Boolean {
        if (y < WAVEFORM_HEIGHT + 30) {
            if (event.isActionDown || event.isActionMove) {
                currentTime = maxOf(0f, minOf((x / engine.camera.width) * totalDuration, totalDuration))
                val svc = GlobalManager.getInstance().songService
                if (svc != null && svc.status == Status.PLAYING) {
                    svc.seekTo(currentTime.toInt())
                }
                updateTimelineCursor()
                updateTimeText()
                return true
            }
            return false
        }

        if (x >= playfieldOffsetX && x <= playfieldOffsetX + playfieldWidth &&
            y >= playfieldOffsetY && y <= playfieldOffsetY + playfieldHeight) {
            return when (currentTool) {
                EditorTool.Select -> handleSelectTool(event, x, y)
                EditorTool.Circle -> handleCircleTool(event, x, y)
                EditorTool.Slider -> handleSliderTool(event, x, y)
                EditorTool.Spinner -> handleSpinnerTool(event, x, y)
                EditorTool.Delete -> handleDeleteTool(event, x, y)
                EditorTool.TimingAdd -> handleTimingAddTool(event, x, y)
                EditorTool.TimingDelete -> handleTimingDeleteTool(event, x, y)
            }
        }
        return false
    }

    private fun handleSelectTool(event: TouchEvent, x: Float, y: Float): Boolean {
        if (event.isActionDown) {
            val idx = findObjectAt(x, y)
            if (idx >= 0) {
                if (isMultiSelecting) {
                    if (selectedObjects.contains(idx)) selectedObjects.remove(idx) else selectedObjects.add(idx)
                    selectedObjectIndex = idx
                } else {
                    selectedObjects.clear()
                    selectedObjects.add(idx)
                    selectedObjectIndex = idx
                }
                isDragging = true
                dragStartX = x
                dragStartY = y

                dragStartPositions.clear()
                for (selIdx in selectedObjects) {
                    val selObj = beatmapData!!.hitObjects.objects[selIdx]
                    dragStartPositions[selIdx] = floatArrayOf(selObj.position.x.toFloat(), selObj.position.y.toFloat())
                }
                highlightObject(idx)
            } else {
                selectedObjects.clear()
                selectedObjectIndex = -1
                highlightObject(-1)
            }
            return true
        }

        if (event.isActionMove && isDragging && selectedObjectIndex >= 0) {
            val scaleX = playfieldWidth / Constants.MAP_WIDTH
            val scaleY = playfieldHeight / Constants.MAP_HEIGHT
            val dx = (x - dragStartX) / scaleX
            val dy = (y - dragStartY) / scaleY

            for (idx in selectedObjects) {
                val obj = beatmapData!!.hitObjects.objects[idx]
                val startPos = dragStartPositions[idx]
                if (startPos != null) {
                    obj.position.x = maxOf(0f, minOf(startPos[0] + dx, Constants.MAP_WIDTH.toFloat()))
                    obj.position.y = maxOf(0f, minOf(startPos[1] + dy, Constants.MAP_HEIGHT.toFloat()))
                }
            }
            renderHitObjects()
            return true
        }

        if (event.isActionUp && isDragging) {
            isDragging = false
            return true
        }
        return false
    }

    private fun handleCircleTool(event: TouchEvent, x: Float, y: Float): Boolean {
        if (event.isActionUp) {
            val scaleX = playfieldWidth / Constants.MAP_WIDTH
            val scaleY = playfieldHeight / Constants.MAP_HEIGHT
            val trackX = snapToGrid(clampTrackX((x - playfieldOffsetX) / scaleX))
            val trackY = snapToGrid(clampTrackY((y - playfieldOffsetY) / scaleY))

            val circle = HitCircle(currentTime.toDouble(), trackX, trackY)
            beatmapData!!.hitObjects.add(circle)
            val idx = beatmapData!!.hitObjects.objects.size - 1
            pushUndo(EditorAction(EditorAction.Type.Add, idx, circle))
            selectedObjectIndex = idx
            renderHitObjects()
            return true
        }
        return event.isActionDown
    }

    private fun handleSliderTool(event: TouchEvent, x: Float, y: Float): Boolean {
        val scaleX = playfieldWidth / Constants.MAP_WIDTH
        val scaleY = playfieldHeight / Constants.MAP_HEIGHT
        val trackX = clampTrackX((x - playfieldOffsetX) / scaleX)
        val trackY = clampTrackY((y - playfieldOffsetY) / scaleY)

        if (event.isActionDown) {
            isCreatingSlider = true
            sliderStartX = snapToGrid(trackX)
            sliderStartY = snapToGrid(trackY)
            return true
        }

        if (event.isActionUp && isCreatingSlider) {
            isCreatingSlider = false
            val endX = snapToGrid(trackX)
            val endY = snapToGrid(trackY)

            val dx = endX - sliderStartX
            val dy = endY - sliderStartY
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist < 10) return true

            val controlPoints = ArrayList<Vector2>()
            controlPoints.add(Vector2(sliderStartX, sliderStartY))
            controlPoints.add(Vector2(endX, endY))

            val path = SliderPath(SliderPathType.Linear, controlPoints, dist.toDouble())

            val timing = getTimingControlPoint(currentTime.toDouble())!!
            val difficulty = getDifficultyControlPoint(currentTime.toDouble())!!

            val sliderVelocity = beatmapData!!.difficulty.sliderMultiplier
            val tickRate = beatmapData!!.difficulty.sliderTickRate

            val slider = Slider(
                currentTime.toDouble(), Vector2(sliderStartX, sliderStartY),
                timing, difficulty, 1, path, sliderVelocity, tickRate, 1.0, true
            )

            beatmapData!!.hitObjects.add(slider)
            val sIdx = beatmapData!!.hitObjects.objects.size - 1
            pushUndo(EditorAction(EditorAction.Type.Add, sIdx, slider))
            selectedObjectIndex = sIdx
            renderHitObjects()
            return true
        }
        return false
    }

    private fun handleSpinnerTool(event: TouchEvent, x: Float, y: Float): Boolean {
        if (event.isActionUp) {
            val spinner = Spinner(currentTime.toDouble(), currentTime.toDouble() + 5000)
            beatmapData!!.hitObjects.add(spinner)
            val spIdx = beatmapData!!.hitObjects.objects.size - 1
            pushUndo(EditorAction(EditorAction.Type.Add, spIdx, spinner))
            selectedObjectIndex = spIdx
            renderHitObjects()
            return true
        }
        return event.isActionDown
    }

    private fun handleDeleteTool(event: TouchEvent, x: Float, y: Float): Boolean {
        if (event.isActionUp) {
            val idx = findObjectAt(x, y)
            if (idx >= 0) {
                val removed = beatmapData!!.hitObjects.remove(idx)
                if (removed != null) {
                    pushUndo(EditorAction(EditorAction.Type.Delete, idx, removed))
                }
                selectedObjectIndex = -1
                renderHitObjects()
            }
            return true
        }
        return event.isActionDown
    }

    private fun handleTimingAddTool(event: TouchEvent, x: Float, y: Float): Boolean {
        if (event.isActionUp) {
            val existing = getTimingControlPoint(currentTime.toDouble())
            val msPerBeat = existing?.msPerBeat ?: (60000.0 / 120.0)
            val sig = existing?.timeSignature ?: 4

            val tp = TimingControlPoint(currentTime.toDouble(), msPerBeat, sig)
            beatmapData!!.timingPoints.timing.add(tp)

            val dp = DifficultyControlPoint(currentTime.toDouble(), 1.0, true)
            beatmapData!!.timingPoints.difficulty.add(dp)

            renderTimingMarkers()
            return true
        }
        return event.isActionDown
    }

    private fun handleTimingDeleteTool(event: TouchEvent, x: Float, y: Float): Boolean {
        if (event.isActionUp) {
            val points = beatmapData!!.timingPoints.timing.controlPoints
            if (points.size <= 1) return true

            var nearestIdx = -1
            var nearestDist = Float.MAX_VALUE
            for (i in points.indices) {
                val t = points[i].time.toFloat()
                val screenX = if (totalDuration > 0) (t / totalDuration) * engine.camera.width else 0f
                val dx = x - screenX
                if (Math.abs(dx) < nearestDist) {
                    nearestDist = Math.abs(dx)
                    nearestIdx = i
                }
            }

            if (nearestIdx >= 0 && nearestDist < 30) {
                beatmapData!!.timingPoints.timing.remove(points[nearestIdx])
                renderTimingMarkers()
            }
            return true
        }
        return event.isActionDown
    }

    private fun findObjectAt(screenX: Float, screenY: Float): Int {
        val scaleX = playfieldWidth / Constants.MAP_WIDTH
        val scaleY = playfieldHeight / Constants.MAP_HEIGHT
        val trackX = (screenX - playfieldOffsetX) / scaleX
        val trackY = (screenY - playfieldOffsetY) / scaleY

        val objects = beatmapData!!.hitObjects.objects
        for (i in objects.indices.reversed()) {
            val pos = objects[i].position
            val dx = trackX - pos.x.toFloat()
            val dy = trackY - pos.y.toFloat()
            if (Math.sqrt((dx * dx + dy * dy).toDouble()) <= 64) return i
        }
        return -1
    }

    private fun snapToGrid(value: Float): Float {
        if (!isGridSnapEnabled) return value
        return Math.round(value / GRID_SIZE) * GRID_SIZE
    }

    private fun clampTrackX(value: Float): Float {
        return maxOf(0f, minOf(value, Constants.MAP_WIDTH.toFloat()))
    }

    private fun clampTrackY(value: Float): Float {
        return maxOf(0f, minOf(value, Constants.MAP_HEIGHT.toFloat()))
    }

    private fun getTimingControlPoint(time: Double): TimingControlPoint? {
        val points = beatmapData!!.timingPoints.timing.controlPoints
        for (i in points.indices.reversed()) {
            if (time >= points[i].time) return points[i]
        }
        return if (points.isEmpty()) TimingControlPoint(0.0, 60000.0 / 120.0, 4) else points[0]
    }

    private fun getDifficultyControlPoint(time: Double): DifficultyControlPoint? {
        val points = beatmapData!!.timingPoints.difficulty.controlPoints
        for (i in points.indices.reversed()) {
            if (time >= points[i].time) return points[i]
        }
        return if (points.isEmpty()) DifficultyControlPoint(0.0, 1.0, true) else points[0]
    }

    private fun highlightObject(index: Int) {
        for (spr in objectSprites) {
            if (spr.sprite != null) {
                val isSelected = selectedObjects.contains(spr.objectIndex)
                spr.sprite!!.setAlpha(if (isSelected) 1.0f else 0.6f)
                for (e in spr.allEntities) {
                    if (e != null && e != spr.sprite) {
                        e.setAlpha(if (isSelected) 1.0f else 0.6f)
                    }
                }
            }
        }
    }

    private fun pushUndo(action: EditorAction) {
        undoStack.push(action)
        redoStack.clear()
    }

    private fun undo() {
        if (undoStack.isEmpty()) return

        val action = undoStack.pop()
        val objects = beatmapData!!.hitObjects.objects

        when (action.type) {
            EditorAction.Type.Add -> {
                if (action.index in objects.indices) {
                    val removed = beatmapData!!.hitObjects.remove(action.index)
                    redoStack.push(EditorAction(EditorAction.Type.Add, action.index, removed))
                }
            }
            EditorAction.Type.Delete -> {
                if (action.index in 0..objects.size) {
                    beatmapData!!.hitObjects.add(action.hitObject!!)
                    redoStack.push(EditorAction(EditorAction.Type.Delete, action.index, null))
                }
            }
            EditorAction.Type.Move -> {
                if (action.index in objects.indices) {
                    val obj = objects[action.index]
                    val curX = obj.position.x
                    val curY = obj.position.y
                    obj.position.x = action.oldX
                    obj.position.y = action.oldY
                    val redo = EditorAction(EditorAction.Type.Move, action.index, null)
                    redo.oldX = curX
                    redo.oldY = curY
                    redoStack.push(redo)
                }
            }
        }

        selectedObjectIndex = -1
        renderHitObjects()
    }

    private fun updateTimelineCursor() {
        val camWidth = engine.camera.width
        val zoomedWidth = camWidth * waveformZoom
        val normalizedX = if (totalDuration > 0) currentTime / totalDuration else 0f
        var x = (normalizedX * zoomedWidth) - waveformScrollX
        x = maxOf(0f, minOf(x, camWidth))
        timelineCursor.setPosition(x, timelineCursor.y)
    }

    private fun updateTimeText() {
        timeText?.setText("${formatTime(currentTime)} / ${formatTime(totalDuration)}")
    }

    private fun formatTime(ms: Float): String {
        val totalSec = (ms / 1000).toInt()
        return String.format("%02d:%02d.%03d", totalSec / 60, totalSec % 60, (ms % 1000).toInt())
    }

    private fun getDefaultCombos(): ArrayList<RGBColor> {
        val combos = ArrayList<RGBColor>()
        if (beatmapData != null && beatmapData!!.colors.comboColors.isNotEmpty()) {
            for (color in beatmapData!!.colors.comboColors) {
                combos.add(RGBColor(color))
            }
        }
        if (combos.isEmpty()) {
            combos.add(RGBColor(1f, 0.4f, 0.4f))
            combos.add(RGBColor(0.4f, 1f, 0.4f))
            combos.add(RGBColor(0.4f, 0.4f, 1f))
            combos.add(RGBColor(1f, 1f, 0.4f))
            combos.add(RGBColor(1f, 0.4f, 1f))
            combos.add(RGBColor(0.4f, 1f, 1f))
        }
        return combos
    }

    private fun getFont(): Font? {
        return try { ResourceManager.getInstance().getFont("smallFont") } catch (_: Exception) { null }
    }

    fun getScene(): Scene = scene

    fun play() {
        val svc = GlobalManager.getInstance().songService
        if (svc != null) {
            if (svc.status == Status.PAUSED) svc.play() else {
                svc.play()
                svc.setVolume(Config.getBgmVolume())
            }
            totalDuration = svc.length.toFloat()
        }
        isPlaying = true
    }

    fun pause() {
        val svc = GlobalManager.getInstance().songService
        if (svc != null && svc.status == Status.PLAYING) svc.pause()
        isPlaying = false
    }

    fun stop() {
        val svc = GlobalManager.getInstance().songService
        svc?.stop()
        isPlaying = false
        currentTime = 0f
        updateTimelineCursor()
        updateTimeText()
    }

    fun seekTo(timeMs: Float) {
        currentTime = maxOf(0f, minOf(timeMs, totalDuration))
        val svc = GlobalManager.getInstance().songService
        if (svc != null && svc.status != Status.STOPPED) svc.seekTo(currentTime.toInt())
        updateTimelineCursor()
        updateTimeText()
    }

    enum class EditorTool { Select, Circle, Slider, Spinner, Delete, TimingAdd, TimingDelete }

    private class EditorObjectSprite {
        var sprite: Entity? = null
        var endSprite: Entity? = null
        val bodySegments = ArrayList<Entity>()
        val allEntities = ArrayList<Entity>()
        var objectIndex = 0
        var startTime = 0f
    }

    private class TimingMarker {
        var line: Rectangle? = null
        var label: Text? = null
        var time = 0f
        var bpm = 0.0
    }

    class EditorAction(val type: Type, val index: Int, val hitObject: HitObject?) {
        enum class Type { Add, Delete, Move }
        var oldX = 0f
        var oldY = 0f
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        if (isPlaying) {
            val svc = GlobalManager.getInstance().songService
            if (svc != null && svc.status == Status.PLAYING) {
                currentTime = svc.position.toFloat()
            } else {
                currentTime += pSecondsElapsed * 1000f
                if (currentTime >= totalDuration) { currentTime = totalDuration; stop() }
            }
            updateTimelineCursor()
            updateTimeText()
        }
        updateObjectVisibility()
        updateWaveform()
    }

    override fun reset() {
        currentTime = 0f
        isPlaying = false
    }

    fun show() {
        scene.registerUpdateHandler(this)
        engine.scene = scene
    }

    fun deleteSelectedObject() {
        if (selectedObjectIndex >= 0 && beatmapData != null) {
            val removed = beatmapData!!.hitObjects.remove(selectedObjectIndex)
            if (removed != null) {
                pushUndo(EditorAction(EditorAction.Type.Delete, selectedObjectIndex, removed))
            }
            selectedObjectIndex = -1
            renderHitObjects()
        }
    }

    fun refresh() {
        renderHitObjects()
        renderTimingMarkers()
        renderBeatSnapLines()
    }

    fun deleteSelectedObjects() {
        if (selectedObjects.isEmpty() && selectedObjectIndex < 0) return
        val objects = beatmapData!!.hitObjects.objects

        val toDelete = ArrayList(selectedObjects)
        if (selectedObjectIndex >= 0 && !toDelete.contains(selectedObjectIndex)) {
            toDelete.add(selectedObjectIndex)
        }
        toDelete.sortDescending()

        for (idx in toDelete) {
            if (idx in objects.indices) {
                val removed = beatmapData!!.hitObjects.remove(idx)
                if (removed != null) {
                    pushUndo(EditorAction(EditorAction.Type.Delete, idx, removed))
                }
            }
        }
        selectedObjects.clear()
        selectedObjectIndex = -1
        renderHitObjects()
    }

    fun copySelected() {
        clipboard.clear()
        val objects = beatmapData!!.hitObjects.objects
        for (idx in selectedObjects) {
            if (idx in objects.indices) {
                clipboard.add(objects[idx].deepClone()!!)
            }
        }
        if (selectedObjectIndex in objects.indices) {
            val alreadyCopied = clipboard.any { it === objects[selectedObjectIndex] }
            if (!alreadyCopied) {
                clipboard.add(objects[selectedObjectIndex].deepClone()!!)
            }
        }
    }

    fun pasteClipboard() {
        if (clipboard.isEmpty()) return
        val objects = beatmapData!!.hitObjects.objects
        var maxTime = 0f
        for (obj in objects) {
            if (obj.startTime > maxTime) maxTime = obj.startTime.toFloat()
        }
        val offset = maxTime + 100 - (if (clipboard.isEmpty()) 0f else clipboard[0].startTime.toFloat())

        for (obj in clipboard) {
            val clone = cloneWithTimeOffset(obj, offset)
            if (clone != null) {
                beatmapData!!.hitObjects.add(clone)
            }
        }
        renderHitObjects()
    }

    private fun cloneWithTimeOffset(obj: HitObject, timeOffset: Float): HitObject? {
        return when (obj) {
            is HitCircle -> HitCircle(obj.startTime + timeOffset, obj.position.x, obj.position.y)
            is Slider -> {
                val cps = ArrayList(obj.path.controlPoints)
                val path = SliderPath(obj.path.pathType, cps, obj.path.expectedDistance)
                val timing = getTimingControlPoint(obj.startTime + timeOffset)!!
                val diff = getDifficultyControlPoint(obj.startTime + timeOffset)!!
                Slider(
                    obj.startTime + timeOffset, Vector2(obj.position.x, obj.position.y),
                    timing, diff, obj.repeatCount, path, beatmapData!!.difficulty.sliderMultiplier,
                    beatmapData!!.difficulty.sliderTickRate, 1.0, true
                )
            }
            is Spinner -> Spinner(obj.startTime + timeOffset, obj.endTime + timeOffset)
            else -> null
        }
    }

    fun toggleMultiSelect() {
        isMultiSelecting = !isMultiSelecting
        if (!isMultiSelecting) selectedObjects.clear()
    }

    fun selectAll() {
        if (beatmapData == null) return
        selectedObjects.clear()
        val objects = beatmapData!!.hitObjects.objects
        for (i in objects.indices) { selectedObjects.add(i) }
        if (objects.isNotEmpty()) selectedObjectIndex = objects.size - 1
        renderHitObjects()
    }

    fun deselectAll() {
        selectedObjects.clear()
        selectedObjectIndex = -1
        renderHitObjects()
    }

    fun openComboEditor() {
        GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
            val fragment = EditorComboFragment()
            fragment.withEditor(this)
            fragment.show()
        }
    }

    fun openSliderEditor() {
        if (selectedObjectIndex < 0 || beatmapData == null) return
        val obj = beatmapData!!.hitObjects.objects[selectedObjectIndex]
        if (obj !is Slider) return
        GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
            val fragment = EditorSliderFragment()
            fragment.withEditor(this)
            fragment.show()
        }
    }

    fun openSpinnerEditor() {
        if (selectedObjectIndex < 0 || beatmapData == null) return
        val obj = beatmapData!!.hitObjects.objects[selectedObjectIndex]
        if (obj !is Spinner) return
        GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
            val fragment = EditorSpinnerFragment()
            fragment.withEditor(this)
            fragment.show()
        }
    }

    fun isMultiSelectMode(): Boolean = isMultiSelecting
    fun getNewComboFlags(): HashMap<Int, Boolean> = newComboFlags
    fun getKiaiFlags(): HashMap<Double, Boolean> = kiaiFlags
    fun setComboColorCount(count: Int) { comboColorCount = count }
    fun getComboColorCount(): Int = comboColorCount
    fun getBeatmapPath(): String? = beatmapPath

    fun testPlay() {
        val data = beatmapData
        val path = beatmapPath
        if (data == null || path == null) return
        GlobalManager.getInstance().getMainActivity()!!.runOnUiThread {
            try {
                val file = java.io.File(path)
                BeatmapEncoder.encode(data, file, kiaiFlags)

                val menu = GlobalManager.getInstance().songMenu
                if (menu != null) {
                    var track = menu.selectedTrack ?: GlobalManager.getInstance().selectedTrack
                    if (track != null) {
                        stop()
                        scene.unregisterUpdateHandler(this@EditorScene)
                        menu.game?.startGame(track, null)
                    } else {
                        ToastLogger.showText("No track selected for test play", true)
                    }
                }
            } catch (e: Exception) {
                ToastLogger.showText("Test play failed: ${e.message}", true)
            }
        }
    }

    fun back() {
        scene.unregisterUpdateHandler(this)
        stop()
        hideToolbar()
        var topOverlay: androidx.fragment.app.Fragment?
        while (com.edlplan.ui.ActivityOverlay.getTopOverlay().also { topOverlay = it } != null) {
            val overlay = topOverlay
            if (overlay is com.edlplan.ui.fragment.BackPressListener) {
                overlay.callDismissOnBackPress()
            } else break
        }
        GlobalManager.getInstance().editorScene = null
        val menu = GlobalManager.getInstance().songMenu
        if (menu != null) {
            menu.isEditorMode = true
            menu.show()
        } else {
            GlobalManager.getInstance().mainScene?.show()
        }
    }

    companion object {
        private const val GRID_SIZE = 16f
        private const val WAVEFORM_HEIGHT = 60f
        private const val PLAYBACK_BAR_WIDTH = 2f
        private val BEAT_SNAPS = floatArrayOf(1f, 0.5f, 1f / 3f, 0.25f, 1f / 6f, 0.125f)
        private val BEAT_SNAP_LABELS = arrayOf("1/1", "1/2", "1/3", "1/4", "1/6", "1/8")
        private const val OBJECT_VISIBLE_WINDOW_MS = 2000f
    }
}
