package ru.nsu.ccfit.zuev.osu.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.edlplan.ext.EdExtensionHelper
import com.edlplan.favorite.FavoriteLibrary
import com.edlplan.replay.OdrDatabase
import com.edlplan.ui.fragment.FilterMenuFragment
import com.edlplan.ui.fragment.PropsMenuFragment
import com.edlplan.ui.fragment.ScoreMenuFragment
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.discord.DiscordRPC
import com.reco1l.legacy.ui.multiplayer.RoomScene
import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters
import com.rian.difficultycalculator.utils.LRUCache
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.ColorBackground
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.HorizontalAlign
import org.anddev.andengine.util.MathUtils
import java.text.SimpleDateFormat
import java.util.*
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.editor.EditorScene
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring
import ru.nsu.ccfit.zuev.osu.online.SeasonalBackgroundManager
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinLayout

class SongMenu : IUpdateHandler, MenuItemListener, IScrollBarListener {

    private var engine: Engine? = null
    var game: GameScene? = null
        private set
    private var scoreScene: ScoringScene? = null

    fun setScoringScene(ss: ScoringScene) {
        scoreScene = ss
    }
    private var camY = 0f
    private var velocityY = 0f
    private var context: Context? = null
    var scene: Scene? = null
        private set
    var frontLayer: Entity = Entity()
    var backLayer: Entity = Entity()
    private var items: ArrayList<MenuItem> = ArrayList()
    private var selectedItem: MenuItem? = null
    var selectedTrack: TrackInfo? = null
        private set
    private var bg: Sprite? = null
    private var bgLoaded = false
    private var bgName = ""
    internal var board: ScoreBoard? = null
    private var touchY: Float? = null
    private var filterText = ""
    private var favsOnly = false
    private var limitC: Set<String>? = null
    private var secondsSinceLastSelect = 0f
    private var maxY = 100500f
    private var pointerId = -1
    private var initalY = -1f
    private var secPassed = 0f
    private var tapTime = 0f
    private var backButton: Sprite? = null
    private var scrollbar: ScrollBar? = null
    private var trackInfo: ChangeableText? = null
    private var mapper: ChangeableText? = null
    private var beatmapInfo: ChangeableText? = null
    private var beatmapInfo2: ChangeableText? = null
    var dimensionInfo: ChangeableText? = null
        private set
    private var isSelectComplete = true
    private var scoringSwitcher: AnimSprite? = null
    internal var filterMenu: FilterMenuFragment? = null
    private var groupType: GroupType = GroupType.MapSet
    var isEditorMode = false
    private var previousSelectionTimer: Timer? = null
    private val previousSelectionInterval = 1000L
    private var previousSelectionPerformed = false
    private val previousSelectedItems: LinkedList<MenuItem> = LinkedList()
    private val mapStatuses = LRUCache<String, RankedStatus>(50)

    @SuppressLint("SimpleDateFormat")
    fun init(context: Activity, engine: Engine, pGame: GameScene) {
        this.engine = engine
        game = pGame
        this.context = context.applicationContext
    }

    fun loadFilter(filterMenu: IFilterMenu) {
        val favFolder = filterMenu.getFavoriteFolder()
        setFilter(filterMenu.getFilter(), filterMenu.getOrder(), filterMenu.isFavoritesOnly(),
            if (favFolder == null) null else FavoriteLibrary.get().getMaps(favFolder))
    }

    fun reload() {
        frontLayer = Entity()
        backLayer = Entity()
        scene?.unregisterUpdateHandler(this)
        scene?.setTouchAreaBindingEnabled(false)
        load()
        GlobalManager.getInstance().gameScene?.setOldScene(scene!!)
    }

    @Synchronized
    fun load() {
        scene = Scene()
        camY = 0f
        velocityY = 0f
        selectedItem = null
        items = ArrayList()
        selectedTrack = null
        bgLoaded = true
        SongMenuPool.getInstance().init()
        loadFilterFragment()
        if (!Multiplayer.isMultiplayer) {
            val savedMod = ModMenu.getInstance().mod.clone()
            val savedSpeed = ModMenu.getInstance().changeSpeed
            val savedFL = ModMenu.getInstance().FLfollowDelay
            val savedCustomAR = ModMenu.getInstance().getCustomAR()
            val savedCustomOD = ModMenu.getInstance().getCustomOD()
            val savedCustomCS = ModMenu.getInstance().getCustomCS()
            val savedCustomHP = ModMenu.getInstance().getCustomHP()
            ModMenu.getInstance().reload()
            ModMenu.getInstance().setMod(savedMod)
            ModMenu.getInstance().changeSpeed = savedSpeed
            ModMenu.getInstance().FLfollowDelay = savedFL
            if (savedCustomAR != null) ModMenu.getInstance().setCustomAR(savedCustomAR)
            if (savedCustomOD != null) ModMenu.getInstance().setCustomOD(savedCustomOD)
            if (savedCustomCS != null) ModMenu.getInstance().setCustomCS(savedCustomCS)
            if (savedCustomHP != null) ModMenu.getInstance().setCustomHP(savedCustomHP)
        }
        bindDataBaseChangedListener()
        scene!!.attachChild(backLayer)
        scene!!.attachChild(frontLayer)
        val tex: TextureRegion = ResourceManager.getInstance().getTexture("menu-background")!!
        var height = tex.height.toFloat()
        height *= Config.getRES_WIDTH() / tex.width.toFloat()
        val bgSprite = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, tex)
        scene!!.setBackground(SpriteBackground(bgSprite))
        val bgDimRect = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        bgDimRect.setColor(0f, 0f, 0f, 0.2f)
        backLayer.attachChild(bgDimRect)
        board = ScoreBoard(scene!!, backLayer, this)
        for (i in LibraryManager.INSTANCE.getLibrary()) {
            val item = MenuItem(this, i)
            items.add(item)
            item.attachToScene(scene!!, backLayer)
        }
        sortOrder = SortOrder.Title
        sort()
        if (items.isEmpty()) {
            val text = Text(0f, 0f, ResourceManager.getInstance().getFont("CaptionFont"),
                "There are no songs in library, try using the beatmap downloader", HorizontalAlign.CENTER)
            text.setPosition(Config.getRES_WIDTH() / 2f - text.width / 2, Config.getRES_HEIGHT() / 2f - text.height / 2)
            text.setScale(1.5f)
            text.setColor(0f, 0f, 0f)
            scene!!.attachChild(text)
            return
        }
        scene!!.setOnSceneTouchListener { _, evt ->
            if (evt.x < Config.getRES_WIDTH() / 5f * 2) return@setOnSceneTouchListener false
            when (evt.action) {
                TouchEvent.ACTION_DOWN -> {
                    velocityY = 0f
                    touchY = evt.y
                    pointerId = evt.pointerID
                    tapTime = secPassed
                    initalY = touchY!!
                }
                TouchEvent.ACTION_MOVE -> {
                    if (pointerId != -1 && pointerId != evt.pointerID) return@setOnSceneTouchListener true
                    if (initalY == -1f) {
                        velocityY = 0f
                        touchY = evt.y
                        initalY = touchY!!
                        tapTime = secPassed
                        pointerId = evt.pointerID
                    }
                    val dy = evt.y - touchY!!
                    camY -= dy
                    touchY = evt.y
                    if (camY <= -Config.getRES_HEIGHT() / 2f) { camY = -Config.getRES_HEIGHT() / 2f; velocityY = 0f }
                    else if (camY >= maxY) { camY = maxY; velocityY = 0f }
                }
                else -> {
                    if (pointerId != -1 && pointerId != evt.pointerID) return@setOnSceneTouchListener true
                    touchY = null
                    if (secPassed - tapTime < 0.001f || initalY == -1f) velocityY = 0f
                    else velocityY = (initalY - evt.y) / (secPassed - tapTime)
                    pointerId = -1
                    initalY = -1f
                }
            }
            true
        }
        scene!!.registerUpdateHandler(this)
        scene!!.setTouchAreaBindingEnabled(true)
        scrollbar = ScrollBar(scene!!)
        val songSelectTopTexture: TextureRegion = ResourceManager.getInstance().getTexture("songselect-top")!!
        val songSelectTop = Sprite(0f, 0f, songSelectTopTexture)
        songSelectTop.setSize(songSelectTopTexture.width * songSelectTopTexture.height / 184f, 184f)
        songSelectTop.setPosition(-1640f, songSelectTop.y)
        songSelectTop.setAlpha(0.6f)
        frontLayer.attachChild(songSelectTop)
        trackInfo = ChangeableText(Utils.toRes(70).toFloat(), Utils.toRes(2).toFloat(),
            ResourceManager.getInstance().getFont("font"), "title", 1024)
        frontLayer.attachChild(trackInfo)
        mapper = ChangeableText(Utils.toRes(70).toFloat(), trackInfo!!.y + trackInfo!!.height + Utils.toRes(2),
            ResourceManager.getInstance().getFont("middleFont"), "mapper", 1024)
        frontLayer.attachChild(mapper)
        beatmapInfo = ChangeableText(Utils.toRes(4).toFloat(), mapper!!.y + mapper!!.height + Utils.toRes(2),
            ResourceManager.getInstance().getFont("middleFont"), "beatmapInfo", 1024)
        frontLayer.attachChild(beatmapInfo)
        beatmapInfo2 = ChangeableText(Utils.toRes(4).toFloat(), beatmapInfo!!.y + beatmapInfo!!.height + Utils.toRes(2),
            ResourceManager.getInstance().getFont("middleFont"), "beatmapInfo2", 1024)
        frontLayer.attachChild(beatmapInfo2)
        dimensionInfo = ChangeableText(Utils.toRes(4).toFloat(), beatmapInfo2!!.y + beatmapInfo2!!.height + Utils.toRes(2),
            ResourceManager.getInstance().getFont("smallFont"), "dimensionInfo", 1024)
        frontLayer.attachChild(dimensionInfo)
        val layoutBackButton: SkinLayout? = OsuSkin.get().getLayout("BackButton")
        var layoutMods: SkinLayout? = null
        if (!Multiplayer.isMultiplayer) layoutMods = OsuSkin.get().getLayout("ModsButton")
        val layoutOptions: SkinLayout? = OsuSkin.get().getLayout("OptionsButton")
        val layoutRandom: SkinLayout? = OsuSkin.get().getLayout("RandomButton")
        if (ResourceManager.getInstance().isTextureLoaded("menu-back-0")) {
            val loadedBackTextures = ArrayList<String>()
            for (i in 0 until 60) {
                if (ResourceManager.getInstance().isTextureLoaded("menu-back-$i")) loadedBackTextures.add("menu-back-$i")
            }
            backButton = object : AnimSprite(0f, 0f, loadedBackTextures.size.toFloat(), *loadedBackTextures.toTypedArray()) {
                var moved = false
                var dx = 0f; var dy = 0f
                val scaleWhenHold = layoutBackButton?.property?.optBoolean("scaleWhenHold", true) ?: true
                override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                    if (pSceneTouchEvent.isActionDown) { if (scaleWhenHold) backButton?.setScale(1.25f); moved = false; dx = pTouchAreaLocalX; dy = pTouchAreaLocalY; ResourceManager.getInstance().getSound("menuback")?.play(); return true }
                    if (pSceneTouchEvent.isActionUp) { if (selectedTrack == null) return true; if (!moved) { backButton?.setScale(1f); back() }; return true }
                    if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) { backButton?.setScale(1f); moved = true }
                    return false
                }
            }
        } else {
            backButton = object : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("menu-back") ?: ResourceManager.getInstance().getTexture("menu-background")!!) {
                var moved = false
                var dx = 0f; var dy = 0f
                val scaleWhenHold = layoutBackButton?.property?.optBoolean("scaleWhenHold", true) ?: true
                override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                    if (pSceneTouchEvent.isActionDown) { if (scaleWhenHold) backButton?.setScale(1.25f); moved = false; dx = pTouchAreaLocalX; dy = pTouchAreaLocalY; ResourceManager.getInstance().getSound("menuback")?.play(); return true }
                    if (pSceneTouchEvent.isActionUp) { if (selectedTrack == null) return true; if (!moved) { backButton?.setScale(1f); back() }; return true }
                    if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) { backButton?.setScale(1f); moved = true }
                    return false
                }
            }
        }
        var modSelection: AnimSprite? = null
        if (!Multiplayer.isMultiplayer) modSelection = object : AnimSprite(0f, 0f, 0f, "selection-mods", "selection-mods-over") {
            var moved = false; var dx = 0f; var dy = 0f
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) { setFrame(1); moved = false; dx = pTouchAreaLocalX; dy = pTouchAreaLocalY; return true }
                if (pSceneTouchEvent.isActionUp) { setFrame(0); if (!moved) { velocityY = 0f; ModMenu.getInstance().show(scene!!, selectedTrack) }; return true }
                if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) { moved = true; setFrame(0) }
                return false
            }
        }
        val optionSelection = object : AnimSprite(0f, 0f, 0f, "selection-options", "selection-options-over") {
            var moved = false; var dx = 0f; var dy = 0f
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) { setFrame(1); moved = false; dx = pTouchAreaLocalX; dy = pTouchAreaLocalY; return true }
                if (pSceneTouchEvent.isActionUp) { setFrame(0); if (!moved) { velocityY = 0f; if (filterMenu == null) loadFilterFragment(); filterMenu?.showMenu(this@SongMenu) }; return true }
                if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) { moved = true; setFrame(0) }
                return false
            }
        }
        val randomMap = object : AnimSprite(0f, 0f, 0f, "selection-random", "selection-random-over") {
            var moved = false; var dx = 0f; var dy = 0f
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setFrame(1); moved = false; dx = pTouchAreaLocalX; dy = pTouchAreaLocalY
                    previousSelectionTimer?.cancel()
                    previousSelectionTimer = Timer()
                    previousSelectionTimer!!.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            if (!isSelectComplete) return
                            var previousItem = previousSelectedItems.pollLast()
                            while (previousItem != null && previousItem.isDeleted()) previousItem = previousSelectedItems.pollLast()
                            if (previousItem == null) { cancel(); return }
                            previousSelectionPerformed = true
                            ResourceManager.getInstance().getSound("menuclick").play()
                            previousItem.select(true, true)
                        }
                        override fun cancel(): Boolean { previousSelectionTimer = null; return super.cancel() }
                    }, previousSelectionInterval, previousSelectionInterval)
                    previousSelectionPerformed = false
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    setFrame(0); previousSelectionTimer?.cancel()
                    if (!isSelectComplete) return true
                    if (!moved && !previousSelectionPerformed) {
                        velocityY = 0f
                        if (items.size <= 1) return true
                        var rnd = MathUtils.random(0, items.size - 1)
                        var index = 0
                        while (rnd > 0) {
                            rnd--; val oldIndex = index
                            do { index = (index + 1) % items.size; if (index == oldIndex) return true } while (!items[index].isVisible())
                        }
                        if (!items[index].isVisible()) return true
                        if (selectedItem == items[index]) return true
                        ResourceManager.getInstance().getSound("menuclick").play()
                        items[index].select(true, true)
                    }
                    previousSelectionPerformed = false
                    return true
                }
                if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) {
                    moved = true; setFrame(0)
                    if (pSceneTouchEvent.isActionOutside) previousSelectionTimer?.cancel()
                }
                return false
            }
        }
        modSelection?.setScale(1.5f)
        optionSelection.setScale(1.5f)
        randomMap.setScale(1.5f)
        if (OsuSkin.get().isUseNewLayout()) {
            layoutBackButton?.baseApply(backButton!!)
            if (layoutMods != null && modSelection != null) layoutMods.baseApply(modSelection)
            layoutOptions?.baseApply(optionSelection)
            layoutRandom?.baseApply(randomMap)
            backButton?.setPosition(0f, Config.getRES_HEIGHT() - backButton!!.heightScaled)
            if (modSelection != null) {
                modSelection.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - modSelection.heightScaled)
                optionSelection.setPosition(modSelection.x + modSelection.widthScaled, Config.getRES_HEIGHT() - optionSelection.heightScaled)
            } else {
                optionSelection.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - optionSelection.heightScaled)
            }
            randomMap.setPosition(optionSelection.x + optionSelection.widthScaled, Config.getRES_HEIGHT() - randomMap.heightScaled)
        } else {
            backButton?.setPosition(0f, Config.getRES_HEIGHT() - backButton!!.height)
            if (modSelection != null) {
                modSelection.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - Utils.toRes(90).toFloat())
                optionSelection.setPosition(modSelection.x + modSelection.widthScaled, Config.getRES_HEIGHT() - Utils.toRes(90).toFloat())
            } else {
                optionSelection.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - Utils.toRes(90).toFloat())
            }
            randomMap.setPosition(optionSelection.x + optionSelection.widthScaled, Config.getRES_HEIGHT() - Utils.toRes(90).toFloat())
        }
        frontLayer.attachChild(backButton)
        scene!!.registerTouchArea(backButton)
        if (modSelection != null) { frontLayer.attachChild(modSelection); scene!!.registerTouchArea(modSelection) }
        frontLayer.attachChild(optionSelection)
        scene!!.registerTouchArea(optionSelection)
        if (isEditorMode) {
            modSelection?.setVisible(false); modSelection?.setIgnoreUpdate(true)
            randomMap.setVisible(false); randomMap.setIgnoreUpdate(true)
            val createButton = object : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("menu-back-0") ?: ResourceManager.getInstance().getTexture("menu-background")!!) {
                var moved = false; var dx = 0f; var dy = 0f
                override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                    if (pSceneTouchEvent.isActionDown) { setColor(0.7f, 0.7f, 0.7f); moved = false; dx = pTouchAreaLocalX; dy = pTouchAreaLocalY; return true }
                    if (pSceneTouchEvent.isActionUp) { setColor(1f, 1f, 1f); if (!moved) createNewBeatmap(); return true }
                    if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) { setColor(1f, 1f, 1f); moved = true }
                    return false
                }
            }
            createButton.setColor(0.4f, 0.8f, 0.4f)
            createButton.setPosition(optionSelection.x + optionSelection.widthScaled, Config.getRES_HEIGHT() - createButton.height)
            frontLayer.attachChild(createButton)
            scene!!.registerTouchArea(createButton)
        } else {
            frontLayer.attachChild(randomMap)
            scene!!.registerTouchArea(randomMap)
        }
        if (OnlineScoring.getInstance().createSecondPanel() != null) {
            val panel: OnlinePanel = OnlineScoring.getInstance().secondPanel!!
            panel.detachSelf()
            panel.setPosition(randomMap.x + randomMap.widthScaled - 18, Config.getRES_HEIGHT() - Utils.toRes(110).toFloat())
            OnlineScoring.getInstance().loadAvatar(false)
            OnlineScoring.getInstance().loadBanner(false)
            frontLayer.attachChild(panel)
            scoringSwitcher = object : AnimSprite(Utils.toRes(5).toFloat(), Utils.toRes(10).toFloat(), 0f, "ranking_disabled", "ranking_enabled", "selection-ranked", "selection-approved", "selection-loved", "selection-question") {
                override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                    if (!pSceneTouchEvent.isActionDown) return false
                    toggleScoringSwitcher()
                    return true
                }
            }
            scoringSwitcher!!.setFrame(0)
            scoringSwitcher!!.setPosition(10f, 10f)
            scene!!.registerTouchArea(scoringSwitcher)
            frontLayer.attachChild(scoringSwitcher)
        }
    }

    fun loadFilterFragment() {
        filterMenu = FilterMenuFragment()
        filterMenu!!.loadConfig(context!!)
    }

    fun unloadFilterFragment() {
        scene?.clearChildScene()
        filterMenu = null
    }

    fun toggleScoringSwitcher() {
        if (board!!.isShowOnlineScores) {
            board!!.setShowOnlineScores(false)
            board!!.init(selectedTrack)
            updateInfo(selectedTrack)
        } else if (OnlineManager.getInstance().isStayOnline) {
            board!!.setShowOnlineScores(true)
            board!!.init(selectedTrack)
        }
        updateScoringSwitcherStatus(true)
    }

    fun show() {
        engine?.setScene(scene)
        DiscordRPC.updateForSongSelection()
        SeasonalBackgroundManager.stopPeriodicRefresh()
        if (GlobalManager.getInstance().songService == null) return
        val track = selectedTrack ?: GlobalManager.getInstance().selectedTrack
        if (track != null && GlobalManager.getInstance().songService!!.status == Status.STOPPED) {
            playMusic(track.audioFilename!!, track.previewTime)
        } else {
            val speed = ModMenu.getInstance().speed
            val enableNC = ModMenu.getInstance().isEnableNCWhenSpeedChange || ModMenu.getInstance().mod.contains(GameMod.MOD_NIGHTCORE)
            GlobalManager.getInstance().songService!!.applySpeed(speed, enableNC)
        }
    }

    @JvmOverloads
    fun setFilter(filter: String?, order: SortOrder, favsOnly: Boolean, limit: Set<String>? = null) {
        var oldTrackFileName = ""
        if (selectedTrack != null) oldTrackFileName = selectedTrack!!.filename!!
        if (order != sortOrder) {
            sortOrder = order
            tryReloadMenuItems(sortOrder)
            sort()
            reSelectItem(oldTrackFileName)
        }
        if (filter == null || filterText == filter) {
            if (favsOnly == this.favsOnly && limitC == limit) return
        }
        limitC = limit
        filterText = filter ?: ""
        camY = 0f
        velocityY = 0f
        val lowerFilter = filterText.lowercase()
        for (item in items) item.applyFilter(lowerFilter, favsOnly, limit)
        if (favsOnly != this.favsOnly) this.favsOnly = favsOnly else reSelectItem(oldTrackFileName)
        if (selectedItem != null && !selectedItem!!.isVisible()) { selectedItem = null; selectedTrack = null }
    }

    fun sort() {
        if (sortOrder != filterMenu?.getOrder()) sortOrder = filterMenu?.getOrder() ?: SortOrder.Title
        Collections.sort(items) { i1, i2 ->
            var s1: String; var s2: String
            when (sortOrder) {
                SortOrder.Artist -> { s1 = i1.beatmap.artist ?: ""; s2 = i2.beatmap.artist ?: "" }
                SortOrder.Creator -> { s1 = i1.beatmap.creator ?: ""; s2 = i2.beatmap.creator ?: "" }
                SortOrder.Date -> return@sort i2.beatmap.date.compareTo(i1.beatmap.date)
                SortOrder.Bpm -> return@sort java.lang.Float.compare(i2.getFirstTrack().bpmMax, i1.getFirstTrack().bpmMax)
                SortOrder.Stars -> return@sort java.lang.Float.compare(i2.getFirstTrack().difficulty, i1.getFirstTrack().difficulty)
                SortOrder.Length -> return@sort i2.getFirstTrack().musicLength.compareTo(i1.getFirstTrack().musicLength)
                else -> { s1 = i1.beatmap.title ?: ""; s2 = i2.beatmap.title ?: "" }
            }
            s1.compareTo(s2, ignoreCase = true)
        }
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        secPassed += pSecondsElapsed
        increaseVolume()
        increaseBackgroundLuminance(pSecondsElapsed)
        secondsSinceLastSelect += pSecondsElapsed
        var oy = -camY
        for (item in items) {
            val cy = oy + Config.getRES_HEIGHT() / 2f + item.getHeight() / 2
            var ox = Config.getRES_WIDTH() / 1.85f + 200 * Math.abs(Math.cos(cy * Math.PI / (Config.getRES_HEIGHT() * 2))).toFloat()
            ox = Utils.toRes(ox)
            item.setPos(ox, oy)
            oy += item.getHeight()
        }
        oy += camY
        camY += velocityY * pSecondsElapsed
        maxY = oy - Config.getRES_HEIGHT() / 2f
        if (camY <= -Config.getRES_HEIGHT() / 2f && velocityY < 0 || camY >= maxY && velocityY > 0) { camY -= velocityY * pSecondsElapsed; velocityY = 0f }
        if (Math.abs(velocityY) > Utils.toRes(1000) * pSecondsElapsed) velocityY -= Utils.toRes(1000) * pSecondsElapsed * Math.signum(velocityY) else velocityY = 0f
        expandSelectedItem(pSecondsElapsed)
        updateScrollbar(camY + Config.getRES_HEIGHT() / 2f, oy)
    }

    fun increaseVolume() {
        if (GlobalManager.getInstance().songService != null) {
            synchronized(musicMutex) {
                if (GlobalManager.getInstance().songService != null && GlobalManager.getInstance().songService!!.status == Status.PLAYING && GlobalManager.getInstance().songService!!.volume < Config.getBgmVolume()) {
                    val vol = minOf(1f, GlobalManager.getInstance().songService!!.volume + 0.01f)
                    GlobalManager.getInstance().songService!!.setVolume(vol)
                }
            }
        }
    }

    fun increaseBackgroundLuminance(pSecondsElapsed: Float) {
        if (bg != null) {
            synchronized(backgroundMutex) {
                if (bg != null && bg!!.red < 1) {
                    val col = minOf(1f, bg!!.red + pSecondsElapsed)
                    bg!!.setColor(col, col, col)
                }
            }
        }
    }

    fun expandSelectedItem(pSecondsElapsed: Float) {
        if (selectedItem != null) {
            if (selectedItem!!.percentAppeared < 1) selectedItem!!.percentAppeared += 2 * pSecondsElapsed else selectedItem!!.percentAppeared = 1f
            selectedItem!!.update(pSecondsElapsed)
        }
    }

    fun updateScrollbar(vy: Float, maxy: Float) {
        scrollbar?.setPosition(vy, maxy)
        scrollbar?.setVisible(Math.abs(velocityY) > Utils.toRes(500))
    }

    override fun reset() {}

    override fun select(item: MenuItem) {
        secondsSinceLastSelect = 0f
        if (selectedItem != null) {
            selectedItem!!.deselect()
            if (!previousSelectionPerformed) {
                while (previousSelectedItems.size >= 10) previousSelectedItems.pollFirst()
                previousSelectedItems.addLast(selectedItem)
            }
        }
        selectedItem = item
        velocityY = 0f
        selectedTrack = null
        var height = 0f
        for (i in items.indices) {
            if (items[i] === selectedItem) break
            height += items[i].getInitialHeight()
        }
        camY = height - Config.getRES_HEIGHT() / 2f
        camY += item.getTotalHeight() / 2
    }

    @SuppressLint("SimpleDateFormat")
    fun changeDimensionInfo(track: TrackInfo?) {
        if (track == null) return
        var ar = track.approachRate
        var od = track.overallDifficulty
        var cs = track.circleSize
        var hp = track.hpDrain
        var bpm_max = track.bpmMax
        var bpm_min = track.bpmMin
        var length = track.musicLength
        val mod: EnumSet<GameMod> = ModMenu.getInstance().mod
        dimensionInfo?.setColor(1f, 1f, 1f)
        beatmapInfo?.setColor(1f, 1f, 1f)
        if (mod.contains(GameMod.MOD_EASY)) { ar *= 0.5f; od *= 0.5f; cs -= 1f; hp *= 0.5f; dimensionInfo?.setColor(46 / 255f, 139 / 255f, 87 / 255f) }
        if (mod.contains(GameMod.MOD_HARDROCK) || mod.contains(GameMod.MOD_PRECISE)) {
            if (mod.contains(GameMod.MOD_HARDROCK)) { ar = minOf(ar * 1.4f, 10f); od = minOf(od * 1.4f, 10f); ++cs; hp = minOf(hp * 1.4f, 10f) }
            dimensionInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f)
        }
        if (ModMenu.getInstance().changeSpeed != 1f) {
            val speed = ModMenu.getInstance().speed; bpm_max *= speed; bpm_min *= speed; length = (length / speed).toLong()
            if (speed > 1) { beatmapInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f); dimensionInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f) }
            else if (speed < 1) { beatmapInfo?.setColor(46 / 255f, 139 / 255f, 87 / 255f); dimensionInfo?.setColor(46 / 255f, 139 / 255f, 87 / 255f) }
        } else {
            if (mod.contains(GameMod.MOD_DOUBLETIME)) { bpm_max *= 1.5f; bpm_min *= 1.5f; length = (length * 2 / 3f).toLong(); beatmapInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f); dimensionInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f) }
            if (mod.contains(GameMod.MOD_NIGHTCORE)) { bpm_max *= 1.5f; bpm_min *= 1.5f; length = (length * 2 / 3f).toLong(); beatmapInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f); dimensionInfo?.setColor(205 / 255f, 85 / 255f, 85 / 255f) }
            if (mod.contains(GameMod.MOD_HALFTIME)) { bpm_max *= 0.75f; bpm_min *= 0.75f; length = (length * 4 / 3f).toLong(); beatmapInfo?.setColor(46 / 255f, 139 / 255f, 87 / 255f); dimensionInfo?.setColor(46 / 255f, 139 / 255f, 87 / 255f) }
        }
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            if (mod.contains(GameMod.MOD_EASY)) { ar *= 2f; ar -= 0.5f }
            ar -= 0.5f
            if (ModMenu.getInstance().changeSpeed != 1f) ar -= ModMenu.getInstance().speed - 1.0f
            else if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) ar -= 0.5f
            od *= 0.5f; cs -= 1f; hp *= 0.5f
            dimensionInfo?.setColor(46 / 255f, 139 / 255f, 87 / 255f)
        }
        @SuppressLint("SimpleDateFormat") var sdf = SimpleDateFormat("mm:ss")
        sdf.timeZone = TimeZone.getTimeZone("GMT+0")
        var binfoStr = StringTable.get(R.string.binfoStr1).format(sdf.format(length),
            if (bpm_min == bpm_max) "${GameHelper.Round(bpm_min, 1)}" else "${GameHelper.Round(bpm_min, 1)}-${GameHelper.Round(bpm_max, 1)}", track.maxCombo)
        if (length > 3600 * 1000) {
            sdf = SimpleDateFormat("HH:mm:ss")
            sdf.timeZone = TimeZone.getTimeZone("GMT+0")
            binfoStr = StringTable.get(R.string.binfoStr1).format(sdf.format(length),
                if (bpm_min == bpm_max) "${GameHelper.Round(bpm_min, 1)}" else "${GameHelper.Round(bpm_min, 1)}-${GameHelper.Round(bpm_max, 1)}", track.maxCombo)
        }
        beatmapInfo?.setText(binfoStr)
        val dimensionStringBuilder = StringBuilder()
        if (ModMenu.getInstance().changeSpeed != 1f) {
            val speed = ModMenu.getInstance().speed
            ar = GameHelper.ms2ar(GameHelper.ar2ms(ar.toDouble()) / speed).let { GameHelper.Round(it.toDouble(), 2).toFloat() }
            od = GameHelper.ms2od(GameHelper.od2ms(od) / speed).let { GameHelper.Round(it.toDouble(), 2).toFloat() }
        } else if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
            ar = GameHelper.ms2ar(GameHelper.ar2ms(ar.toDouble()) * 2 / 3).let { GameHelper.Round(it.toDouble(), 2).toFloat() }
            od = GameHelper.ms2od(GameHelper.od2ms(od) * 2 / 3).let { GameHelper.Round(it.toDouble(), 2).toFloat() }
        } else if (mod.contains(GameMod.MOD_HALFTIME)) {
            ar = GameHelper.ms2ar(GameHelper.ar2ms(ar.toDouble()) * 4 / 3).let { GameHelper.Round(it.toDouble(), 2).toFloat() }
            od = GameHelper.ms2od(GameHelper.od2ms(od) * 4 / 3).let { GameHelper.Round(it.toDouble(), 2).toFloat() }
        }
        val rawAR = ar; val rawOD = od; val rawCS = cs; val rawHP = hp
        if (ModMenu.getInstance().isCustomAR()) ar = ModMenu.getInstance().getCustomAR()!!
        if (ModMenu.getInstance().isCustomOD()) od = ModMenu.getInstance().getCustomOD()!!
        if (ModMenu.getInstance().isCustomCS()) cs = ModMenu.getInstance().getCustomCS()!!
        if (ModMenu.getInstance().isCustomHP()) hp = ModMenu.getInstance().getCustomHP()!!
        if (ar != rawAR || od != rawOD || cs != rawCS || hp != rawHP) dimensionInfo?.setColor(255 / 255f, 180 / 255f, 0 / 255f)
        dimensionStringBuilder.append("AR: ").append(GameHelper.Round(ar, 2)).append(" ")
            .append("OD: ").append(GameHelper.Round(od, 2)).append(" ")
            .append("CS: ").append(GameHelper.Round(cs, 2)).append(" ")
            .append("HP: ").append(GameHelper.Round(hp, 2)).append(" ")
            .append("Stars: ").append(GameHelper.Round(track.difficulty, 2))
        dimensionInfo?.setText(dimensionStringBuilder.toString())
    }

    fun updateInfo(track: TrackInfo?) {
        if (track == null) return
        val beatmap = track.beatmap!!
        val tinfoStr = (if (beatmap.artistUnicode == null || Config.isForceRomanized()) beatmap.artist else beatmap.artistUnicode) + " - " +
                (if (beatmap.titleUnicode == null || Config.isForceRomanized()) beatmap.title else beatmap.titleUnicode) + " [" + track.mode + "]"
        val mapperStr = "Beatmap by " + track.creator
        val binfoStr2 = StringTable.get(R.string.binfoStr2).format(track.hitCircleCount, track.sliderCount, track.spinnerCount, track.beatmapSetID)
        trackInfo?.setText(tinfoStr)
        mapper?.setText(mapperStr)
        beatmapInfo2?.setText(binfoStr2)
        changeDimensionInfo(track)
        Execution.async {
            val beatmapData = BeatmapParser(track.filename!!).setCalculator(true).parse(true)
            if (beatmapData == null) { setStarsDisplay(0f); return@async }
            if (selectedTrack != null && beatmapData.getMD5() != selectedTrack!!.getMD5()) return@async
            beatmapData.populateMetadata(track)
            changeDimensionInfo(track)
            val parameters = DifficultyCalculationParameters()
            val modMenu = ModMenu.getInstance()
            parameters.mods = modMenu.mod
            parameters.customSpeedMultiplier = modMenu.changeSpeed
            if (modMenu.isCustomCS()) parameters.customCS = modMenu.getCustomCS()!!
            if (modMenu.isCustomAR()) parameters.customAR = modMenu.getCustomAR()!!
            if (modMenu.isCustomOD()) parameters.customOD = modMenu.getCustomOD()!!
            val attributes = BeatmapDifficultyCalculator.calculateDifficulty(beatmapData, parameters)
            setStarsDisplay(GameHelper.Round(attributes.starRating, 2).toFloat())
        }
    }

    override fun selectTrack(track: TrackInfo, reloadBG: Boolean) {
        val selectedAudioTrack = this.selectedTrack ?: GlobalManager.getInstance().selectedTrack
        if (selectedAudioTrack == null || selectedAudioTrack.audioFilename != track.audioFilename) {
            playMusic(track.audioFilename!!, track.previewTime)
        }
        if (selectedTrack === track) {
            synchronized(bgMutex) { if (!bgLoaded) return }
            ResourceManager.getInstance().getSound("menuhit").play()
            if (Multiplayer.isMultiplayer) { setMultiplayerRoomBeatmap(selectedTrack); back(false); return }
            stopMusic()
            if (isEditorMode) { openEditorForTrack(track); isEditorMode = false; unload(); return }
            Replay.oldMod = ModMenu.getInstance().mod
            Replay.oldChangeSpeed = ModMenu.getInstance().changeSpeed
            Replay.oldCustomAR = ModMenu.getInstance().getCustomAR()
            Replay.oldCustomOD = ModMenu.getInstance().getCustomOD()
            Replay.oldCustomCS = ModMenu.getInstance().getCustomCS()
            Replay.oldCustomHP = ModMenu.getInstance().getCustomHP()
            Replay.oldFLFollowDelay = ModMenu.getInstance().FLfollowDelay
            game?.startGame(track, null)
            unload()
            return
        }
        isSelectComplete = false
        selectedTrack = track
        EdExtensionHelper.onSelectTrack(track)
        GlobalManager.getInstance().selectedTrack = track
        updateInfo(track)
        updateScoringSwitcherStatus(false)
        board?.init(track)
        val quality = Config.getBackgroundQuality()
        synchronized(backgroundMutex) {
            if (!reloadBG && (track.background == null || bgName == track.background)) { isSelectComplete = true; return }
            bgName = track.background ?: ""
            bg = null
            bgLoaded = false
            scene?.setBackground(ColorBackground(0f, 0f, 0f))
            if (quality == 0) Config.setBackgroundQuality(4)
        }
        Execution.async {
            synchronized(backgroundMutex) {
                val tex: TextureRegion? = if (Config.isSafeBeatmapBg() || track.background == null) ResourceManager.getInstance().getTexture("menu-background") else ResourceManager.getInstance().loadBackground(bgName)
                if (tex != null) {
                    var h = tex.height.toFloat()
                    h *= Config.getRES_WIDTH() / tex.width.toFloat()
                    bg = Sprite(0f, (Config.getRES_HEIGHT() - h) / 2, Config.getRES_WIDTH().toFloat(), h, tex)
                    bg!!.setColor(0f, 0f, 0f)
                }
                Execution.updateThread {
                    synchronized(backgroundMutex) {
                        if (bg == null) {
                            val tex1: TextureRegion = ResourceManager.getInstance().getTexture("menu-background")!!
                            var height1 = tex1.height.toFloat()
                            height1 *= Config.getRES_WIDTH() / tex1.width.toFloat()
                            bg = Sprite(0f, (Config.getRES_HEIGHT() - height1) / 2, Config.getRES_WIDTH().toFloat(), height1, tex1)
                            bgName = ""
                        }
                        scene?.setBackground(SpriteBackground(bg!!))
                        Config.setBackgroundQuality(quality)
                        synchronized(bgMutex) { bgLoaded = true }
                    }
                }
            }
            isSelectComplete = true
        }
    }

    override fun stopScroll(y: Float) {
        velocityY = 0f
        touchY = y
        initalY = -1f
    }

    fun updateScore() {
        board?.init(selectedTrack)
        selectedItem?.updateMarks()
    }

    override fun openScore(id: Int, showOnline: Boolean, playerName: String) {
        if (showOnline) {
            engine?.setScene(LoadingScreen().scene)
            ToastLogger.showTextId(com.edlplan.osudroidresource.R.string.online_loadrecord, false)
            Execution.async {
                try {
                    val scorePack = OnlineManager.getInstance().getScorePack(id)
                    val params = scorePack.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (params.size < 11) return@async
                    val stat = StatisticV2(params)
                    if (stat.isLegacySC) stat.processLegacySC(selectedTrack!!)
                    stat.setPlayerName(playerName)
                    scoreScene?.load(stat, null, null, OnlineManager.getReplayURL(id), null, selectedTrack!!)
                    DiscordRPC.updateForResults()
                    engine?.setScene(scoreScene?.scene)
                } catch (e: OnlineManagerException) {
                    Debug.e("Cannot load play info: ${e.message}", e)
                    engine?.setScene(scene)
                }
            }
            return
        }
        val stat = ScoreLibrary.getInstance().getScore(id)
        if (stat.isLegacySC) stat.processLegacySC(selectedTrack!!)
        scoreScene?.load(stat, null, null, stat.replayName, null, selectedTrack!!)
        DiscordRPC.updateForResults()
        engine?.setScene(scoreScene?.scene)
    }

    override fun onScroll(where: Float) {
        velocityY = 0f
        camY = where - Config.getRES_HEIGHT() / 2f
    }

    fun unload() {}

    private fun openEditorForTrack(track: TrackInfo?) {
        if (track == null) return
        ResourceManager.getInstance().getSound("menuhit").play()
        stopMusic()
        Execution.async {
            val beatmapData = BeatmapParser(track.filename!!).parse(true)
            if (beatmapData == null) { ToastLogger.showText("Failed to parse beatmap for editing", true); return@async }
            val osuPath = track.filename!!
            Execution.updateThread {
                val manager = GlobalManager.getInstance()
                val editorScene = EditorScene(manager.engine!!)
                manager.editorScene = editorScene
                editorScene.loadBeatmap(beatmapData, osuPath)
                editorScene.show()
            }
        }
    }

    fun back() = back(true)

    private fun back(resetMultiplayerBeatmap: Boolean) {
        unbindDataBaseChangedListener()
        isEditorMode = false
        GlobalManager.getInstance().songService?.applySpeed(1.0f, false)
        if (Multiplayer.isMultiplayer) {
            if (resetMultiplayerBeatmap) resetMultiplayerRoomBeatmap()
            RoomScene.show()
            return
        }
        GlobalManager.getInstance().mainScene?.show()
    }

    private fun resetMultiplayerRoomBeatmap() {
        if (!Multiplayer.isMultiplayer) return
        RoomScene.awaitBeatmapChange = true
        if (!Multiplayer.isConnected) return
        val room = Multiplayer.room
        val beatmap = room?.previousBeatmap
        if (room != null && beatmap != null) {
            RoomAPI.changeBeatmap(beatmap.md5, beatmap.title ?: "", beatmap.artist ?: "", beatmap.version ?: "", beatmap.creator ?: "")
        } else {
            RoomAPI.changeBeatmap()
        }
    }

    private fun setMultiplayerRoomBeatmap(track: TrackInfo?) {
        if (!Multiplayer.isMultiplayer) return
        RoomScene.awaitBeatmapChange = true
        if (!Multiplayer.isConnected) return
        if (track != null) {
            RoomAPI.changeBeatmap(track.getMD5(), track.beatmap?.title ?: "", track.beatmap?.artist ?: "", track.mode ?: "", track.creator ?: "")
        } else {
            RoomAPI.changeBeatmap()
        }
    }

    fun bindDataBaseChangedListener() {
        OdrDatabase.get().onDatabaseChangedListener = Runnable { reloadScoreBroad() }
    }

    fun unbindDataBaseChangedListener() {
        OdrDatabase.get().onDatabaseChangedListener = null
    }

    override fun setY(y: Float) {
        velocityY = 0f
        camY = y
    }

    fun stopMusic() {
        synchronized(musicMutex) { GlobalManager.getInstance().songService?.stop() }
    }

    override fun playMusic(filename: String, previewTime: Int) {
        if (!Config.isPlayMusicPreview()) return
        Execution.async {
            synchronized(musicMutex) {
                GlobalManager.getInstance().songService?.stop()
                try {
                    GlobalManager.getInstance().songService!!.preLoadPreview(filename)
                    GlobalManager.getInstance().songService!!.play()
                    GlobalManager.getInstance().songService!!.setVolume(0f)
                    val previewSpeed = ModMenu.getInstance().speed
                    val enableNC = ModMenu.getInstance().isEnableNCWhenSpeedChange || ModMenu.getInstance().mod.contains(GameMod.MOD_NIGHTCORE)
                    GlobalManager.getInstance().songService!!.applySpeed(previewSpeed, enableNC)
                    if (previewTime >= 0) GlobalManager.getInstance().songService!!.seekTo(previewTime) else GlobalManager.getInstance().songService!!.seekTo(GlobalManager.getInstance().songService!!.length / 2)
                } catch (e: Exception) { Debug.e("LoadingMusic: ${e.message}", e) }
            }
        }
    }

    override fun isSelectAllowed(): Boolean = bgLoaded && secondsSinceLastSelect > 0.5f

    override fun showPropertiesMenu(item: MenuItem?) {
        var targetItem = item
        if (targetItem == null) { if (selectedItem == null) return; targetItem = selectedItem }
        PropsMenuFragment().show(this, targetItem!!)
    }

    fun showDeleteScoreMenu(scoreId: Int) {
        ScoreMenuFragment().show(scoreId)
    }

    fun reloadScoreBroad() {
        board?.init(selectedTrack)
    }

    fun select() {
        if (GlobalManager.getInstance().selectedTrack != null) {
            val beatmapInfo = GlobalManager.getInstance().selectedTrack!!.beatmap
            var i = items.size - 1
            while (i >= 0) {
                val item = items[i]
                if (item.beatmap == beatmapInfo) { secondsSinceLastSelect = 2f; item.select(false, true); break }
                --i
            }
        }
    }

    private fun tryReloadMenuItems(order: SortOrder) {
        when (order) {
            SortOrder.Title, SortOrder.Artist, SortOrder.Creator, SortOrder.Date, SortOrder.Bpm -> reloadMenuItems(GroupType.MapSet)
            SortOrder.Stars, SortOrder.Length -> reloadMenuItems(GroupType.SingleDiff)
        }
    }

    private fun reloadMenuItems(type: GroupType) {
        if (groupType != type) {
            groupType = type
            for (item in items) item.removeFromScene()
            items.clear()
            when (type) {
                GroupType.MapSet -> { for (i in LibraryManager.INSTANCE.getLibrary()) { val item = MenuItem(this, i); items.add(item); item.attachToScene(scene!!, backLayer) } }
                GroupType.SingleDiff -> { for (i in LibraryManager.INSTANCE.getLibrary()) { for (j in 0 until i.getCount()) { val item = MenuItem(this, i, j); items.add(item); item.attachToScene(scene!!, backLayer) } } }
            }
            val lowerFilter = filterMenu?.getFilter()?.lowercase() ?: ""
            val favsOnly = filterMenu?.isFavoritesOnly() ?: false
            val favFolder = filterMenu?.getFavoriteFolder()
            val limit = FavoriteLibrary.get().getMaps(favFolder ?: "")
            for (item in items) item.applyFilter(lowerFilter, favsOnly, limit)
        }
    }

    fun setStarsDisplay(star: Float) {
        val str = dimensionInfo?.text ?: return
        val strs = str.split("Stars: ")
        if (strs.size == 2) dimensionInfo?.setText(strs[0] + "Stars: " + star)
    }

    private fun reSelectItem(oldTrackFileName: String) {
        if (oldTrackFileName.isNotEmpty()) {
            if (selectedTrack?.filename == oldTrackFileName && items.size > 1 && selectedItem != null && selectedItem!!.isVisible()) {
                velocityY = 0f
                var height = 0f
                for (i in items.indices) { if (items[i] === selectedItem) break; height += items[i].getInitialHeight() }
                camY = height - Config.getRES_HEIGHT() / 2f
                camY += items[0].getTotalHeight() / 2
                return
            }
            for (item in items) {
                if (item == null || !item.isVisible()) continue
                val trackid = item.tryGetCorrespondingTrackId(oldTrackFileName)
                if (trackid >= 0) {
                    item.select(true, true)
                    if (trackid != 0) {
                        val track = item.getTrackSpritesById(trackid)
                        if (track != null) item.selectTrack(track, false)
                    }
                    break
                }
            }
        }
    }

    private fun updateScoringSwitcherStatus(forceUpdate: Boolean) {
        if (scoringSwitcher == null) return
        if (selectedTrack == null || board?.isShowOnlineScores != true) { scoringSwitcher?.setFrame(0); return }
        val md5 = selectedTrack!!.getMD5()
        if (!forceUpdate && mapStatuses.containsKey(md5)) {
            scoringSwitcher?.setFrame(when (mapStatuses[md5]) {
                RankedStatus.ranked -> 2; RankedStatus.approved -> 3; RankedStatus.loved -> 4; else -> 5
            })
            return
        }
        scoringSwitcher?.setFrame(1)
        Execution.async {
            try {
                val status = OnlineManager.getInstance().getBeatmapStatus(md5!!)
                if (board?.isShowOnlineScores != true || status == null || scoringSwitcher == null || selectedTrack == null || selectedTrack!!.getMD5() != md5) return@async
                mapStatuses.put(md5, status)
                scoringSwitcher?.setFrame(when (status) { RankedStatus.ranked -> 2; RankedStatus.approved -> 3; RankedStatus.loved -> 4; else -> 5 })
            } catch (e: OnlineManagerException) {
                Debug.e("Cannot get beatmap status: ${e.message}", e)
                scoringSwitcher?.setFrame(1)
            }
        }
    }

    private fun openEditor() {
        if (selectedTrack == null) return
        ResourceManager.getInstance().getSound("menuhit").play()
        stopMusic()
        Execution.async {
            val beatmapData = BeatmapParser(selectedTrack!!.filename!!).parse(true)
            if (beatmapData == null) { ToastLogger.showText("Failed to parse beatmap for editing", true); return@async }
            Execution.updateThread {
                val manager = GlobalManager.getInstance()
                val editorScene = EditorScene(manager.engine!!)
                manager.editorScene = editorScene
                editorScene.loadBeatmap(beatmapData, beatmapData.folder ?: "")
                editorScene.show()
            }
        }
    }

    private fun createNewBeatmap() {
        ResourceManager.getInstance().getSound("menuhit").play()
        stopMusic()
        Execution.async {
            val beatmapData = BeatmapData()
            beatmapData.metadata.artist = "Unknown"
            beatmapData.metadata.title = "New Beatmap"
            beatmapData.metadata.creator = "Unknown"
            Execution.updateThread {
                val manager = GlobalManager.getInstance()
                val editorScene = EditorScene(manager.engine!!)
                manager.editorScene = editorScene
                editorScene.loadBeatmap(beatmapData, beatmapData.folder ?: "")
                editorScene.show()
            }
        }
    }

    @JvmField
    var sortOrder: SortOrder = SortOrder.Title

    enum class SortOrder { Title, Artist, Creator, Date, Bpm, Stars, Length }
    enum class GroupType { MapSet, SingleDiff }

    companion object {
        private val musicMutex = Any()
        private val bgMutex = Any()
        private val backgroundMutex = Any()

        @JvmStatic
        fun stopMusicStatic() {
            synchronized(musicMutex) { GlobalManager.getInstance().songService?.stop() }
        }
    }
}
