package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import android.os.Build
import android.os.SystemClock
import com.dgsrz.bancho.security.SecurityUtils
import com.edlplan.ext.EdExtensionHelper
import com.edlplan.framework.math.FMath
import com.edlplan.framework.support.ProxySprite
import com.edlplan.framework.support.osb.StoryboardSprite
import com.edlplan.framework.utils.functionality.SmartIterator
import com.edlplan.osu.support.timing.TimingPoints
import com.edlplan.osu.support.timing.controlpoint.ControlPoints
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.server.SpectateAPI
import com.reco1l.api.server.SocialAPI
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.discord.DiscordRPC
import com.reco1l.legacy.engine.BlankTextureRegion
import com.reco1l.legacy.engine.VideoSprite
import com.reco1l.legacy.ui.entity.InGameLeaderboard
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.spectate.SpectateBuffer
import com.reco1l.legacy.ui.spectate.SpectateCursorSprite
import com.reco1l.legacy.ui.spectate.SpectateFrame
import com.reco1l.legacy.ui.spectate.SpectatePacket
import com.reco1l.legacy.ui.spectate.SpectateState
import com.reco1l.legacy.ui.multiplayer.RoomScene
import com.reco1l.legacy.replay.ReplayOverlay
import com.reco1l.legacy.replay.ReplayOverlayFragment
import com.rian.difficultycalculator.attributes.TimedDifficultyAttributes
import com.rian.difficultycalculator.beatmap.hitobject.HitObject
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration
import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.SmoothCamera
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.engine.options.TouchOptions
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.ColorBackground
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.util.FPSCounter
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.audio.effect.Metronome
import ru.nsu.ccfit.zuev.audio.serviceAudio.PlayMode
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapCountdown
import ru.nsu.ccfit.zuev.osu.beatmap.constants.SampleBank
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity
import ru.nsu.ccfit.zuev.osu.game.cursor.main.AutoCursor
import ru.nsu.ccfit.zuev.osu.game.cursor.main.Cursor
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorEntity
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.menu.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import javax.microedition.khronos.opengles.GL10

class GameScene(private val engine: Engine) : IUpdateHandler, GameObjectListener,
    Scene.IOnSceneTouchListener {

    companion object {
        const val CursorCount = 3

    private fun getCursorCount(): Int = if (GameHelper.isRelaxMod()) 1 else CursorCount
    }

    @JvmField var filePath: String? = null
    @JvmField var hasFailed = false
    @JvmField var stat: StatisticV2? = null
    @JvmField var scoreBoard: InGameLeaderboard? = null

    internal var scene: Scene = Scene()
    private var bgScene = Scene()
    private var mgScene = Scene()
    private var fgScene = Scene()
    private var oldScene: Scene? = null
    private var beatmapData: BeatmapData? = null
    private var lastTrack: TrackInfo? = null
    private var scoringScene: ScoringScene? = null
    private var currentTimingPoint: TimingPoint? = null
    private var soundTimingPoint: TimingPoint? = null
    private var firstTimingPoint: TimingPoint? = null
    private var timingPoints: Queue<TimingPoint>? = null
    private var activeTimingPoints: Queue<TimingPoint>? = null
    private var trackMD5: String? = null
    private var lastObjectId = -1
    private var secPassed = 0f
    private var leadOut = 0f
    private var objects: LinkedList<GameObjectData>? = null
    private var allObjects: ArrayList<GameObjectData>? = null
    private var combos: ArrayList<RGBColor>? = null
    private var comboNum = -1
    private var currentComboNum = 0
    private var comboWasMissed = false
    private var comboWas100 = false
    private var activeObjects = ArrayList<GameObject>()
    private var passiveObjects = ArrayList<GameObject>()
    private var expiredObjects = LinkedList<GameObject>()
    private var comboText: GameScoreText? = null
    private var accText: GameScoreText? = null
    private var scoreText: GameScoreText? = null
    private var scoreShadow: GameScoreTextShadow? = null
    private var breakPeriods: Queue<BreakPeriod> = LinkedList()
    private var breakAnimator: BreakAnimator? = null
    private var scorebar: ScoreBar? = null
    private var progressBar: SongProgressBar? = null
    private var hitErrorMeter: HitErrorMeter? = null
    private var metronome: Metronome? = null
    private var isFirst = true
    private var scale = 0f
    private var approachRate = 0f
    private var rawDifficulty = 0f
    private var overallDifficulty = 0f
    private var rawDrain = 0f
    private var drain = 0f
    private var gameStarted = false
    private var totalOffset = 0f
    private var totalLength = Int.MAX_VALUE
    private var loadComplete = false
    private var paused = false
    private var lastPauseTime = 0L
    private var skipBtn: Sprite? = null
    private var skipTime = 0f
    private var musicStarted = false
    private var musicReady = false
    private var distToNextObject = 0.0
    private var timeMultiplier = 1.0f
    private var cursorSprites: Array<CursorEntity>? = null
    private var autoCursor: AutoCursor? = null
    private var flashlightSprite: FlashLightEntity? = null
    private var mainCursorId = -1
    private var replay: Replay? = null
    private var replaying = false
    private var replayFile: String? = null
    private var avgOffset = 0f
    private var offsetRegs = 0
    private var kiaiRect: Rectangle? = null
    private var dimRectangle: Rectangle? = null
    private var unranked: Sprite? = null
    private var replayText: ChangeableText? = null
    private var title: String? = null
    private var artist: String? = null
    private var version: String? = null
    private var comboBurst: ComboBurst? = null
    private var failcount = 0
    private var lastActiveObjectHitTime = 0f
    private var sliderPaths: Array<SliderPath?>? = null
    private var sliderIndex = 0
    private var replayOverlayFragment: ReplayOverlayFragment? = null
    private var pendingReplaySeekMs = -1
    private var storyboardSprite: StoryboardSprite? = null
    private var storyboardOverlayProxy: ProxySprite? = null
    private var difficultyHelper: DifficultyHelper = DifficultyHelper.StdDifficulty
    private var timedDifficultyAttributes: List<TimedDifficultyAttributes>? = null
    private var ppText: ChangeableText? = null
    private var previousFrameTime: Long = 0
    private var video: VideoSprite? = null
    private var videoOffset = 0f
    private var relaxCursorHeld = false
    private var waitingForCursorHold = false
    private var relaxLiftOffCount = 0
    private var relaxLiftOffWindowStart = 0L
    private var videoStarted = false
    private var lastBackPressTime = -1f
    private var isSkipRequested = false
    private var realTimeElapsed: Long = 0
    private var statisticDataTimeElapsed: Long = 0
    private var lastScoreSent: ScoreBoardItem? = null

    // Spectate: target side (streaming)
    private var spectateFrameCounter = 0
    private val SPECTATE_FRAME_INTERVAL = 1 // Send every frame (~60fps)
    private val spectateHitsSinceLastSend = mutableListOf<Pair<Int, Int>>()

    // Spectate: spectator side (receiving)
    var isSpectating = false
    var spectateTargetId = 0L
    private val spectateBuffer = SpectateBuffer()
    private var spectateStateApplied = false
    private var spectateCursorSprites: Array<SpectateCursorSprite?> = arrayOfNulls(3)
    private var spectateDataListener: io.socket.emitter.Emitter.Listener? = null
    private var spectateStateListener: io.socket.emitter.Emitter.Listener? = null

    private val cursors = arrayOfNulls<Cursor>(CursorCount)
    private val cursorIIsDown = BooleanArray(CursorCount)
    private val pressConsumedThisFrame = BooleanArray(CursorCount)
    private val kbKeyToSlot = HashMap<Int, Int>()
    private val strBuilder = StringBuilder()
    private val soundNameBuilder = StringBuilder()
    private val skipMaxPos = PointF()
    private val spinnerCenter = PointF()

    init {
        scene.attachChild(bgScene)
        scene.attachChild(mgScene)
        scene.attachChild(fgScene)
    }

    fun setScoringScene(sc: ScoringScene) { scoringScene = sc }
    fun setOldScene(oscene: Scene) { oldScene = oscene }
    fun getScene(): Scene = scene

    private fun setBackground() {
        dimRectangle = null
        video?.release()
        video = null
        var bgSprite: Sprite? = null

        if (Config.isVideoEnabled() && beatmapData!!.events.videoFilename != null
            && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || timeMultiplier == 1.0f)) {
            try {
                videoStarted = false
                videoOffset = beatmapData!!.events.videoStartTime / 1000f
                video = VideoSprite(lastTrack!!.beatmap!!.path + "/" + beatmapData!!.events.videoFilename, engine)
                video!!.setAlpha(0f)
                bgSprite = video as? Sprite
                storyboardSprite?.transparentBackground = true
            } catch (e: Exception) {
                e.printStackTrace()
                video = null
            }
        }

        if (storyboardSprite == null || !storyboardSprite!!.isStoryboardAvailable()) {
            if (bgSprite == null && beatmapData!!.events.backgroundFilename != null) {
                val tex = if (Config.isSafeBeatmapBg())
                    ResourceManager.getInstance().getTexture("menu-background")
                else
                    ResourceManager.getInstance().getTextureIfLoaded("::background")
                if (tex != null) bgSprite = Sprite(0f, 0f, tex)
            }
            if (bgSprite == null) {
                bgSprite = Sprite(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat(), BlankTextureRegion())
                if (beatmapData!!.events.backgroundColor != null)
                    beatmapData!!.events.backgroundColor!!.apply(bgSprite)
                else
                    bgSprite.setColor(0f, 0f, 0f)
            }
            dimRectangle = Rectangle(0f, 0f, bgSprite.width, bgSprite.height)
            dimRectangle!!.setColor(0f, 0f, 0f, 1.0f - Config.getBackgroundBrightness())
            bgSprite.attachChild(dimRectangle)
        } else {
            storyboardSprite!!.setBrightness(Config.getBackgroundBrightness())
        }

        bgSprite?.let {
            val factor = if (Config.isKeepBackgroundAspectRatio())
                Config.getRES_HEIGHT() / it.height
            else
                Config.getRES_WIDTH() / it.width
            it.setScale(factor)
            it.setPosition((Config.getRES_WIDTH() - it.width) / 2f, (Config.getRES_HEIGHT() - it.height) / 2f)
            scene.setBackground(SpriteBackground(it))
        }
    }

    private fun loadGame(track: TrackInfo, rFile: String?): Boolean {
        if (!SecurityUtils.verifyFileIntegrity(GlobalManager.getInstance().getMainActivity()!!)) {
            ToastLogger.showTextId(R.string.file_integrity_tampered, true)
            return false
        }

        if (rFile != null && rFile.startsWith("https://")) {
            this.replayFile = Config.getCachePath() + "/" + MD5Calculator.getStringMD5(rFile) + ".odr"
            Debug.i("ReplayFile = ${this.replayFile}")
            if (!OnlineFileOperator.downloadFile(rFile, this.replayFile!!)) {
                ToastLogger.showTextId(R.string.replay_cantdownload, true)
                return false
            }
        } else {
            this.replayFile = rFile
        }

        val parser = BeatmapParser(track.filename!!)
        if (parser.openFile()) {
            beatmapData = parser.parse(true)
        } else {
            Debug.e("startGame: cannot open file")
            ToastLogger.showText(StringTable.format(R.string.message_error_open, track.filename), true)
            return false
        }

        if (beatmapData?.md5 != track.md5) {
            ToastLogger.showText("Invalid beatmap file.", true)
            return false
        }

        if (beatmapData == null) return false

        SkinManager.getInstance().loadBeatmapSkin(beatmapData!!.folder ?: "")

        breakPeriods = LinkedList()
        for (period in beatmapData!!.events.breaks) {
            breakPeriods.add(BreakPeriod(period.start / 1000f, (period.start + period.length) / 1000f))
        }

        totalOffset = Config.getOffset()
        var beatmapName = track.filename!!
        beatmapName = beatmapName.substring(0, beatmapName.lastIndexOf('/'))
        val props = PropertiesLibrary.instance.getProperties(beatmapName)
        if (props != null) totalOffset += props.offset

        try {
            val musicFile = File(track.audioFilename!!)
            if (!musicFile.exists()) throw FileNotFoundException(musicFile.path)
            filePath = musicFile.path
        } catch (e: Exception) {
            Debug.e("Load Music: ${e.message}")
            ToastLogger.showText(e.message ?: "Unknown error", true)
            return false
        }

        title = beatmapData!!.metadata.title ?: ""
        artist = beatmapData!!.metadata.artist ?: ""
        version = beatmapData!!.metadata.version ?: ""

        scale = ((Config.getRES_HEIGHT() / 480.0f) * (54.42 - beatmapData!!.difficulty.cs * 4.48) * 2 / GameObjectSize.BASE_OBJECT_SIZE).toFloat() + 0.5f * Config.getScaleMultiplier()
        val rawApproachRate = beatmapData!!.difficulty.ar.toFloat()
        approachRate = (GameHelper.ar2ms(rawApproachRate.toDouble()) / 1000f).toFloat()
        overallDifficulty = beatmapData!!.difficulty.od.toFloat()
        drain = beatmapData!!.difficulty.hp.toFloat()
        rawDifficulty = overallDifficulty
        rawDrain = drain

        if (ModMenu.getInstance().mod.contains(GameMod.MOD_EASY)) {
            scale += 0.125f; drain *= 0.5f; overallDifficulty *= 0.5f
            approachRate = (GameHelper.ar2ms((rawApproachRate / 2f).toDouble()) / 1000f).toFloat()
        }
        GameHelper.setHardrock(false)
        if (ModMenu.getInstance().mod.contains(GameMod.MOD_HARDROCK)) {
            scale -= 0.125f; drain = minOf(1.4f * drain, 10f); overallDifficulty = minOf(1.4f * overallDifficulty, 10f)
            approachRate = (GameHelper.ar2ms(minOf(1.4f * rawApproachRate, 10f).toDouble()) / 1000f).toFloat()
            GameHelper.setHardrock(true)
        }

        timeMultiplier = 1f
        GameHelper.setDoubleTime(false); GameHelper.setNightCore(false); GameHelper.setHalfTime(false)
        val filePathVal = filePath!!
        GlobalManager.getInstance().songService!!.preLoad(filePathVal, 1.0f, false)
        GameHelper.setTimeMultiplier(1f)

        if (ModMenu.getInstance().changeSpeed != 1.00f) {
            timeMultiplier = ModMenu.getInstance().speed
            GlobalManager.getInstance().songService!!.preLoad(filePathVal, timeMultiplier, ModMenu.getInstance().isEnableNCWhenSpeedChange || ModMenu.getInstance().mod.contains(GameMod.MOD_NIGHTCORE))
            GameHelper.setTimeMultiplier(1 / timeMultiplier)
        } else if (ModMenu.getInstance().mod.contains(GameMod.MOD_DOUBLETIME)) {
            GlobalManager.getInstance().songService!!.preLoad(filePathVal, PlayMode.MODE_DT)
            timeMultiplier = 1.5f; GameHelper.setDoubleTime(true); GameHelper.setTimeMultiplier(2 / 3f)
        } else if (ModMenu.getInstance().mod.contains(GameMod.MOD_NIGHTCORE)) {
            GlobalManager.getInstance().songService!!.preLoad(filePathVal, PlayMode.MODE_NC)
            timeMultiplier = 1.5f; GameHelper.setNightCore(true); GameHelper.setTimeMultiplier(2 / 3f)
        } else if (ModMenu.getInstance().mod.contains(GameMod.MOD_HALFTIME)) {
            GlobalManager.getInstance().songService!!.preLoad(filePathVal, PlayMode.MODE_HT)
            timeMultiplier = 0.75f; GameHelper.setHalfTime(true); GameHelper.setTimeMultiplier(4 / 3f)
        }

        if (ModMenu.getInstance().mod.contains(GameMod.MOD_REALLYEASY)) {
            scale += 0.125f; drain *= 0.5f; overallDifficulty *= 0.5f
            var ar = GameHelper.ms2ar((approachRate * 1000f).toDouble()).toFloat()
            if (ModMenu.getInstance().mod.contains(GameMod.MOD_EASY)) { ar *= 2; ar -= 0.5f }
            ar -= (timeMultiplier - 1.0f) + 0.5f
            approachRate = (GameHelper.ar2ms(ar.toDouble()) / 1000f).toFloat()
        }

        if (ModMenu.getInstance().isCustomAR()) approachRate = (GameHelper.ar2ms(ModMenu.getInstance().customAR!!.toDouble()) / 1000f * timeMultiplier).toFloat()
        if (ModMenu.getInstance().isCustomOD()) overallDifficulty = ModMenu.getInstance().customOD!!
        if (ModMenu.getInstance().isCustomCS()) scale = (Config.getRES_HEIGHT() / 480.0f * (54.42f - ModMenu.getInstance().customCS!! * 4.48f) * 2f / GameObjectSize.BASE_OBJECT_SIZE + 0.5f * Config.getScaleMultiplier()).toFloat()
        if (ModMenu.getInstance().isCustomHP()) drain = ModMenu.getInstance().customHP!!

        GameHelper.setRelaxMod(ModMenu.getInstance().mod.contains(GameMod.MOD_RELAX))
        GameHelper.setAutopilotMod(ModMenu.getInstance().mod.contains(GameMod.MOD_AUTOPILOT))
        GameHelper.setAuto(ModMenu.getInstance().mod.contains(GameMod.MOD_AUTO))
        GameHelper.setStackLeniency(beatmapData!!.general.stackLeniency)
        if (scale < 0.001f) scale = 0.001f
        GameHelper.setSpeed(beatmapData!!.difficulty.sliderMultiplier * 100)
        GameHelper.tickRate = beatmapData!!.difficulty.sliderTickRate.toFloat()
        GameHelper.scale = scale
        GameHelper.setDifficulty(overallDifficulty)
        GameHelper.drain = drain
        GameHelper.approachRate = approachRate

        objects = LinkedList(); allObjects = ArrayList()
        for (s in beatmapData!!.rawHitObjects) {
            val data = GameObjectData(s)
            objects!!.add(data); allObjects!!.add(data)
        }
        if (objects!!.isEmpty()) { ToastLogger.showText("Empty Beatmap", true); return false }

        activeObjects = ArrayList(); passiveObjects = ArrayList(); expiredObjects = LinkedList()
        lastObjectId = -1

        GameHelper.sliderColor = SkinManager.getInstance().getSliderColor()
        beatmapData!!.colors.sliderBorderColor?.let { GameHelper.sliderColor = it }
        if (OsuSkin.get().isForceOverrideSliderBorderColor()) GameHelper.sliderColor = OsuSkin.get().sliderBorderColor.currentValue

        combos = ArrayList()
        for (color in beatmapData!!.colors.comboColors) combos!!.add(RGBColor(color.r() / 255f, color.g() / 255f, color.b() / 255f))
        if (combos!!.isEmpty() || Config.isUseCustomComboColors()) { combos!!.clear(); combos!!.addAll(Config.getComboColors().filterNotNull()) }
        if (OsuSkin.get().isForceOverrideComboColor()) { combos!!.clear(); combos!!.addAll(OsuSkin.get().comboColor) }
        comboNum = -1; currentComboNum = 0; lastActiveObjectHitTime = 0f

        val defSound = beatmapData!!.general.sampleBank
        TimingPoint.setDefaultSound(if (defSound == SampleBank.soft) "soft" else "normal")
        timingPoints = LinkedList(); activeTimingPoints = LinkedList()

        currentTimingPoint = TimingPoint(arrayOf("0", "1000", "4", "0", "0", "100", "1", "0"), null)
        val rawTPs = beatmapData!!.rawTimingPoints
        val tpCount = rawTPs.size
        val splitTPs = Array(tpCount) { rawTPs[it].split(",").toTypedArray() }
        for (pars in splitTPs) {
            if (pars.size >= 2) {
                val ms = pars[1].trim()
                if (ms.isNotEmpty() && ms[0] != '-') { currentTimingPoint = TimingPoint(pars, currentTimingPoint); break }
            }
        }
        for (pars in splitTPs) {
            val tp = TimingPoint(pars, currentTimingPoint)
            timingPoints!!.add(tp)
            if (!tp.wasInderited()) currentTimingPoint = tp
        }

        GameHelper.controlPoints = ControlPoints()
        GameHelper.controlPoints.load(TimingPoints.parse(beatmapData!!.rawTimingPoints))
        currentTimingPoint = timingPoints!!.peek()
        firstTimingPoint = currentTimingPoint
        soundTimingPoint = currentTimingPoint
        if (soundTimingPoint != null) {
            GameHelper.setTimingOffset(soundTimingPoint!!.time)
            GameHelper.setBeatLength(soundTimingPoint!!.beatLength * GameHelper.speed / 100f)
            GameHelper.timeSignature = soundTimingPoint!!.signature
            GameHelper.isKiai = soundTimingPoint!!.isKiai()
        } else {
            GameHelper.setTimingOffset(0); GameHelper.setBeatLength(1); GameHelper.timeSignature = 4; GameHelper.isKiai = false
        }
        GameHelper.initalBeatLength = GameHelper.beatLength

        GameObjectPool.getInstance().purge(); SpritePool.getInstance().purge(); GameHelper.clearPools(); ModifierFactory.clear()

        avgOffset = 0f; offsetRegs = 0
        val trackFile = File(track.filename)
        trackMD5 = track.md5; replaying = false
        replay = Replay()
        replay!!.setObjectCount(objects!!.size)
        replay!!.setMap(trackFile.parentFile.name, trackFile.name, trackMD5!!)

        if (replayFile != null) {
            replaying = replay!!.load(replayFile!!)
            if (!replaying) { ToastLogger.showTextId(R.string.replay_invalid, true); return false }
            else replay!!.countMarks(overallDifficulty)
        } else if (ModMenu.getInstance().mod.contains(GameMod.MOD_AUTO)) { replay = null }

        if (!replaying && !isSpectating) OnlineScoring.getInstance().startPlay(track, trackMD5!!)

        if (!replaying && !isSpectating) {
            com.reco1l.framework.lang.Execution.asyncIgnoreExceptions {
                SocialAPI.updateStatus(OnlineManager.getInstance().userId, "playing")
            }
        }
        if (Config.isEnableStoryboard()) { val fn = track.filename; storyboardSprite!!.loadStoryboard(fn!!) }

        GameObjectPool.getInstance().preload()

        ppText = null
        if (Config.isDisplayRealTimePPCounter()) {
            val parameters = DifficultyCalculationParameters()
            val modMenu = ModMenu.getInstance()
            parameters.mods = modMenu.mod.clone()
            parameters.customSpeedMultiplier = modMenu.changeSpeed
            if (modMenu.isCustomCS()) parameters.customCS = modMenu.customCS!!
            if (modMenu.isCustomAR()) parameters.customAR = modMenu.customAR!!
            if (modMenu.isCustomOD()) parameters.customOD = modMenu.customOD!!
            val calcData = BeatmapParser(track.filename!!).setCalculator(true).parse(true)
            timedDifficultyAttributes = calcData?.let { BeatmapDifficultyCalculator.calculateTimedDifficulty(it, parameters) }
        } else { timedDifficultyAttributes = emptyList() }

        lastTrack = track; stackNotes(); calculateAllSliderPaths()

        Multiplayer.finalData = null; hasFailed = false; lastBackPressTime = -1f; isSkipRequested = false; realTimeElapsed = 0; statisticDataTimeElapsed = 0; lastScoreSent = null; relaxLiftOffCount = 0; relaxLiftOffWindowStart = 0L
        paused = false; gameStarted = false
        return true
    }

    fun restartGame() {
        dismissReplayOverlay()
        if (!replaying) EdExtensionHelper.onRestartGame(lastTrack)
        startGame(null, null)
    }

    fun startGame(track: TrackInfo?, replayFile: String?) {
        org.anddev.andengine.opengl.texture.TextureManager.setSuppressGC(true)
        GameHelper.updateGameid()
        if (!replaying) EdExtensionHelper.onStartGame(track)

        scene = Scene()
        if (Config.isEnableStoryboard()) {
            if (storyboardSprite == null || storyboardOverlayProxy == null) {
                storyboardSprite = StoryboardSprite(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
                storyboardOverlayProxy = ProxySprite(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
                val overlayProxy = storyboardOverlayProxy!!
                storyboardSprite!!.setOverlayDrawProxy(overlayProxy)
                scene.attachChild(storyboardSprite)
            }
            storyboardSprite?.detachSelf()
            scene.attachChild(storyboardSprite)
        }
        bgScene = Scene(); mgScene = Scene(); fgScene = Scene()
        scene.attachChild(bgScene); scene.attachChild(mgScene)
        storyboardOverlayProxy?.let { it.detachSelf(); scene.attachChild(it) }
        scene.attachChild(fgScene)
        scene.setBackground(ColorBackground(0f, 0f, 0f))
        bgScene.setBackgroundEnabled(false); mgScene.setBackgroundEnabled(false); fgScene.setBackgroundEnabled(false)
        isFirst = true; failcount = 0; mainCursorId = -1
        val screen = LoadingScreen()
        engine.setScene(screen.scene)
        val rfile = if (track != null) replayFile else this.replayFile
        Execution.async {
            if (loadGame(track ?: lastTrack!!, rfile)) prepareScene()
            else {
                ModMenu.getInstance().setMod(Replay.oldMod)
                ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed)
                ModMenu.getInstance().FLfollowDelay = Replay.oldFLFollowDelay
                ModMenu.getInstance().setCustomAR(Replay.oldCustomAR)
                ModMenu.getInstance().setCustomOD(Replay.oldCustomOD)
                ModMenu.getInstance().setCustomCS(Replay.oldCustomCS)
                ModMenu.getInstance().setCustomHP(Replay.oldCustomHP)
                quit()
            }
        }
        ResourceManager.getInstance().getSound("failsound").stop()
    }

    private fun prepareScene() {
        val _bi = lastTrack?.beatmap
        val titleUnicode = _bi?.titleUnicode
        val artistUnicode = _bi?.artistUnicode
        val _title = if (_bi != null && titleUnicode != null && titleUnicode.isNotEmpty()) titleUnicode
            else _bi?.title ?: "Unknown"
        val _artist = if (_bi != null && artistUnicode != null && artistUnicode.isNotEmpty()) artistUnicode
            else _bi?.artist ?: "Unknown"
        val _difficulty = lastTrack?.mode ?: ""
        if (replaying) DiscordRPC.updateForReplay(_artist, _title, _difficulty)
        else DiscordRPC.updateForPlaying(Multiplayer.isMultiplayer, _artist, _title, _difficulty)
        scene.setOnSceneTouchListener(this)
        if (GlobalManager.getInstance().camera is SmoothCamera) {
            val camera = GlobalManager.getInstance().camera as SmoothCamera
            camera.setZoomFactorDirect(Config.getPlayfieldSize())
            if (Config.isShrinkPlayfieldDownwards()) camera.setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f * Config.getPlayfieldSize())
        }
        setBackground()

        if (Config.isShowFPS() || Config.isDisplayRealTimePPCounter()) {
            val font = ResourceManager.getInstance().getFont("smallFont")
            val urText = ChangeableText(Utils.toRes(720).toFloat(), Utils.toRes(480).toFloat(), font, "00.00 UR    ")
            val accTextOverlay = ChangeableText(Utils.toRes(720).toFloat(), Utils.toRes(440).toFloat(), font, "Avg offset: 0ms     ")
            val fpsOverlayH = font.lineHeight + 12
            accTextOverlay.setPosition(Config.getRES_WIDTH() - accTextOverlay.width - 5, Config.getRES_HEIGHT() - accTextOverlay.height - 10 - fpsOverlayH)
            urText.setPosition(Config.getRES_WIDTH() - urText.width - 5, accTextOverlay.y - urText.height)
            fgScene.attachChild(accTextOverlay); fgScene.attachChild(urText)

            if (Config.isDisplayRealTimePPCounter()) {
                ppText = ChangeableText(Utils.toRes(720).toFloat(), Utils.toRes(440).toFloat(), font, "0.00pp", 100)
                fgScene.attachChild(ppText)
            }
            var memText: ChangeableText? = null
            if (BuildConfig.DEBUG) { memText = ChangeableText(Utils.toRes(780).toFloat(), Utils.toRes(520).toFloat(), font, "0 MB/0 MB    "); fgScene.attachChild(memText) }
            fgScene.registerUpdateHandler(object : FPSCounter() {
                var elapsedInt = 0
                override fun onUpdate(pSecondsElapsed: Float) {
                    super.onUpdate(pSecondsElapsed); elapsedInt++
                    if (mSecondsElapsed >= 1f) reset()
                    strBuilder.setLength(0)
                    if (offsetRegs != 0 && elapsedInt > 200) {
                        val mean = avgOffset / offsetRegs
                        accTextOverlay.setText(strBuilder.append("Avg offset: ").append((mean * 1000f).toInt()).append("ms").toString())
                        strBuilder.setLength(0); elapsedInt = 0
                    }
                    val ur = stat!!.unstableRate.toFloat()
                    val urInt = (ur * 100 + 0.5f).toInt()
                    strBuilder.append(urInt / 100).append('.').append(if (urInt % 100 < 10) "0" else "").append(urInt % 100).append(" UR    ")
                    urText.setText(strBuilder.toString()); strBuilder.setLength(0)
                    accTextOverlay.setPosition(Config.getRES_WIDTH() - accTextOverlay.width - 5, Config.getRES_HEIGHT() - accTextOverlay.height - 10 - fpsOverlayH)
                    urText.setPosition(Config.getRES_WIDTH() - urText.width - 5, accTextOverlay.y - urText.height)
                    ppText?.setPosition(Config.getRES_WIDTH() - ppText!!.width - 5, urText.y - ppText!!.height)
                    memText?.let {
                        val runtime = Runtime.getRuntime()
                        val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                        val total = runtime.totalMemory() / 1024 / 1024
                        strBuilder.append(used).append(" MB/").append(total).append(" MB    ")
                        it.setText(strBuilder.toString()); strBuilder.setLength(0)
                        it.setPosition(Config.getRES_WIDTH() - it.width - 5, (ppText ?: urText).y - it.height)
                    }
                }
            })
        }

        stat = StatisticV2()
        stat!!.setMod(ModMenu.getInstance().mod)
        stat!!.canFail = !stat!!.mod.contains(GameMod.MOD_NOFAIL) && !stat!!.mod.contains(GameMod.MOD_RELAX) && !stat!!.mod.contains(GameMod.MOD_AUTOPILOT) && !stat!!.mod.contains(GameMod.MOD_AUTO)

        var multiplier = 1f + minOf(rawDifficulty, 10f) / 10f + minOf(rawDrain, 10f) / 10f
        multiplier += (minOf(beatmapData!!.difficulty.cs, 17.62f) - 3f) / 4f
        stat!!.diffModifier = multiplier
        stat!!.maxObjectsCount = lastTrack!!.totalHitObjectCount
        stat!!.maxHighestCombo = lastTrack!!.maxCombo
        stat!!.beatmapCS = beatmapData!!.difficulty.cs.toFloat()
        stat!!.beatmapOD = beatmapData!!.difficulty.od.toFloat()
        stat!!.setCustomAR(ModMenu.getInstance().customAR)
        stat!!.setCustomOD(ModMenu.getInstance().customOD)
        stat!!.setCustomCS(ModMenu.getInstance().customCS)
        stat!!.setCustomHP(ModMenu.getInstance().customHP)
        stat!!.setChangeSpeed(ModMenu.getInstance().changeSpeed)
        stat!!.setFLFollowDelay(ModMenu.getInstance().FLfollowDelay)

        GameHelper.setHardrock(stat!!.mod.contains(GameMod.MOD_HARDROCK))
        GameHelper.setDoubleTime(stat!!.mod.contains(GameMod.MOD_DOUBLETIME))
        GameHelper.setNightCore(stat!!.mod.contains(GameMod.MOD_NIGHTCORE))
        GameHelper.setHalfTime(stat!!.mod.contains(GameMod.MOD_HALFTIME))
        GameHelper.setHidden(stat!!.mod.contains(GameMod.MOD_HIDDEN))
        GameHelper.setFlashLight(stat!!.mod.contains(GameMod.MOD_FLASHLIGHT))
        GameHelper.setRelaxMod(stat!!.mod.contains(GameMod.MOD_RELAX))
        GameHelper.setAutopilotMod(stat!!.mod.contains(GameMod.MOD_AUTOPILOT))
        GameHelper.setSuddenDeath(stat!!.mod.contains(GameMod.MOD_SUDDENDEATH))
        GameHelper.setPerfect(stat!!.mod.contains(GameMod.MOD_PERFECT))
        GameHelper.setScoreV2(stat!!.mod.contains(GameMod.MOD_SCOREV2))
        GameHelper.setEasy(stat!!.mod.contains(GameMod.MOD_EASY))
        difficultyHelper = if (stat!!.mod.contains(GameMod.MOD_PRECISE)) DifficultyHelper.HighDifficulty else DifficultyHelper.StdDifficulty
        GameHelper.difficultyHelper = difficultyHelper

        // Spectating: spectator never fails, host controls game state
        if (isSpectating) {
            stat!!.canFail = false
        }

        for (i in 0 until CursorCount) { cursors[i] = Cursor(); cursors[i]!!.mouseDown = false; cursors[i]!!.mousePressed = false; cursors[i]!!.mouseOldDown = false }
        cursorIIsDown.fill(false); kbKeyToSlot.clear()
        comboWas100 = false; comboWasMissed = false

        val leadIn = beatmapData!!.general.audioLeadIn
        previousFrameTime = 0; secPassed = -leadIn / 1000f
        if (secPassed > -1) secPassed = -1f
        if (video != null && videoOffset < 0) secPassed = minOf(videoOffset, secPassed)

        skipTime = if (!objects.isNullOrEmpty()) objects!!.peek().getTime() - approachRate - 1f else 0f

        metronome = null
        if ((Config.getMetronomeSwitch() == 1 && GameHelper.isNightCore()) || Config.getMetronomeSwitch() == 2) metronome = Metronome()

        secPassed -= Config.getOffset() / 1000f
        if (secPassed > 0) { skipTime -= secPassed; secPassed = 0f }
        distToNextObject = 0.0

        if (isSpectating) {
            // Spectating: need cursorSprites to render remote cursor, skip AUTO cursor
            cursorSprites = Array(CursorCount) { CursorEntity().also { it.attachToScene(fgScene) } }
        } else if ((replaying || Config.isShowCursor()) && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()) {
            cursorSprites = Array(CursorCount) { CursorEntity().also { it.attachToScene(fgScene) } }
        } else { cursorSprites = null }

        if (!isSpectating && (GameHelper.isAuto() || GameHelper.isAutopilotMod())) { autoCursor = AutoCursor(); autoCursor!!.attachToScene(fgScene) }

        if (GameHelper.isRelaxMod() && !replaying) {
            val centerX = Config.getRES_WIDTH() / 2f
            val centerY = Config.getRES_HEIGHT() / 2f
            cursors[0]!!.mousePos.x = centerX
            cursors[0]!!.mousePos.y = centerY
            cursorSprites?.get(0)?.setPosition(centerX, centerY)
            cursorSprites?.get(0)?.setShowing(true)
            waitingForCursorHold = true
            relaxCursorHeld = false
        }

        val countdown = beatmapData!!.general.countdown
        if (Config.isCorovans() && countdown != null) {
            val cdSpeed = countdown.speed.toFloat()
            skipTime -= cdSpeed * Countdown.COUNTDOWN_LENGTH
            if (cdSpeed != 0f && objects!!.peek().getTime() - secPassed >= cdSpeed * Countdown.COUNTDOWN_LENGTH) {
                addPassiveObject(Countdown(this, bgScene, cdSpeed, 0f, objects!!.peek().getTime() - secPassed))
            }
        }

        val lastObjectTime = if (!objects.isNullOrEmpty()) objects!!.last.getTime() else 0f
        if (!Config.isHideInGameUI()) {
            progressBar = SongProgressBar(this, fgScene, lastObjectTime, objects!!.first.getTime(), PointF(0f, Config.getRES_HEIGHT().toFloat() - 7), Config.getRES_WIDTH().toFloat(), 7f)
            progressBar!!.setProgressRectColor(RGBAColor(153f / 255f, 204f / 255f, 51f / 255f, 0.4f))
        }

        if (Config.getErrorMeter() == 1 || (Config.getErrorMeter() == 2 && replaying)) {
            hitErrorMeter = HitErrorMeter(fgScene, PointF(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT().toFloat() - 20), overallDifficulty, 12f, difficultyHelper)
        }

        skipBtn = null
        if (skipTime > 1) {
            val tex: TextureRegion
            if (ResourceManager.getInstance().isTextureLoaded("play-skip-0")) {
                val loadedSkipTextures = ArrayList<String>()
                for (i in 0 until 60) { if (ResourceManager.getInstance().isTextureLoaded("play-skip-$i")) loadedSkipTextures.add("play-skip-$i") }
                tex = ResourceManager.getInstance().getTexture("play-skip-0")!!
                skipBtn = AnimSprite(Config.getRES_WIDTH() - tex.width.toFloat(), Config.getRES_HEIGHT() - tex.height.toFloat(), loadedSkipTextures.size.toFloat(), *loadedSkipTextures.toTypedArray())
            } else {
                tex = ResourceManager.getInstance().getTexture("play-skip")!!
                skipBtn = Sprite(Config.getRES_WIDTH() - tex.width.toFloat(), Config.getRES_HEIGHT() - tex.height.toFloat(), tex)
            }
            skipBtn!!.setAlpha(0.7f); fgScene.attachChild(skipBtn)
        }
        GameHelper.globalTime = 0.0

        var effectOffset = 155 - 25f
        breakAnimator = BreakAnimator(this, fgScene, stat!!, beatmapData!!.general.letterboxInBreaks, dimRectangle)
        if (!Config.isHideInGameUI()) {
            scorebar = ScoreBar(this, fgScene, stat!!); addPassiveObject(scorebar!!)
            val scoreDigitTex = ResourceManager.getInstance().getTexture("score-0")!!
            accText = GameScoreText(OsuSkin.get().scorePrefix, Config.getRES_WIDTH() - scoreDigitTex.width * 4.75f, 50f, "000.00%", 0.6f)
            comboText = GameScoreText(OsuSkin.get().comboPrefix, Utils.toRes(2).toFloat(), Config.getRES_HEIGHT() - Utils.toRes(95).toFloat(), "0000x", 1.5f)
            comboText!!.changeText("0****")
            scoreText = GameScoreText(OsuSkin.get().scorePrefix, Config.getRES_WIDTH() - scoreDigitTex.width * 7.25f, 0f, "0000000000", 0.9f)
            comboText!!.attachToScene(fgScene); accText!!.attachToScene(fgScene); scoreText!!.attachToScene(fgScene)
            if (Config.isComplexAnimations()) {
                scoreShadow = GameScoreTextShadow(0f, Config.getRES_HEIGHT() - Utils.toRes(90).toFloat(), "0000x", 1.5f, comboText!!)
                scoreShadow!!.attachToScene(bgScene); passiveObjects.add(scoreShadow!!)
            }
            if (stat!!.mod.contains(GameMod.MOD_AUTO)) { fgScene.attachChild(Sprite(Utils.toRes(Config.getRES_WIDTH() - 140).toFloat(), Utils.toRes(100).toFloat(), ResourceManager.getInstance().getTexture("selection-mod-autoplay"))); effectOffset += 25f }
            else if (stat!!.mod.contains(GameMod.MOD_RELAX)) { fgScene.attachChild(Sprite(Utils.toRes(Config.getRES_WIDTH() - 140).toFloat(), Utils.toRes(98).toFloat(), ResourceManager.getInstance().getTexture("selection-mod-relax"))); effectOffset += 25f }
            else if (stat!!.mod.contains(GameMod.MOD_AUTOPILOT)) { fgScene.attachChild(Sprite(Utils.toRes(Config.getRES_WIDTH() - 140).toFloat(), Utils.toRes(98).toFloat(), ResourceManager.getInstance().getTexture("selection-mod-relax2"))); effectOffset += 25f }
            if (Config.isComboburst()) { comboBurst = ComboBurst(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat()); comboBurst!!.attachAll(bgScene) }
        }

        var timeOffset = 0f
        fun addModEffect(name: String) {
            val effect = GameObjectPool.getInstance().getEffect(name)
            effect.init(fgScene, PointF(Utils.toRes(Config.getRES_WIDTH() - effectOffset).toFloat(), Utils.toRes(130).toFloat()), scale,
                SequenceEntityModifier(ModifierFactory.newScaleModifier(0.25f, 1.2f, 1f), ModifierFactory.newDelayModifier(2f - timeOffset),
                    ParallelEntityModifier(ModifierFactory.newFadeOutModifier(0.5f), ModifierFactory.newScaleModifier(0.5f, 1f, 1.5f))))
            effectOffset += 25f; timeOffset += 0.25f
        }
        if (stat!!.mod.contains(GameMod.MOD_SCOREV2)) addModEffect("selection-mod-scorev2")
        if (stat!!.mod.contains(GameMod.MOD_EASY)) addModEffect("selection-mod-easy")
        else if (stat!!.mod.contains(GameMod.MOD_HARDROCK)) addModEffect("selection-mod-hardrock")
        if (stat!!.mod.contains(GameMod.MOD_NOFAIL)) addModEffect("selection-mod-nofail")
        if (stat!!.mod.contains(GameMod.MOD_HIDDEN)) addModEffect("selection-mod-hidden")
        if (stat!!.mod.contains(GameMod.MOD_DOUBLETIME)) addModEffect("selection-mod-doubletime")
        if (stat!!.mod.contains(GameMod.MOD_NIGHTCORE)) addModEffect("selection-mod-nightcore")
        if (stat!!.mod.contains(GameMod.MOD_HALFTIME)) addModEffect("selection-mod-halftime")
        if (stat!!.mod.contains(GameMod.MOD_PRECISE)) addModEffect("selection-mod-precise")
        if (stat!!.mod.contains(GameMod.MOD_SUDDENDEATH)) addModEffect("selection-mod-suddendeath")
        else if (stat!!.mod.contains(GameMod.MOD_PERFECT)) addModEffect("selection-mod-perfect")
        if (stat!!.mod.contains(GameMod.MOD_FLASHLIGHT)) addModEffect("selection-mod-flashlight")
        if (stat!!.mod.contains(GameMod.MOD_REALLYEASY)) addModEffect("selection-mod-reallyeasy")

        kiaiRect = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        kiaiRect!!.isVisible = false; kiaiRect!!.setColor(1f, 1f, 1f); bgScene.attachChild(kiaiRect, 0)

        unranked = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("play-unranked"))
        unranked!!.setPosition(Config.getRES_WIDTH() / 2f - unranked!!.width / 2, 80f); unranked!!.isVisible = false; fgScene.attachChild(unranked)

        if (SmartIterator.wrap(stat!!.mod.iterator()).applyFilter { m -> m.unranked }.hasNext()
            || ModMenu.getInstance().isCustomAR() || ModMenu.getInstance().isCustomOD() || ModMenu.getInstance().isCustomCS() || ModMenu.getInstance().isCustomHP() || !ModMenu.getInstance().isDefaultFLFollowDelay())
            unranked!!.isVisible = true

        var playname = Config.getLocalUsername()
        replayText = ChangeableText(0f, 0f, ResourceManager.getInstance().getFont("font"), "", 1000)
        replayText!!.isVisible = false; replayText!!.setPosition(0f, 140f); replayText!!.setAlpha(0.7f); fgScene.attachChild(replayText, 0)
        if (stat!!.mod.contains(GameMod.MOD_AUTO) || replaying) {
            playname = if (replaying) GlobalManager.getInstance().scoring!!.getReplayStat()?.playerName ?: "osu!" else "osu!"
            replayText!!.text = "Watching $playname play $artist - $title [$version]"
            replayText!!.registerEntityModifier(LoopEntityModifier(MoveXModifier(40f, Config.getRES_WIDTH() + 5f, -replayText!!.width - 5f)))
            replayText!!.isVisible = !Config.isHideReplayMarquee()
        } else if (Multiplayer.room != null && Multiplayer.room!!.isTeamVersus) playname = Multiplayer.player!!.team.toString()
        else if (OnlineManager.getInstance().isStayOnline) playname = Config.getOnlineUsername()

        if (Config.isShowScoreboard()) { scoreBoard = InGameLeaderboard(playname, stat!!); fgScene.attachChild(scoreBoard) }
        if (GameHelper.isFlashLight()) { flashlightSprite = FlashLightEntity(); fgScene.attachChild(flashlightSprite, 0) }

        if (Multiplayer.isMultiplayer) { RoomAPI.notifyBeatmapLoaded(); return }
        start()
    }

    fun start() {
        if (skipTime <= 1) RoomScene.chat.dismiss()
        SeasonalBackgroundManager.stopPeriodicRefresh()
        leadOut = 0f; musicStarted = false; musicReady = false
        val touchOptions = TouchOptions()
        touchOptions.setRunOnUpdateThread(false)
        engine.touchController.applyTouchOptions(touchOptions)
        engine.setScene(scene)
        scene.registerUpdateHandler(this)

        // Spectate: send map start event to spectators
        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
            // Register spectator count listener (server sends this when spectators join/leave)
            SpectateAPI.onCount { count ->
                android.util.Log.d("GameScene", "Spectator count updated: $count")
            }

            Execution.asyncIgnoreExceptions {
                val modsStr = ModMenu.getInstance().mod.joinToString(",") { it.name }
                SpectateAPI.sendMapStart(OnlineManager.getInstance().userId, trackMD5 ?: "", modsStr)
            }
        }
    }

    fun getComboColor(num: Int): RGBColor = combos!![num % combos!!.size]

    override fun onUpdate(pSecondsElapsed: Float) {
        previousFrameTime = SystemClock.uptimeMillis()
        Utils.clearSoundMask()

        if (pendingReplaySeekMs >= 0) { val pos = pendingReplaySeekMs; pendingReplaySeekMs = -1; processReplaySeek(pos) }

        if (waitingForCursorHold) {
            if (relaxCursorHeld) {
                waitingForCursorHold = false
                if (GlobalManager.getInstance().songService?.status != Status.PLAYING && secPassed > 0) {
                    GlobalManager.getInstance().songService!!.play()
                    GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume())
                }
            }
            if (cursorSprites != null) { for (i in 0 until getCursorCount()) cursorSprites!![i].update(pSecondsElapsed) }
            return
        }

        var dt = pSecondsElapsed * timeMultiplier
        if (GlobalManager.getInstance().songService!!.status == Status.PLAYING) {
            val audioPos = GlobalManager.getInstance().songService!!.position / 1000.0f
            if (!musicReady && audioPos > 0) musicReady = true
            if (musicReady) {
                val offset = totalOffset / 1000f
                val realsecPassed = audioPos
                val criticalError = if (Config.isSyncMusic()) 0.1f else 0.5f
                val normalError = if (Config.isSyncMusic()) dt else 0.05f
                if (secPassed + offset - realsecPassed > criticalError) secPassed = realsecPassed - offset
                else if (Math.abs(secPassed + offset - realsecPassed) > normalError) {
                    if (secPassed + offset > realsecPassed) dt /= 2f else dt *= 2f
                }
                secPassed += dt
            }
        }

        if (replaying && musicStarted) ReplayOverlay.updatePosition((secPassed * 1000).toInt())

        if (Multiplayer.isMultiplayer) {
            val mSecElapsed = (pSecondsElapsed * 1000).toLong()
            realTimeElapsed += mSecElapsed; statisticDataTimeElapsed += mSecElapsed
            if (statisticDataTimeElapsed > 3000) {
                statisticDataTimeElapsed %= 3000
                if (Multiplayer.isConnected) {
                    val liveScore = stat!!.toBoardItem()
                    if (liveScore != lastScoreSent) { lastScoreSent = liveScore; Execution.asyncIgnoreExceptions { RoomAPI.submitLiveScore(lastScoreSent!!.toJson()) } }
                }
            }
        }

        // Spectate: stream cursor data to spectators only if someone is watching
        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline && SpectateAPI.spectatorCount > 0 && Config.isAllowSpectating) {
            spectateFrameCounter++
            if (spectateFrameCounter >= SPECTATE_FRAME_INTERVAL) {
                spectateFrameCounter = 0
                val timeMs = (secPassed * 1000).toInt()
                val hitsToSend: List<Pair<Int, Int>>
                synchronized(spectateHitsSinceLastSend) {
                    hitsToSend = spectateHitsSinceLastSend.toList()
                    spectateHitsSinceLastSend.clear()
                }

                val targetId = OnlineManager.getInstance().userId
                Execution.asyncIgnoreExceptions {
                    SpectateAPI.sendSpectateData(
                        targetId = targetId,
                        timeMs = timeMs,
                        cursors = cursors,
                        score = stat?.totalScoreWithMultiplier ?: 0,
                        combo = stat?.getCombo() ?: 0,
                        acc = stat?.getAccuracy() ?: 0f,
                        hp = stat?.hp ?: 1f,
                        alive = stat?.isAlive ?: true,
                        hits = hitsToSend
                    )
                }
            }
        }

        // Spectate: receive and apply remote cursor data
        if (isSpectating && !spectateBuffer.isEmpty()) {
            val timeMs = (secPassed * 1000).toInt()
            val packet = spectateBuffer.getPacketAt(timeMs)
            if (packet != null) {
                // Apply cursor positions
                for (i in 0 until minOf(3, packet.cursors.size)) {
                    val cursorData = packet.cursors[i]
                    if (i < cursors.size && cursors[i] != null) {
                        cursors[i]!!.mousePos.x = cursorData.x
                        cursors[i]!!.mousePos.y = cursorData.y
                        if (cursorData.down && !cursors[i]!!.mouseDown) {
                            cursors[i]!!.mouseDown = true
                            cursors[i]!!.mousePressed = true
                        } else if (!cursorData.down && cursors[i]!!.mouseDown) {
                            cursors[i]!!.mouseDown = false
                        }
                    }
                }
                // Apply score state
                if (stat != null) {
                    stat!!.setScore(packet.score)
                    // Note: combo/acc/hp are applied visually via UI
                }
            }
        }

        val gtime = if (soundTimingPoint == null || soundTimingPoint!!.time > secPassed) 0.0
            else (secPassed - firstTimingPoint!!.time) % GameHelper.kiaiTickLength
        GameHelper.globalTime = gtime

        if (Config.isEnableStoryboard()) storyboardSprite?.updateTime((secPassed * 1000).toDouble())

        if (replaying) {
            for (i in replay!!.cursorIndex.indices) {
                if (replay!!.cursorMoves.size <= i) break
                var cIndex = replay!!.cursorIndex[i]
                var movement: Replay.ReplayMovement? = null
                while (cIndex < replay!!.cursorMoves[i].size && run { movement = replay!!.cursorMoves[i].movements[cIndex]; movement!! }.time <= (secPassed + dt / 4) * 1000) {
                    val mx = movement!!.point.x; val my = movement!!.point.y
                    when (movement!!.touchType) {
                        TouchType.DOWN -> { cursors[i]!!.mouseDown = true; for (j in replay!!.cursorIndex.indices) cursors[j]!!.mouseOldDown = false; cursors[i]!!.mousePos.x = mx; cursors[i]!!.mousePos.y = my; replay!!.lastMoveIndex[i] = -1 }
                        TouchType.MOVE -> { cursors[i]!!.mousePos.x = mx; cursors[i]!!.mousePos.y = my; replay!!.lastMoveIndex[i] = cIndex }
                        else -> cursors[i]!!.mouseDown = false
                    }
                    replay!!.cursorIndex[i]++; cIndex++
                }
                if (movement != null && movement!!.touchType == TouchType.MOVE && replay!!.lastMoveIndex[i] >= 0) {
                    val lIndex = replay!!.lastMoveIndex[i]
                    val lastMovement = replay!!.cursorMoves[i].movements[lIndex]
                    val t = (secPassed * 1000 - movement!!.time) / (lastMovement!!.time - movement!!.time)
                    cursors[i]!!.mousePos.x = lastMovement.point.x * t + movement!!.point.x * (1 - t)
                    cursors[i]!!.mousePos.y = lastMovement.point.y * t + movement!!.point.y * (1 - t)
                }
            }
        }

        if (isSpectating && cursorSprites != null) {
            // Spectating: show spectated cursor positions from remote data
            for (i in 0 until minOf(getCursorCount(), 3)) {
                cursorSprites!![i].update(dt)
                cursorSprites!![i].setPosition(cursors[i]!!.mousePos.x, cursors[i]!!.mousePos.y)
                cursorSprites!![i].setShowing(cursors[i]!!.mouseDown)
                if (cursors[i]!!.mouseDown && cursors[i]!!.mousePressed) cursorSprites!![i].click()
            }
        } else if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) autoCursor?.update(dt)
        else if (cursorSprites != null) {
            for (i in 0 until getCursorCount()) {
                cursorSprites!![i].update(dt)
                if (replaying) { cursorSprites!![i].setPosition(cursors[i]!!.mousePos.x, cursors[i]!!.mousePos.y); cursorSprites!![i].setShowing(cursors[i]!!.mouseDown) }
                else if (GameHelper.isRelaxMod() && i == 0) { cursorSprites!![i].setPosition(cursors[i]!!.mousePos.x, cursors[i]!!.mousePos.y); cursorSprites!![i].setShowing(true) }
                if (cursors[i]!!.mouseDown && cursors[i]!!.mousePressed) cursorSprites!![i].click()
            }
        }

        for (c in cursors) {
            if (c!!.mouseDown && !c.mouseOldDown) { c.mousePressed = true; c.mouseOldDown = true } else c.mousePressed = false
        }
        pressConsumedThisFrame.fill(false)

        if (GameHelper.isFlashLight() && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()) {
            if (mainCursorId < 0) { var i = 0; for (c in cursors) { if (c!!.mousePressed) { mainCursorId = i; flashlightSprite!!.onMouseMove(c.mousePos.x, c.mousePos.y); break }; ++i } }
            else if (!cursors[mainCursorId]!!.mouseDown) mainCursorId = -1
            else if (cursors[mainCursorId]!!.mouseDown) flashlightSprite!!.onMouseMove(cursors[mainCursorId]!!.mousePos.x, cursors[mainCursorId]!!.mousePos.y)
            flashlightSprite!!.onUpdate(stat!!.getCombo())
        }

        while (timingPoints!!.isNotEmpty() && timingPoints!!.peek().time <= secPassed + approachRate) { currentTimingPoint = timingPoints!!.poll(); activeTimingPoints!!.add(currentTimingPoint) }
        while (activeTimingPoints!!.isNotEmpty() && activeTimingPoints!!.peek().time <= secPassed) {
            soundTimingPoint = activeTimingPoints!!.poll()
            if (!soundTimingPoint!!.inherited) { GameHelper.setBeatLength(soundTimingPoint!!.beatLength); GameHelper.setTimingOffset(soundTimingPoint!!.time) }
            GameHelper.timeSignature = soundTimingPoint!!.signature; GameHelper.isKiai = soundTimingPoint!!.isKiai()
        }

        if (breakPeriods.isNotEmpty() && !breakAnimator!!.isBreak() && breakPeriods.peek().start <= secPassed) {
            gameStarted = false; breakAnimator!!.init(breakPeriods.peek().length)
            if (GameHelper.isFlashLight()) flashlightSprite!!.onBreak(true)
            if (Multiplayer.isConnected) RoomScene.chat.show()
            scorebar?.setVisible(false); breakPeriods.poll()
        }
        if (breakAnimator!!.isOver()) { RoomScene.chat.dismiss(); gameStarted = true; scorebar?.setVisible(true); if (GameHelper.isFlashLight()) flashlightSprite!!.onBreak(false) }
        if (objects.isNullOrEmpty() && activeObjects.isEmpty()) if (GameHelper.isFlashLight()) flashlightSprite!!.onBreak(true)

        if (gameStarted) {
            var rate = 0.375
            if (drain > 0 && distToNextObject > 0) rate = 1 + drain / (2 * distToNextObject)
            stat!!.changeHp(-rate.toFloat() * 0.01f * dt)
            if (stat!!.hp <= 0 && stat!!.canFail) {
                if (GameHelper.isEasy() && failcount < 3) { failcount++; stat!!.changeHp(1f) }
                else {
                    if (Multiplayer.isMultiplayer) { if (!hasFailed) ToastLogger.showText("You failed but you can continue playing.", false); hasFailed = true }
                    else { gameover(); return }
                }
            }
        }

        hitErrorMeter?.update(dt)

        if (!Config.isHideInGameUI()) {
            strBuilder.setLength(0); strBuilder.append(stat!!.getCombo())
            while (strBuilder.length < 5) strBuilder.append('*')
            val comboStr = strBuilder.toString()
            if (Config.isComplexAnimations()) scoreShadow?.changeText(comboStr) else comboText?.changeText(comboStr)
            strBuilder.setLength(0)
            var rawAccuracy = stat!!.getAccuracy() * 100f
            strBuilder.append(rawAccuracy.toInt()); if (rawAccuracy.toInt() < 10) strBuilder.insert(0, '0')
            strBuilder.append('.'); rawAccuracy -= rawAccuracy.toInt().toFloat(); rawAccuracy *= 100
            if (rawAccuracy.toInt() < 10) strBuilder.append('0'); strBuilder.append(rawAccuracy.toInt())
            if (strBuilder.length < 6) strBuilder.insert(0, '*')
            accText?.changeText(strBuilder.toString())
            strBuilder.setLength(0); strBuilder.append(stat!!.totalScoreWithMultiplier)
            while (strBuilder.length < 8) strBuilder.insert(0, '0')
            var scoreTextOffset = 0
            while (strBuilder.length < 10) { strBuilder.insert(0, '*'); scoreTextOffset++ }
            scoreText!!.setPosition(Config.getRES_WIDTH() - scoreText!!.digitWidth * (9.25f - scoreTextOffset), 0f)
            scoreText!!.changeText(strBuilder.toString())
        }

        if (comboBurst != null) { if (stat!!.getCombo() == 0) comboBurst!!.breakCombo() else comboBurst!!.checkAndShow(stat!!.getCombo()) }

        while (expiredObjects.isNotEmpty()) { val obj = expiredObjects.poll()!!; obj.cleanupFromScene(); activeObjects.remove(obj); passiveObjects.remove(obj) }
        updatePassiveObjects(dt); updateLastActiveObjectHitTime(); updateActiveObjects(dt)

        if (!isSpectating && (GameHelper.isAuto() || GameHelper.isAutopilotMod()) && activeObjects.isNotEmpty()) autoCursor?.moveToObject(activeObjects[0], secPassed, this)

        if (!isSpectating) {
            var downPressCursorCount = 0
            for (i in 0 until getCursorCount()) { if (cursorIIsDown[i]) downPressCursorCount++; cursorIIsDown[i] = false }
            for (i in 0 until downPressCursorCount - 1) { updateLastActiveObjectHitTime(); tryHitActiveObjects(dt) }
        }

        if (video != null && secPassed >= videoOffset) {
            if (!videoStarted) { video!!.texture.play(); video!!.texture.setPlaybackSpeed(timeMultiplier); videoStarted = true }
            if (video!!.alpha < 1.0f) video!!.setAlpha(minOf(video!!.alpha + 0.03f, 1.0f))
        }

        if (secPassed >= 0 && !musicStarted) {
            GlobalManager.getInstance().songService!!.play(); GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume())
            totalLength = GlobalManager.getInstance().songService!!.length; musicStarted = true; musicReady = false; secPassed = 0f
            if (replaying || GameHelper.isAuto()) showReplayOverlay()
            return
        }

        var shouldBePunished = false
        while (objects!!.isNotEmpty() && secPassed + approachRate > objects!!.peek().getTime()) {
            gameStarted = true
            val data = objects!!.poll()!!
            val params = data.rawdata; val pos = data.pos; val objDefine = data.comboCode; val time = data.getRawTime()
            if (time > totalLength) shouldBePunished = true
            val nextObj = objects!!.peek()
            if (objDefine and 2 <= 0) { pos.x += data.posOffset; pos.y += data.posOffset }
            if (objects!!.isNotEmpty()) { distToNextObject = (nextObj.getTime() - data.getTime()).toDouble(); if (soundTimingPoint != null && distToNextObject < soundTimingPoint!!.beatLength / 2) distToNextObject = soundTimingPoint!!.beatLength / 2 } else distToNextObject = 0.0
            var comboCode = objDefine
            if (comboCode == 12) currentComboNum = 0
            else if (comboNum == -1) { comboNum = 1; currentComboNum = 0 }
            else if (comboCode and 4 > 0) { currentComboNum = 0; if (comboCode / 15 > 0) { comboCode /= 15; for (i in 0..Int.MAX_VALUE) { if (comboCode shr i == 1) { comboNum = i; break } } } else comboNum = (comboNum + 1) % combos!!.size }

            if (objDefine and 1 > 0) {
                val col = getComboColor(comboNum); val circle = GameObjectPool.getInstance().getCircle()
                var tempSound: String? = null; if (params.size > 5) tempSound = params[5]
                circle.init(this, mgScene, pos, data.getTime() - secPassed, col.r(), col.g(), col.b(), scale, currentComboNum, data.sampleSet, tempSound, isFirst)
                circle.setEndsCombo(objects!!.isEmpty() || nextObj.isNewCombo())
                addObject(circle); isFirst = false
                if (objects!!.isNotEmpty() && !nextObj.isNewCombo()) { val track = GameObjectPool.getInstance().getTrack(); val end = if (nextObj.getTime() > data.getTime()) data.getEnd() else data.pos; track.init(this, bgScene, end, nextObj.pos, nextObj.getTime() - secPassed, approachRate, scale) }
                if (!isSpectating && GameHelper.isAuto()) circle.setAutoPlay()
                circle.hitTime = data.getTime()
                if (objects!!.isNotEmpty() && nextObj.getTime() > data.getTime()) currentComboNum++
                circle.setId(++lastObjectId)
                if (replaying) circle.setReplayData(replay!!.objectData!![circle.getId()])
            } else if (objDefine and 8 > 0) {
                val endTime = params[5].toInt() / 1000.0f; val rps = 2 + 2 * overallDifficulty / 10f
                val spinner = GameObjectPool.getInstance().getSpinner()
                var tempSound: String? = null; if (params.size > 6) tempSound = params[6]
                spinner.init(this, mgScene, (data.getTime() - secPassed) / timeMultiplier, (endTime - data.getTime()) / timeMultiplier, rps, data.sampleSet, tempSound, stat!!)
                spinner.setEndsCombo(objects!!.isEmpty() || nextObj.isNewCombo()); addObject(spinner); isFirst = false
                if (!isSpectating && (GameHelper.isAuto() || GameHelper.isAutopilotMod())) spinner.setAutoPlay()
                spinner.setId(++lastObjectId)
                if (replaying) spinner.setReplayData(replay!!.objectData!![spinner.getId()])
            } else if (objDefine and 2 > 0) {
                val col = getComboColor(comboNum); val soundspec = if (params.size > 8) params[8] else null
                val slider = GameObjectPool.getInstance().getSlider()
                var tempSound: String? = null; if (params.size > 9) tempSound = params[9]
                val sliderPath = getSliderPath(sliderIndex)
                slider.init(this, mgScene, pos, data.posOffset, data.getTime() - secPassed, col.r(), col.g(), col.b(), scale, currentComboNum, data.sampleSet, data.customSound, data.timingShift.toFloat(), params[5], currentTimingPoint, soundspec, tempSound, isFirst, data.getRawTime().toDouble(), sliderPath)
                sliderIndex++; slider.setEndsCombo(objects!!.isEmpty() || nextObj.isNewCombo()); addObject(slider); isFirst = false
                if (objects!!.isNotEmpty() && !nextObj.isNewCombo()) { val track = GameObjectPool.getInstance().getTrack(); val end = if (nextObj.getTime() > data.getTime()) data.getEnd() else data.pos; track.init(this, bgScene, end, nextObj.pos, nextObj.getTime() - secPassed, approachRate, scale) }
                if (GameHelper.isAuto()) slider.setAutoPlay()
                slider.hitTime = data.getTime()
                if (objects!!.isNotEmpty() && nextObj.getTime() > data.getTime()) currentComboNum++
                slider.setId(++lastObjectId)
                if (replaying) { slider.setReplayData(replay!!.objectData!![slider.getId()]); slider.getReplayData()?.let { if (it.tickSet == null) it.tickSet = BitSet() } }
            }
        }

        metronome?.update(secPassed)
        val playerStatus = GlobalManager.getInstance().songService!!.status
        if (playerStatus != Status.PLAYING) secPassed += dt

        if (shouldBePunished || (objects!!.isEmpty() && activeObjects.isEmpty() && leadOut > 2)) {
            dismissReplayOverlay(); scene = Scene(); SkinManager.setSkinEnabled(false)
            GameObjectPool.getInstance().purge(); SpritePool.getInstance().purge(); GameHelper.clearPools()
            passiveObjects.clear(); breakPeriods.clear(); cursorSprites = null

            // Spectate: notify spectators that game ended
            if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
                Execution.asyncIgnoreExceptions {
                    SpectateAPI.sendSpectateEnd(OnlineManager.getInstance().userId)
                }
            }

            // Spectate: cleanup when spectating ends
            if (isSpectating) {
                // Don't clean up listeners or exit — stay alive waiting for next state event
                // SpectateScene handles restart via stateListener
                return
            }

            var rFile: String? = null; stat!!.setTime(System.currentTimeMillis())
            if (replay != null && !replaying) {
                val ctime = System.currentTimeMillis().toString()
                rFile = Config.getCorePath() + "Scores/" + MD5Calculator.getStringMD5(lastTrack!!.filename + ctime) + ctime.substring(0, minOf(3, ctime.length)) + ".odr"
                replay!!.stat = stat; replay!!.save(rFile)
            }

            // Spectate: notify spectators that map finished
            if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
                Execution.asyncIgnoreExceptions {
                    SpectateAPI.sendMapFinish(
                        OnlineManager.getInstance().userId,
                        stat?.totalScoreWithMultiplier ?: 0,
                        stat?.getCombo() ?: 0,
                        stat?.getAccuracy() ?: 0f,
                        ""
                    )
                }
            }

            if (GlobalManager.getInstance().camera is SmoothCamera) { val camera = GlobalManager.getInstance().camera as SmoothCamera; camera.setZoomFactorDirect(1f); if (Config.isShrinkPlayfieldDownwards()) camera.setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f) }
            if (scoringScene != null) {
                if (replaying) { ModMenu.getInstance().setMod(Replay.oldMod); ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed); ModMenu.getInstance().FLfollowDelay = Replay.oldFLFollowDelay; ModMenu.getInstance().setCustomAR(Replay.oldCustomAR); ModMenu.getInstance().setCustomOD(Replay.oldCustomOD); ModMenu.getInstance().setCustomCS(Replay.oldCustomCS); ModMenu.getInstance().setCustomHP(Replay.oldCustomHP) }
                if (replaying) scoringScene!!.load(scoringScene!!.getReplayStat()!!, null, GlobalManager.getInstance().songService, rFile, null, lastTrack)
                else { val s = stat!!; if (s.mod.contains(GameMod.MOD_AUTO)) s.playerName = "osu!"; EdExtensionHelper.onEndGame(lastTrack, stat); if (Multiplayer.isConnected) { Multiplayer.log("Match ended, moving to results scene."); RoomScene.chat.show();                 Execution.asyncIgnoreExceptions { RoomAPI.submitFinalScore(s.toJson()) }; ToastLogger.showText("Loading room statistics...", false) }; scoringScene!!.load(s, lastTrack, GlobalManager.getInstance().songService, rFile, trackMD5, null) }
                GlobalManager.getInstance().songService!!.setVolume(0.2f); DiscordRPC.updateForResults(); engine.setScene(scoringScene!!.scene)
            } else GlobalManager.getInstance().songMenu?.show()
            val touchOptions2 = TouchOptions(); touchOptions2.setRunOnUpdateThread(true); engine.touchController.applyTouchOptions(touchOptions2)
            video?.let { it.release(); video = null; videoStarted = false }
        } else if (objects!!.isEmpty() && activeObjects.isEmpty()) { gameStarted = false; leadOut += dt }

        if (secPassed > skipTime - 1f && skipBtn != null) { RoomScene.chat.dismiss(); skipBtn!!.detachSelf(); skipBtn = null }
        else if (skipBtn != null) {
            skipMaxPos.set(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
            for (c in cursors) { if (c!!.mouseDown && Utils.distance(c.mousePos, skipMaxPos) < 250) {
                if (Multiplayer.isConnected) {                 if (!isSkipRequested) { isSkipRequested = true; ResourceManager.getInstance().getSound("menuhit").play(); skipBtn!!.isVisible = false; Execution.async { RoomAPI.requestSkip() }; ToastLogger.showText("Skip requested", false) }; return }
                skipBtn?.detachSelf(); skipBtn = null; skip(); return
            } }
        }
    }

    private fun updateLastActiveObjectHitTime() { for (obj in activeObjects) { if (!obj.isStartHit()) { lastActiveObjectHitTime = obj.hitTime; break } } }
    private fun tryHitActiveObjects(deltaTime: Float) { for (obj in activeObjects) obj.tryHit(deltaTime) }
    private fun updateActiveObjects(deltaTime: Float) { for (obj in activeObjects) obj.update(deltaTime) }
    private fun updatePassiveObjects(deltaTime: Float) { for (obj in passiveObjects) obj.update(deltaTime) }

    fun skip() {
        RoomScene.chat.dismiss()
        if (secPassed > skipTime - 1f) return
        if (GlobalManager.getInstance().songService!!.status != Status.PLAYING) {
            GlobalManager.getInstance().songService!!.play(); GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume())
            totalLength = GlobalManager.getInstance().songService!!.length; musicStarted = true; musicReady = false
        }
        ResourceManager.getInstance().getSound("menuhit").play()
        val difference = skipTime - 0.5f - secPassed; secPassed = skipTime - 0.5f
        val seekTime = Math.ceil(secPassed * 1000.0).toInt(); val videoSeekTime = seekTime - (videoOffset * 1000).toInt()

        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
            Execution.asyncIgnoreExceptions {
                SpectateAPI.sendSpectateSkip(OnlineManager.getInstance().userId, seekTime)
            }
        }

        Execution.updateThread {
            updatePassiveObjects(difference)
            GlobalManager.getInstance().songService!!.seekTo(seekTime)
            video?.texture?.seekTo(videoSeekTime)
            skipBtn?.detachSelf(); skipBtn = null
        }
    }

    private fun onExit() {
        dismissReplayOverlay()
        if (!replaying) EdExtensionHelper.onExitGame(lastTrack)
        SkinManager.setSkinEnabled(false); GameObjectPool.getInstance().purge(); SpritePool.getInstance().purge(); GameHelper.clearPools()
        passiveObjects.clear(); breakPeriods.clear(); cursorSprites = null; scoreBoard = null
        GlobalManager.getInstance().songService?.let { it.stop(); it.preLoadPreview(filePath!!); it.play(); it.setVolume(Config.getBgmVolume()) }
        if (replaying) { replayFile = null; ModMenu.getInstance().setMod(Replay.oldMod); ModMenu.getInstance().setChangeSpeed(Replay.oldChangeSpeed); ModMenu.getInstance().FLfollowDelay = Replay.oldFLFollowDelay; ModMenu.getInstance().setCustomAR(Replay.oldCustomAR); ModMenu.getInstance().setCustomOD(Replay.oldCustomOD); ModMenu.getInstance().setCustomCS(Replay.oldCustomCS); ModMenu.getInstance().setCustomHP(Replay.oldCustomHP) }
    }

    fun quit() {
        dismissReplayOverlay(); org.anddev.andengine.opengl.texture.TextureManager.setSuppressGC(false)
        val touchOptions = TouchOptions(); touchOptions.setRunOnUpdateThread(true); engine.touchController.applyTouchOptions(touchOptions)
        if (!replaying) EdExtensionHelper.onQuitGame(lastTrack)
        if (storyboardSprite != null) { storyboardSprite!!.detachSelf(); storyboardOverlayProxy!!.detachSelf(); storyboardSprite!!.releaseStoryboard(); storyboardOverlayProxy!!.drawProxy = null; storyboardSprite = null }
        video?.let { it.release(); video = null; videoStarted = false }
        onExit()

        if (!replaying && !isSpectating) {
            com.reco1l.framework.lang.Execution.asyncIgnoreExceptions {
                SocialAPI.updateStatus(OnlineManager.getInstance().userId, "online")
            }
        }
        // Clean up spectate listeners
        if (isSpectating) {
            SpectateAPI.stopSpectating(spectateTargetId)
            spectateDataListener?.let { com.reco1l.api.server.ServerAPI.off("spectate_data", it) }
            spectateStateListener?.let { com.reco1l.api.server.ServerAPI.off("spectate_state", it) }
            spectateDataListener = null
            spectateStateListener = null
            isSpectating = false
            spectateBuffer.clear()
        }
        if (GlobalManager.getInstance().camera is SmoothCamera) { val camera = GlobalManager.getInstance().camera as SmoothCamera; camera.setZoomFactorDirect(1f); if (Config.isShrinkPlayfieldDownwards()) camera.setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f) }
        scene = Scene()
        if (Multiplayer.isMultiplayer) { RoomScene.show(); return }
        ResourceManager.getInstance().getSound("failsound").stop(); GlobalManager.getInstance().songMenu?.show()
    }

    override fun reset() {}

    private fun registerHit(objectId: Int, score: Int, endCombo: Boolean): String {
        val writeReplay = objectId != -1 && replay != null && !replaying

        // Spectate: track hit results for streaming
        if (!replaying && !isSpectating && objectId != -1 && OnlineManager.getInstance().stayOnline) {
            synchronized(spectateHitsSinceLastSend) {
                spectateHitsSinceLastSend.add(Pair(objectId, score))
            }
        }

        if (score == 0) {
            if (stat!!.getCombo() > 30) ResourceManager.getInstance().getCustomSound("combobreak", 1)?.play()
            comboWasMissed = true; stat!!.registerHit(0, false, false)
            if (writeReplay) replay!!.addObjectScore(objectId, ResultType.MISS)
            if (GameHelper.isPerfect()) { gameover(); if (!Multiplayer.isMultiplayer) restartGame() }
            if (GameHelper.isSuddenDeath()) { stat!!.changeHp(-1.0f); gameover() }
            if (objectId != -1) updatePPCounter(objectId)
            return "hit0"
        }
        var scoreName = "hit300"
        if (score == 50) { stat!!.registerHit(50, false, false); if (writeReplay) replay!!.addObjectScore(objectId, ResultType.HIT50); scoreName = "hit50"; comboWas100 = true; if (GameHelper.isPerfect()) { gameover(); if (!Multiplayer.isMultiplayer) restartGame() } }
        else if (score == 100) { comboWas100 = true; if (writeReplay) replay!!.addObjectScore(objectId, ResultType.HIT100); if (endCombo && !comboWasMissed) { stat!!.registerHit(100, true, false); scoreName = "hit100k" } else { stat!!.registerHit(100, false, false); scoreName = "hit100" }; if (GameHelper.isPerfect()) { gameover(); if (!Multiplayer.isMultiplayer) restartGame() } }
        else if (score == 300) { if (writeReplay) replay!!.addObjectScore(objectId, ResultType.HIT300); if (endCombo && !comboWasMissed) { if (!comboWas100) { stat!!.registerHit(300, true, true); scoreName = "hit300g" } else { stat!!.registerHit(300, true, false); scoreName = "hit300k" } } else { stat!!.registerHit(300, false, false); scoreName = "hit300" } }
        if (endCombo) { comboWas100 = false; comboWasMissed = false }
        if (objectId != -1) updatePPCounter(objectId)
        return scoreName
    }

    override fun onCircleHit(id: Int, acc: Float, pos: PointF, endCombo: Boolean, forcedScore: Byte, color: RGBColor) {
        if (GameHelper.isAuto()) autoCursor?.click()
        val accuracy = Math.abs(acc.toFloat()).toDouble()
        val writeReplay = replay != null && !replaying
        if (writeReplay) { val sacc = (acc * 1000).toInt().toShort(); replay!!.addObjectResult(id, sacc, BitSet()) }
        if (GameHelper.isFlashLight() && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()) { val nId = getNearestCursorId(pos.x, pos.y); if (nId >= 0) { mainCursorId = nId; flashlightSprite!!.onMouseMove(cursors[mainCursorId]!!.mousePos.x, cursors[mainCursorId]!!.mousePos.y) } }
        if (accuracy > difficultyHelper.hitWindowFor50(overallDifficulty) || forcedScore == ResultType.MISS.id) { createHitEffect(pos, "hit0", color); registerHit(id, 0, endCombo); return }
        val scoreName: String = when {
            forcedScore == ResultType.HIT300.id || forcedScore == 0.toByte() && accuracy <= difficultyHelper.hitWindowFor300(overallDifficulty) -> registerHit(id, 300, endCombo)
            forcedScore == ResultType.HIT100.id || forcedScore == 0.toByte() && accuracy <= difficultyHelper.hitWindowFor100(overallDifficulty) -> registerHit(id, 100, endCombo)
            else -> registerHit(id, 50, endCombo)
        }
        createBurstEffect(pos, color); createHitEffect(pos, scoreName, color)
    }

    override fun onSliderReverse(pos: PointF, ang: Float, color: RGBColor) { createBurstEffectSliderReverse(pos, ang, color) }

    override fun onSliderHit(id: Int, score: Int, start: PointF?, end: PointF?, endCombo: Boolean, color: RGBColor, type: Int) {
        if (GameHelper.isFlashLight() && !GameHelper.isAuto() && !GameHelper.isAutopilotMod()) { val nId = getNearestCursorId(end!!.x, end.y); if (nId >= 0) { mainCursorId = nId; flashlightSprite!!.onMouseMove(cursors[mainCursorId]!!.mousePos.x, cursors[mainCursorId]!!.mousePos.y) } }
        if (score == 0) { createHitEffect(start!!, "hit0", color); createHitEffect(end!!, "hit0", color); registerHit(id, 0, endCombo); return }
        if (score == -1) {
            if (stat!!.getCombo() > 30) ResourceManager.getInstance().getCustomSound("combobreak", 1)?.play()
            if (GameHelper.isSuddenDeath()) { stat!!.changeHp(-1.0f); gameover() }
            stat!!.registerHit(0, true, false)
            if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
                synchronized(spectateHitsSinceLastSend) { spectateHitsSinceLastSend.add(Pair(id, -1)) }
            }
            return
        }

        // Always send slider hits to spectators (ticks, ends, everything)
        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
            synchronized(spectateHitsSinceLastSend) { spectateHitsSinceLastSend.add(Pair(id, score)) }
        }

        var scoreName = "hit0"
        when (score) {
            300 -> scoreName = registerHit(id, 300, endCombo)
            100 -> { scoreName = registerHit(id, 100, endCombo); stat!!.setPerfect(false) }
            50 -> { scoreName = registerHit(id, 50, endCombo); stat!!.setPerfect(false) }
            30 -> { scoreName = "sliderpoint30"; stat!!.registerHit(30, false, false) }
            10 -> { scoreName = "sliderpoint10"; stat!!.registerHit(10, false, false) }
        }
        if (score > 10) when (type) { GameObjectListener.SLIDER_START -> createBurstEffectSliderStart(end!!, color); GameObjectListener.SLIDER_END -> createBurstEffectSliderEnd(end!!, color); GameObjectListener.SLIDER_REPEAT -> {}; else -> createBurstEffect(end!!, color) }
        createHitEffect(end!!, scoreName, color)
    }

    override fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int) {
        if (score == 1000) {
            stat!!.registerHit(score, false, false)
            if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
                synchronized(spectateHitsSinceLastSend) { spectateHitsSinceLastSend.add(Pair(id, 1000)) }
            }
            return
        }
        if (replay != null && !replaying) { var acc = (totalScore * 4).toInt().toShort(); when (score) { 300 -> acc = (acc.toInt() + 3).toShort(); 100 -> acc = (acc.toInt() + 2).toShort(); 50 -> acc = (acc.toInt() + 1).toShort() }; replay!!.addObjectResult(id, acc, BitSet()) }
        spinnerCenter.set(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        val pos = spinnerCenter
        if (score == 0) { val effect = GameObjectPool.getInstance().getEffect("hit0"); effect.init(scene, pos, scale, SequenceEntityModifier(ModifierFactory.newFadeInModifier(0.15f), ModifierFactory.newDelayModifier(0.35f), ModifierFactory.newFadeOutModifier(0.25f))); registerHit(id, 0, endCombo); return }
        val scoreName = when (score) { 300 -> registerHit(id, 300, endCombo); 100 -> registerHit(id, 100, endCombo); 50 -> registerHit(id, 50, endCombo); else -> "hit0" }
        if (Config.isHitLighting() && ResourceManager.getInstance().getTexture("lighting") != null) { val light = GameObjectPool.getInstance().getEffect("lighting"); light.init(mgScene, pos, scale, FadeOutModifier(0.7f), SequenceEntityModifier(ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newScaleModifier(0.45f, scale * 1.5f, 2f * scale))) }
        val effect2 = GameObjectPool.getInstance().getEffect(scoreName); effect2.init(mgScene, pos, scale, SequenceEntityModifier(ModifierFactory.newScaleModifier(0.15f, scale, 1.2f * scale), ModifierFactory.newScaleModifier(0.05f, 1.2f * scale, scale), ModifierFactory.newAlphaModifier(1f, 1f, 0f)))
        pos.y /= 2f
        val effect3 = GameObjectPool.getInstance().getEffect("spinner-osu"); effect3.init(mgScene, pos, 1f, ModifierFactory.newFadeOutModifier(1.5f))
    }

    override fun playSound(name: String, sampleSet: Int, addition: Int) {
        if (addition > 0 && name != "hitnormal" && addition < Constants.SAMPLE_PREFIX.size) { playSound(Constants.SAMPLE_PREFIX[addition], name); return }
        if (sampleSet > 0 && sampleSet < Constants.SAMPLE_PREFIX.size) playSound(Constants.SAMPLE_PREFIX[sampleSet], name) else playSound(soundTimingPoint!!.hitSound!!, name)
    }

    fun playSound(prefix: String, name: String) {
        val fullName = soundNameBuilder.append(prefix).append('-').append(name).toString(); soundNameBuilder.setLength(0)
        val snd: BassSoundProvider? = if (soundTimingPoint!!.customSound == 0) ResourceManager.getInstance().getSound(fullName) else ResourceManager.getInstance().getCustomSound(fullName, soundTimingPoint!!.customSound)
        snd ?: return
        if (name == "sliderslide" || name == "sliderwhistle") snd.setLooping(true)
        when (name) { "hitnormal" -> { snd.play(soundTimingPoint!!.volume * 0.8f); return }; "hitwhistle", "hitclap" -> { snd.play(soundTimingPoint!!.volume * 0.85f); return } }
        snd.play(soundTimingPoint!!.volume)
    }

    override fun addObject(obj: GameObject) { activeObjects.add(obj) }
    override fun getMousePos(index: Int): PointF = cursors[index]!!.mousePos
    override fun isMouseDown(index: Int): Boolean = cursors[index]!!.mouseDown

    override fun isMousePressed(obj: GameObject, index: Int): Boolean {
        if (GameHelper.isAuto()) return false
        if (activeObjects.isEmpty()) return false
        val frontmostHitTime = lastActiveObjectHitTime
        if (Math.abs(obj.hitTime - frontmostHitTime) > 0.001f && secPassed < frontmostHitTime) return false
        if (!cursors[index]!!.mousePressed || pressConsumedThisFrame[index]) return false
        pressConsumedThisFrame[index] = true
        return true
    }

    override fun downFrameOffset(index: Int): Double = cursors[index]!!.mouseDownOffsetMS.toDouble()
    override fun removeObject(obj: GameObject) { expiredObjects.add(obj) }

    override fun registerAccuracy(acc: Double) { hitErrorMeter?.putErrorResult(acc.toFloat()); avgOffset += acc.toFloat(); offsetRegs++; stat!!.addHitOffset(acc); if (replaying) scoringScene!!.getReplayStat()?.addHitOffset(acc) }
    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet) { onTrackingSliders(false); if (GameHelper.isAuto()) autoCursor?.onSliderEnd(); if (replay != null && !replaying) replay!!.addObjectResult(id, accuracy.toShort(), tickSet.clone() as BitSet) }
    override fun onTrackingSliders(isTrackingSliders: Boolean) { if (GameHelper.isAuto()) autoCursor?.onSliderTracking(); if (GameHelper.isFlashLight()) flashlightSprite!!.onTrackingSliders(isTrackingSliders) }
    override fun onUpdatedAutoCursor(pX: Float, pY: Float) { if (GameHelper.isFlashLight()) flashlightSprite!!.onMouseMove(pX, pY) }
    override fun updateAutoBasedPos(pX: Float, pY: Float) { if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) autoCursor?.setPosition(pX, pY, this) }
    override fun getCursorsCount(): Int = getCursorCount()
    override fun addPassiveObject(obj: GameObject) { passiveObjects.add(obj) }
    override fun removePassiveObject(obj: GameObject) { expiredObjects.add(obj) }

    private fun applyCursorTrackCoordinates(cursor: Cursor): PointF {
        var rawX = cursor.mousePos.x; var rawY = cursor.mousePos.y
        val width = Config.getRES_WIDTH().toFloat(); val height = Config.getRES_HEIGHT().toFloat()
        if (GameHelper.isHardrock()) { rawY -= height / 2f; rawY *= -1; rawY += height / 2f }
        rawY -= (height - Constants.MAP_ACTUAL_HEIGHT) / 2f; rawX -= (width - Constants.MAP_ACTUAL_WIDTH) / 2f
        rawX *= Constants.MAP_WIDTH.toFloat() / Constants.MAP_ACTUAL_WIDTH; rawY *= Constants.MAP_HEIGHT.toFloat() / Constants.MAP_ACTUAL_HEIGHT
        cursor.trackPos.x = rawX; cursor.trackPos.y = rawY
        return cursor.trackPos
    }

    override fun onSceneTouchEvent(pScene: Scene, event: TouchEvent): Boolean {
        if (replaying || isSpectating) return false
        val id = event.pointerID; if (id < 0 || id >= getCursorCount()) return false
        val cursor = cursors[id]!!
        val sprite = if (!GameHelper.isAuto() && !GameHelper.isAutopilotMod() && cursorSprites != null) cursorSprites!![id] else null
        val touchX = FMath.clamp(event.x, 0f, Config.getRES_WIDTH().toFloat())
        val touchY = FMath.clamp(event.y, 0f, Config.getRES_HEIGHT().toFloat())
        val frameOffset = if (previousFrameTime > 0) (event.motionEvent.eventTime - previousFrameTime) * timeMultiplier else 0f
        val eventTime = (secPassed * 1000 + frameOffset).toInt()

        if (GameHelper.isRelaxMod() && !paused && !Multiplayer.isMultiplayer) {
            val grabRadius = Utils.toRes(80).toFloat()
            val distToCursor = Utils.squaredDistance(PointF(touchX, touchY), cursor.mousePos)
            when {
                event.isActionDown -> {
                    if (distToCursor <= grabRadius * grabRadius) {
                        relaxCursorHeld = true
                        cursor.mouseDown = true
                        cursor.mouseDownOffsetMS = frameOffset.toDouble()
                        for (v in cursors) v!!.mouseOldDown = false
                        val gamePoint = applyCursorTrackCoordinates(cursor)
                        replay?.addPress(eventTime, gamePoint, id)
                        cursorIIsDown[id] = true
                        sprite?.setShowing(true)
                    }
                }
                event.isActionMove -> {
                    if (relaxCursorHeld) {
                        cursor.mousePos.x = touchX
                        cursor.mousePos.y = touchY
                        sprite?.setPosition(touchX, touchY)
                        sprite?.setShowing(true)
                        val gamePoint = applyCursorTrackCoordinates(cursor)
                        replay?.addMove(eventTime, gamePoint, id)
                    }
                }
                event.isActionUp -> {
                    if (relaxCursorHeld) {
                        relaxCursorHeld = false
                        cursor.mouseDown = false
                        cursorIIsDown[id] = false
                        replay?.addUp(eventTime, id)
                        if (breakAnimator == null || !breakAnimator!!.isBreak()) pause()
                    }
                }
            }
            return true
        }

        cursor.mousePos.x = touchX
        cursor.mousePos.y = touchY
        sprite?.setPosition(cursor.mousePos.x, cursor.mousePos.y)
        when {
            event.isActionDown -> { sprite?.setShowing(true); cursor.mouseDown = true; cursor.mouseDownOffsetMS = frameOffset.toDouble(); for (v in cursors) v!!.mouseOldDown = false; val gamePoint = applyCursorTrackCoordinates(cursor); replay?.addPress(eventTime, gamePoint, id); cursorIIsDown[id] = true }
            event.isActionMove -> { sprite?.setShowing(true); val gamePoint = applyCursorTrackCoordinates(cursor); replay?.addMove(eventTime, gamePoint, id) }
            event.isActionUp -> {
                sprite?.setShowing(false); cursor.mouseDown = false; cursorIIsDown[id] = false; replay?.addUp(eventTime, id)
                if (GameHelper.isRelaxMod() && Multiplayer.isMultiplayer && !hasFailed && breakAnimator != null && !breakAnimator!!.isBreak()) {
                    val now = SystemClock.uptimeMillis()
                    if (now - relaxLiftOffWindowStart > 10000) {
                        relaxLiftOffCount = 0
                        relaxLiftOffWindowStart = now
                    }
                    relaxLiftOffCount++
                    if (relaxLiftOffCount > 3) {
                        hasFailed = true
                        ToastLogger.showText("You failed but you can continue playing.", false)
                    }
                }
            }
            else -> return false
        }
        return true
    }

    fun onKeyboardDown(keyCode: Int): Boolean {
        if (replaying || !KeyboardConfig.isEnabled()) return false
        val preferredSlot = KeyboardConfig.getCursorForKey(keyCode)
        if (preferredSlot < 0 || preferredSlot >= getCursorCount()) return false
        if (kbKeyToSlot.containsKey(keyCode)) return true
        var slot = -1
        for (s in 1 until getCursorCount()) { if (!cursors[s]!!.mouseDown) { slot = s; break } }
        if (slot < 0) return false
        kbKeyToSlot[keyCode] = slot
        val cursor = cursors[slot]!!
        val sprite = if (!GameHelper.isAuto() && !GameHelper.isAutopilotMod() && cursorSprites != null) cursorSprites!![slot] else null
        val aimCursor = cursors[0]!!
        cursor.mousePos.x = aimCursor.mousePos.x; cursor.mousePos.y = aimCursor.mousePos.y
        sprite?.let { it.setPosition(KeyboardConfig.getCursorX(slot), KeyboardConfig.getCursorY(slot)); it.setShowing(true) }
        val frameOffset = if (previousFrameTime > 0) (SystemClock.uptimeMillis() - previousFrameTime) * timeMultiplier else 0f
        val eventTime = (secPassed * 1000 + frameOffset).toInt()
        cursor.mouseDown = true; cursor.mouseDownOffsetMS = frameOffset.toDouble(); cursor.mouseOldDown = false
        val gamePoint = applyCursorTrackCoordinates(cursor); replay?.addPress(eventTime, gamePoint, slot); cursorIIsDown[slot] = true
        return true
    }

    fun onKeyboardUp(keyCode: Int): Boolean {
        if (replaying || !KeyboardConfig.isEnabled()) return false
        val slot = kbKeyToSlot.getOrDefault(keyCode, -1)
        if (slot < 0 || slot >= getCursorCount()) return false
        kbKeyToSlot.remove(keyCode)
        val cursor = cursors[slot]!!
        val sprite = if (!GameHelper.isAuto() && !GameHelper.isAutopilotMod() && cursorSprites != null) cursorSprites!![slot] else null
        sprite?.setShowing(false); cursor.mouseDown = false; cursorIIsDown[slot] = false
        val frameOffset = if (previousFrameTime > 0) (SystemClock.uptimeMillis() - previousFrameTime) * timeMultiplier else 0f
        val eventTime = (secPassed * 1000 + frameOffset).toInt()
        replay?.addUp(eventTime, slot)
        return true
    }

    override fun stopSound(name: String) { val fullName = soundNameBuilder.append(soundTimingPoint!!.hitSound).append('-').append(name).toString(); soundNameBuilder.setLength(0); ResourceManager.getInstance().getSound(fullName)?.stop() }

    fun pause() {
        if (paused) return
        if (isSpectating) return  // Spectator cannot manually pause
        if (System.currentTimeMillis() - lastPauseTime < 10000) return
        if (Multiplayer.isMultiplayer) { if (lastBackPressTime > 0 && realTimeElapsed - lastBackPressTime > 300) { if (Multiplayer.isConnected)                 Execution.asyncIgnoreExceptions { RoomAPI.submitFinalScore(stat!!.toJson()) }; Multiplayer.log("Player left the match."); quit(); return }; lastBackPressTime = realTimeElapsed.toFloat(); ToastLogger.showText("Tap twice to exit to room.", false); return }
        if (!replaying) EdExtensionHelper.onPauseGame(lastTrack)
        video?.texture?.pause()

        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
            Execution.asyncIgnoreExceptions {
                SpectateAPI.sendMapPause(OnlineManager.getInstance().userId, (secPassed * 1000).toInt())
            }
        }

        if (!GameHelper.isAuto() && !GameHelper.isAutopilotMod() && !replaying) {
            val frameOffset = if (previousFrameTime > 0) (SystemClock.uptimeMillis() - previousFrameTime) * timeMultiplier else 0f
            val time = (secPassed * 1000 + frameOffset).toInt()
            for (i in 0 until getCursorCount()) { val cursor = cursors[i]!!; if (cursor.mouseDown) { cursor.mouseDown = false; replay?.addUp(time, i) }; if (!GameHelper.isRelaxMod()) cursorSprites?.get(i)?.setShowing(false) }
        }
        if (GlobalManager.getInstance().songService?.status == Status.PLAYING) GlobalManager.getInstance().songService!!.pause()
        paused = true
        val menu = PauseMenu(engine, this, false); scene.setChildScene(menu.getScene(), false, true, true)
    }

    fun gameover() {
        if (Multiplayer.isMultiplayer) { if (Multiplayer.isConnected) { Multiplayer.log("Player has lost, moving to room scene.");                 Execution.asyncIgnoreExceptions { RoomAPI.submitFinalScore(stat!!.toJson()) } }; quit(); return }
        if (!replaying) EdExtensionHelper.onGameover(lastTrack)
        dismissReplayOverlay(); scorebar?.flush(); ResourceManager.getInstance().getSound("failsound").play()

        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
            Execution.asyncIgnoreExceptions {
                SpectateAPI.sendSpectateDeath(OnlineManager.getInstance().userId)
            }
        }

        val menu = PauseMenu(engine, this, true); gameStarted = false
        video?.texture?.pause()
        if (GlobalManager.getInstance().songService?.status == Status.PLAYING) GlobalManager.getInstance().songService!!.pause()
        paused = true; scene.setChildScene(menu.getScene(), false, true, true)
    }

    fun resume() {
        if (!paused) return
        scene.childScene.back(); paused = false
        lastPauseTime = System.currentTimeMillis()

        if (!replaying && !isSpectating && OnlineManager.getInstance().stayOnline) {
            Execution.asyncIgnoreExceptions {
                SpectateAPI.sendSpectateResume(OnlineManager.getInstance().userId)
            }
        }

        if (stat!!.hp <= 0 && !stat!!.mod.contains(GameMod.MOD_NOFAIL) && !stat!!.mod.contains(GameMod.MOD_RELAX) && !stat!!.mod.contains(GameMod.MOD_AUTOPILOT)) { quit(); return }
        if (!replaying) EdExtensionHelper.onResume(lastTrack)
        if (video != null && videoStarted) video!!.texture.play()
        if (GlobalManager.getInstance().songService?.status != Status.PLAYING && secPassed > 0) { GlobalManager.getInstance().songService!!.play(); GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume()); totalLength = GlobalManager.getInstance().songService!!.length }
        if (GameHelper.isRelaxMod() && !replaying) {
            waitingForCursorHold = true
            relaxCursorHeld = false
            GlobalManager.getInstance().songService?.pause()
        }
    }

    fun isPaused(): Boolean = paused

    /**
     * Called when the host pauses while we're spectating.
     * Pauses music without showing the pause menu.
     */
    fun spectatePause() {
        if (paused) return
        video?.texture?.pause()
        if (GlobalManager.getInstance().songService?.status == Status.PLAYING) {
            GlobalManager.getInstance().songService!!.pause()
        }
        paused = true
    }

    fun spectateResume() {
        if (!paused) return
        if (video != null && videoStarted) video!!.texture.play()
        if (GlobalManager.getInstance().songService?.status != Status.PLAYING && secPassed > 0) {
            GlobalManager.getInstance().songService!!.play()
            GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume())
            totalLength = GlobalManager.getInstance().songService!!.length
        }
        paused = false
    }

    fun registerSpectateListeners() {
        if (spectateDataListener != null) return  // Already registered

        spectateStateListener = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? org.json.JSONObject ?: return@Listener
            spectateBuffer.clear()
        }
        spectateDataListener = io.socket.emitter.Emitter.Listener { args ->
            val data = args.getOrNull(0) as? org.json.JSONObject ?: return@Listener
            try {
                val packet = SpectatePacket.fromJson(data)
                spectateBuffer.addPacket(packet)

                // Apply hits on game thread for safety
                if (packet.hits.isNotEmpty()) {
                    engine.runOnUpdateThread {
                        for (hit in packet.hits) {
                            val obj = activeObjects.firstOrNull { it.getId() == hit.objectId } ?: continue
                            val pos = PointF(obj.getPos().x, obj.getPos().y)
                            val color = getComboColor(currentComboNum)

                            // Mark sliders/spinners as started so they animate
                            if (obj is Slider || obj is Spinner) {
                                obj.setStartHit(true)
                            }

                            when (hit.result) {
                                // Miss
                                0 -> {
                                    if (stat!!.getCombo() > 30) ResourceManager.getInstance().getCustomSound("combobreak", 1)?.play()
                                    stat!!.registerHit(0, false, false)
                                    createHitEffect(pos, "hit0", color)
                                    // Only consume circles, not sliders/spinners
                                    if (obj !is Slider && obj !is Spinner) expiredObjects.add(obj)
                                }
                                // Slider tick small
                                10 -> {
                                    stat!!.registerHit(10, false, false)
                                    createHitEffect(pos, "sliderpoint10", color)
                                }
                                // Slider tick big
                                30 -> {
                                    stat!!.registerHit(30, false, false)
                                    createHitEffect(pos, "sliderpoint30", color)
                                }
                                // 50
                                50 -> {
                                    stat!!.registerHit(50, false, false)
                                    createHitEffect(pos, "hit50", color)
                                    createBurstEffect(pos, color)
                                    if (obj !is Slider && obj !is Spinner) expiredObjects.add(obj)
                                }
                                // 100
                                100 -> {
                                    stat!!.registerHit(100, false, false)
                                    createHitEffect(pos, "hit100", color)
                                    createBurstEffect(pos, color)
                                    if (obj !is Slider && obj !is Spinner) expiredObjects.add(obj)
                                }
                                // 300
                                300 -> {
                                    stat!!.registerHit(300, false, false)
                                    createHitEffect(pos, "hit300", color)
                                    createBurstEffect(pos, color)
                                    if (obj !is Slider && obj !is Spinner) expiredObjects.add(obj)
                                }
                                // Slider break / miss during slider
                                -1 -> {
                                    if (stat!!.getCombo() > 30) ResourceManager.getInstance().getCustomSound("combobreak", 1)?.play()
                                    stat!!.registerHit(0, true, false)
                                }
                                // Spinner bonus
                                1000 -> {
                                    stat!!.registerHit(1000, false, false)
                                    createHitEffect(pos, "hit300", color)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        com.reco1l.api.server.ServerAPI.on("spectate_data", spectateDataListener!!)
        com.reco1l.api.server.ServerAPI.on("spectate_state", spectateStateListener!!)
    }

    fun spectateSkip(hostTimeMs: Int = 0) {
        val targetSec = if (hostTimeMs > 0) hostTimeMs / 1000f else skipTime - 0.5f
        if (GlobalManager.getInstance().songService!!.status != Status.PLAYING) {
            GlobalManager.getInstance().songService!!.play()
            GlobalManager.getInstance().songService!!.setVolume(Config.getBgmVolume())
            totalLength = GlobalManager.getInstance().songService!!.length
            musicStarted = true; musicReady = false
        }
        val difference = targetSec - secPassed; secPassed = targetSec
        val seekTime = Math.ceil(secPassed * 1000.0).toInt()
        val videoSeekTime = seekTime - (videoOffset * 1000).toInt()
        Execution.updateThread {
            updatePassiveObjects(difference)
            GlobalManager.getInstance().songService!!.seekTo(seekTime)
            video?.texture?.seekTo(videoSeekTime)
        }
    }

    /**
     * Stop the game but keep the spectating session alive.
     * Used when the host dies — spectator waits for next state event.
     */
    fun spectateStop() {
        dismissReplayOverlay()
        video?.let { it.release(); video = null; videoStarted = false }

        if (GlobalManager.getInstance().songService?.status == Status.PLAYING) {
            GlobalManager.getInstance().songService!!.pause()
        }
        paused = true
        gameStarted = false

        scene = Scene()
        GameObjectPool.getInstance().purge()
        SpritePool.getInstance().purge()
        GameHelper.clearPools()
        passiveObjects.clear()
        breakPeriods.clear()
        cursorSprites = null
        spectateBuffer.clear()
    }

    private fun createHitEffect(pos: PointF, name: String, color: RGBColor) {
        val effect = GameObjectPool.getInstance().getEffect(name)
        if (name == "hit0") { if (GameHelper.isSuddenDeath()) { effect.init(mgScene, pos, scale * 3, SequenceEntityModifier(ModifierFactory.newFadeInModifier(0.15f), ModifierFactory.newDelayModifier(0.35f), ModifierFactory.newFadeOutModifier(0.25f))); return }; effect.init(mgScene, pos, scale, SequenceEntityModifier(ModifierFactory.newFadeInModifier(0.15f), ModifierFactory.newDelayModifier(0.35f), ModifierFactory.newFadeOutModifier(0.25f))); return }
        if (Config.isHitLighting() && name != "sliderpoint10" && name != "sliderpoint30" && ResourceManager.getInstance().getTexture("lighting") != null) { val light = GameObjectPool.getInstance().getEffect("lighting"); light.setColor(color); light.init(bgScene, pos, scale, ModifierFactory.newFadeOutModifier(1f), SequenceEntityModifier(ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newScaleModifier(0.45f, scale * 1.5f, 1.9f * scale), ModifierFactory.newScaleModifier(0.3f, scale * 1.9f, scale * 2f))); light.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_DST_ALPHA) }
        effect.init(mgScene, pos, scale, SequenceEntityModifier(ModifierFactory.newScaleModifier(0.15f, scale, 1.2f * scale), ModifierFactory.newScaleModifier(0.05f, 1.2f * scale, scale), ModifierFactory.newAlphaModifier(0.5f, 1f, 0f)))
    }

    private fun createBurstEffect(pos: PointF, color: RGBColor) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat!!.mod.contains(GameMod.MOD_HIDDEN)) return
        val b1 = GameObjectPool.getInstance().getEffect("hitcircle"); b1.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f)); b1.setColor(color)
        val b2 = GameObjectPool.getInstance().getEffect("hitcircleoverlay"); b2.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f))
    }

    private fun createBurstEffectSliderStart(pos: PointF, color: RGBColor) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat!!.mod.contains(GameMod.MOD_HIDDEN)) return
        val b1 = GameObjectPool.getInstance().getEffect("sliderstartcircle"); b1.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f)); b1.setColor(color)
        val b2 = GameObjectPool.getInstance().getEffect("sliderstartcircleoverlay"); b2.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f))
    }

    private fun createBurstEffectSliderEnd(pos: PointF, color: RGBColor) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat!!.mod.contains(GameMod.MOD_HIDDEN)) return
        val b1 = GameObjectPool.getInstance().getEffect("sliderendcircle"); b1.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f)); b1.setColor(color)
        val b2 = GameObjectPool.getInstance().getEffect("sliderendcircleoverlay"); b2.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f))
    }

    private fun createBurstEffectSliderReverse(pos: PointF, ang: Float, color: RGBColor) {
        if (!Config.isComplexAnimations() || !Config.isBurstEffects() || stat!!.mod.contains(GameMod.MOD_HIDDEN)) return
        val b1 = GameObjectPool.getInstance().getEffect("reversearrow"); b1.hit.rotation = ang; b1.init(mgScene, pos, scale, ModifierFactory.newScaleModifier(0.25f, scale, 1.5f * scale), ModifierFactory.newAlphaModifier(0.25f, 0.8f, 0f))
    }

    private fun getNearestCursorId(pX: Float, pY: Float): Int {
        var distance = Float.POSITIVE_INFINITY; var id = -1; var i = 0
        for (c in cursors) { if (c!!.mouseDown || c.mousePressed || c.mouseOldDown) { val dx = c.mousePos.x - pX; val dy = c.mousePos.y - pY; val d = dx * dx + dy * dy; if (d < distance) { id = i; distance = d } }; ++i }
        return id
    }

    private fun stackNotes() {
        var i = 0
        for (data in objects!!) { val pos = data.pos; val params = data.rawdata; val objDefine = params[3].toInt()
            if (objects!!.isNotEmpty() && objDefine and 1 > 0 && i + 1 < objects!!.size) {             if (objects!![i + 1].getTime() - data.getTime() < 2f * GameHelper.stackLeniency && Utils.squaredDistance(pos, objects!![i + 1].pos) < scale) objects!![i + 1].setPosOffset(data.posOffset + Utils.toRes(4) * scale) }
            i++ }
    }

    private fun calculateAllSliderPaths() {
        if (objects.isNullOrEmpty()) return
        var sliderCount = 0
        for (data in objects!!) { if (data.rawdata[3].toInt() and 2 > 0) sliderCount++ }
        if (sliderCount <= 0) return
        sliderPaths = arrayOfNulls(sliderCount); sliderIndex = 0
        for (data in objects!!) { val params = data.rawdata; if (params[3].toInt() and 2 > 0) { val pos = data.pos; val length = params[7].toFloat(); val offset = data.posOffset; pos.x += data.posOffset; pos.y += data.posOffset; sliderPaths!![sliderIndex] = if (length < 0) GameHelper.calculatePath(Utils.realToTrackCoords(pos), params[5].split("\\|".toRegex()).toTypedArray(), 0f, offset) else GameHelper.calculatePath(Utils.realToTrackCoords(pos), params[5].split("\\|".toRegex()).toTypedArray(), length, offset); sliderIndex++ } }
        sliderIndex = 0
    }

    private fun getSliderPath(index: Int): SliderPath? = if (sliderPaths != null && index < sliderPaths!!.size && index >= 0) sliderPaths!![index] else null

    fun getReplaying(): Boolean = replaying
    fun replaySeekTo(positionMs: Int) { if (!replaying && !GameHelper.isAuto()) return; pendingReplaySeekMs = positionMs }

    private fun processReplaySeek(positionMs: Int) {
        val targetSec = positionMs / 1000f
        if (targetSec < secPassed) {
            var excludedCount = 0; var excludedSliders = 0
            for (data in allObjects!!) { if (data.getTime() + approachRate <= targetSec) { excludedCount++; if (data.isSlider()) excludedSliders++ } else break }
            lastObjectId = excludedCount - 1; sliderIndex = excludedSliders
            objects!!.clear(); for (data in allObjects!!) { if (data.getTime() + approachRate > targetSec) objects!!.add(data) }
            val iterA = activeObjects.iterator(); while (iterA.hasNext()) { val obj = iterA.next(); obj.cleanupFromScene(); iterA.remove() }
            val iterP = passiveObjects.iterator(); while (iterP.hasNext()) { val obj = iterP.next(); obj.cleanupFromScene(); iterP.remove() }
        } else { while (objects!!.isNotEmpty() && objects!!.peek().getTime() + approachRate <= targetSec) { val data = objects!!.poll()!!; lastObjectId++; if (data.isSlider()) sliderIndex++ } }
        secPassed = targetSec; GlobalManager.getInstance().songService!!.seekTo(positionMs)
        if (video != null) { val videoSeekTime = positionMs - (videoOffset * 1000).toInt(); video!!.texture.seekTo(videoSeekTime) }
        if (replay != null) { for (i in replay!!.cursorIndex.indices) { replay!!.cursorIndex[i] = 0; replay!!.lastMoveIndex[i] = -1
            if (replay!!.cursorMoves.size > i) { var lastMovement: Replay.ReplayMovement? = null
                for (j in 0 until replay!!.cursorMoves[i].size) { val movement = replay!!.cursorMoves[i].movements[j]; if (movement!!.time > positionMs) { replay!!.cursorIndex[i] = j; break }; lastMovement = movement }
                if (lastMovement != null) { cursors[i]!!.mousePos.x = lastMovement.point.x; cursors[i]!!.mousePos.y = lastMovement.point.y; cursors[i]!!.mouseDown = lastMovement.touchType != TouchType.UP }
            } } }
    }

    fun replaySetSpeed(speed: Float) {
        if (!replaying && !GameHelper.isAuto()) return
        val s = maxOf(0.25f, minOf(3.0f, speed)); timeMultiplier = s
        GlobalManager.getInstance().songService?.let { val enableNC = ModMenu.getInstance().isEnableNCWhenSpeedChange || ModMenu.getInstance().mod.contains(GameMod.MOD_NIGHTCORE); it.applySpeed(s, enableNC) }
        if (video != null && videoStarted) video!!.texture.setPlaybackSpeed(s)
        ReplayOverlay.updateSpeed(s)
    }

    fun showReplayOverlay() {
        if (!replaying && !GameHelper.isAuto()) return
        Execution.mainThread {
            val activity = GlobalManager.getInstance().getMainActivity() ?: return@mainThread
            if (activity.isFinishing) return@mainThread
            val fm = activity.supportFragmentManager
            val existing = fm.findFragmentByTag("replay_overlay")
            if (existing != null) fm.beginTransaction().remove(existing).commitAllowingStateLoss()
            replayOverlayFragment = null
            ReplayOverlay.updateTotalLength(totalLength); ReplayOverlay.setOriginalSpeed(timeMultiplier); ReplayOverlay.updateSpeed(timeMultiplier); ReplayOverlay.show()
            replayOverlayFragment = ReplayOverlayFragment(); fm.beginTransaction().add(android.R.id.content, replayOverlayFragment!!, "replay_overlay").commitAllowingStateLoss()
        }
    }

    fun dismissReplayOverlay() { if (replayOverlayFragment != null && replayOverlayFragment!!.isAdded) { replayOverlayFragment!!.dismissOverlay(); replayOverlayFragment = null }; ReplayOverlay.hide() }

    fun saveFailedReplay(): Boolean {
        stat!!.setTime(System.currentTimeMillis())
        if (replay != null && !replaying) {
            for (obj in activeObjects) { stat!!.registerHit(0, false, false); replay!!.addObjectScore(obj.getId(), ResultType.MISS) }
            while (objects!!.isNotEmpty()) { objects!!.poll(); stat!!.registerHit(0, false, false); replay!!.addObjectScore(++lastObjectId, ResultType.MISS) }
            val ctime = System.currentTimeMillis().toString()
            replayFile = Config.getCorePath() + "Scores/" + MD5Calculator.getStringMD5(lastTrack!!.filename + ctime) + ctime.substring(0, minOf(3, ctime.length)) + ".odr"
            val rf = replayFile!!
            replay!!.stat = stat; replay!!.save(rf); ScoreLibrary.getInstance().addScore(lastTrack!!.filename!!, stat!!, rf)
            ToastLogger.showText(StringTable.get(R.string.message_save_replay_successful), true); replayFile = null; return true
        } else { ToastLogger.showText(StringTable.get(R.string.message_save_replay_failed), true); return false }
    }

    private fun updatePPCounter(objectId: Int) { if (ppText == null) return; val obj = beatmapData!!.hitObjects.objects[objectId]; val time = if (obj is HitObjectWithDuration) obj.endTime else obj.startTime; ppText!!.text = String.format(Locale.ENGLISH, "%.2fpp", getPPAtTime(time)) }
    private fun getPPAtTime(time: Double): Double { val attrs = getAttributeAtTime(time) ?: return 0.0; return BeatmapDifficultyCalculator.calculatePerformance(attrs.attributes, stat!!).total }
    private fun getAttributeAtTime(time: Double): TimedDifficultyAttributes? {
        if (timedDifficultyAttributes.isNullOrEmpty()) return null; if (time < timedDifficultyAttributes!![0].time) return null
        if (time >= timedDifficultyAttributes!!.last().time) return timedDifficultyAttributes!!.last()
        var l = 0; var r = timedDifficultyAttributes!!.size - 2
        while (l <= r) { val pivot = l + ((r - l) shr 1); val attrs = timedDifficultyAttributes!![pivot]; when { attrs.time < time -> l = pivot + 1; attrs.time > time -> r = pivot - 1; else -> return attrs } }
        return timedDifficultyAttributes!![l]
    }
}
