package ru.nsu.ccfit.zuev.osu.scoring

import com.edlplan.framework.utils.functionality.SmartIterator
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import com.reco1l.legacy.ui.entity.StatisticSelector
import com.rian.difficultycalculator.attributes.DifficultyAttributes
import com.rian.difficultycalculator.attributes.PerformanceAttributes
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.entity.modifier.FadeInModifier
import org.anddev.andengine.entity.modifier.ParallelEntityModifier
import org.anddev.andengine.entity.modifier.ScaleModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osu.menu.SongMenu
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.SendingPanel
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoringScene(private val engine: Engine, private val game: GameScene, private val menu: SongMenu) {
    internal var scene: Scene? = null
    private var songService: SongService? = null
    private var replayStat: StatisticV2? = null
    private var replayID = -1
    @JvmField
    var track: TrackInfo? = null

    @JvmField
    var currentStatistic: StatisticV2? = null

    private var selector: StatisticSelector? = null

    fun load(
        stat: StatisticV2, track: TrackInfo?,
        player: SongService?, replay: String?, mapMD5: String?,
        trackToReplay: TrackInfo?
    ) {
        scene = Scene()
        songService = player
        currentStatistic = stat
        if (replay != null && track == null) {
            replayStat = stat
        }
        var tex = ResourceManager.getInstance().getTextureIfLoaded("::background")
        if (tex == null) {
            tex = ResourceManager.getInstance().getTexture("menu-background")
        }
        var height = tex!!.height.toFloat()
        height *= Config.getRES_WIDTH() / tex.width.toFloat()
        val bg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH().toFloat(), height, tex)
        scene!!.setBackground(SpriteBackground(bg))

        val bgTopRect = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Utils.toRes(100f))
        bgTopRect.setColor(0f, 0f, 0f, 0.8f)
        scene!!.attachChild(bgTopRect)

        var trackInfo = trackToReplay
        if (trackToReplay == null && track != null) {
            trackInfo = track
        }
        this.track = trackInfo
        val x = 0f
        val y = 100f
        val panelr = ResourceManager.getInstance().getTexture("ranking-panel")
        val panel = Sprite(x, y, Utils.toRes(panelr!!.width * 0.9f), Utils.toRes(panelr.height * 0.9f), panelr)
        scene!!.attachChild(panel)

        val hit300sr = ResourceManager.getInstance().getTexture("hit300")
        val hit300s = Sprite(Utils.toRes(10f), Utils.toRes(130f), Utils.toRes(hit300sr!!.width.toFloat()), Utils.toRes(hit300sr.height.toFloat()), hit300sr)
        hit300s.setPosition(Utils.toRes(70f - hit300s.width / 2 + x), Utils.toRes(130f - hit300s.height / 2 + y))
        scene!!.attachChild(hit300s)

        val hit100sr = ResourceManager.getInstance().getTexture("hit100")
        val hit100s = Sprite(Utils.toRes(10f), Utils.toRes(130f + 92), Utils.toRes(hit100sr!!.width.toFloat()), Utils.toRes(hit100sr.height.toFloat()), hit100sr)
        hit100s.setPosition(Utils.toRes(70f - hit100s.width / 2 + x), Utils.toRes(130f + 92 - hit100s.height / 2 + y))
        scene!!.attachChild(hit100s)

        val hit50sr = ResourceManager.getInstance().getTexture("hit50")
        val hit50s = Sprite(0f, Utils.toRes(120f + 92 * 2), Utils.toRes(hit50sr!!.width.toFloat()), Utils.toRes(hit50sr.height.toFloat()), hit50sr)
        hit50s.setPosition(Utils.toRes(70f - hit50s.width / 2 + x), Utils.toRes(130f + 92 * 2 - hit50s.height / 2 + y))
        scene!!.attachChild(hit50s)

        val hit300ksr = ResourceManager.getInstance().getTexture("hit300g")
        val hit300ks = Sprite(Utils.toRes(300f), Utils.toRes(100f), Utils.toRes(hit300ksr!!.width.toFloat()), Utils.toRes(hit300ksr.height.toFloat()), hit300ksr)
        hit300ks.setPosition(Utils.toRes(340f - hit300ks.width / 2 + x), Utils.toRes(130f - hit300ks.height / 2 + y))
        scene!!.attachChild(hit300ks)

        val hit100ksr = ResourceManager.getInstance().getTexture("hit100k")
        val hit100ks = Sprite(Utils.toRes(300f), Utils.toRes(120f + 92), Utils.toRes(hit100ksr!!.width.toFloat()), Utils.toRes(hit100ksr.height.toFloat()), hit100ksr)
        hit100ks.setPosition(Utils.toRes(340f - hit100ks.width / 2 + x), Utils.toRes(130f + 92 - hit100ks.height / 2 + y))
        scene!!.attachChild(hit100ks)

        val hit0sr = ResourceManager.getInstance().getTexture("hit0")
        val hit0s = Sprite(Utils.toRes(300f), Utils.toRes(120f + 92 * 2), Utils.toRes(hit0sr!!.width.toFloat()), Utils.toRes(hit0sr.height.toFloat()), hit0sr)
        hit0s.setPosition(Utils.toRes(340f - hit0s.width / 2 + x), Utils.toRes(130f + 92 * 2 - hit0s.height / 2 + y))
        scene!!.attachChild(hit0s)

        val rankingText = Sprite(Utils.toRes(580f), 0f, ResourceManager.getInstance().getTexture("ranking-title"))
        rankingText.setPosition(Config.getRES_WIDTH() * 5 / 6 - rankingText.width / 2, 0f)
        scene!!.attachChild(rankingText)

        var scoreStr = stat.totalScoreWithMultiplier.toString()
        while (scoreStr.length < 8) {
            scoreStr = "0$scoreStr"
        }
        val scoreNum = ScoreNumber(Utils.toRes(220f + x), Utils.toRes(18f + y), scoreStr, 1f, false)
        scoreNum.attachToScene(scene!!)

        val hit300num = ScoreNumber(Utils.toRes(138f + x), Utils.toRes(110f + y), "${stat.hit300}x", 1f, false)
        hit300num.attachToScene(scene!!)
        val hit100num = ScoreNumber(Utils.toRes(138f + x), Utils.toRes(110f + 85 + y), "${stat.hit100}x", 1f, false)
        hit100num.attachToScene(scene!!)
        val hit50num = ScoreNumber(Utils.toRes(138f + x), Utils.toRes(110f + 85 * 2 + y), "${stat.hit50}x", 1f, false)
        hit50num.attachToScene(scene!!)

        val hit300knum = ScoreNumber(Utils.toRes(400f + x), Utils.toRes(110f + y), "${stat.hit300k}x", 1f, false)
        hit300knum.attachToScene(scene!!)
        val hit100knum = ScoreNumber(Utils.toRes(400f + x), Utils.toRes(110f + 85 + y), "${stat.hit100k}x", 1f, false)
        hit100knum.attachToScene(scene!!)
        val hit0num = ScoreNumber(Utils.toRes(400f + x), Utils.toRes(110f + 85 * 2 + y), "${stat.misses}x", 1f, false)
        hit0num.attachToScene(scene!!)

        val maxComboText = Sprite(Utils.toRes(20f + x), Utils.toRes(332f + y), ResourceManager.getInstance().getTexture("ranking-maxcombo"))
        scene!!.attachChild(maxComboText)
        val accText = Sprite(Utils.toRes(260f + x), Utils.toRes(332f + y), ResourceManager.getInstance().getTexture("ranking-accuracy"))
        scene!!.attachChild(accText)
        val maxCombo = ScoreNumber(Utils.toRes(20f + x), Utils.toRes(maxComboText.y + 38), "${stat.maxCombo}x", 1f, false)
        maxCombo.attachToScene(scene!!)
        val accStr = String.format(Locale.ENGLISH, "%2.2f%%", stat.getAccuracy() * 100)
        val accuracy = ScoreNumber(Utils.toRes(260f + x), Utils.toRes(accText.y + 38), accStr, 1f, false)
        accuracy.attachToScene(scene!!)

        val mark = Sprite(Utils.toRes(610f), 0f, ResourceManager.getInstance().getTexture("ranking-${stat.getMark()}"))
        if (track != null) {
            mark.alpha = 0f
            mark.setScale(1.5f)
            mark.registerEntityModifier(ParallelEntityModifier(FadeInModifier(2f), ScaleModifier(2f, 2f, 1f)))
        }
        mark.setPosition(Config.getRES_WIDTH() * 5 / 6 - mark.width / 2, 80f)

        val backBtn = object : Sprite(Utils.toRes(580f), Utils.toRes(490f), ResourceManager.getInstance().getTexture("ranking-back")) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    ResourceManager.getInstance().getSound("menuback").play()
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    back()
                    return true
                }
                return false
            }
        }
        backBtn.setPosition(Config.getRES_WIDTH() - backBtn.width - 10, Config.getRES_HEIGHT() - backBtn.height - 10)
        scene!!.attachChild(backBtn)

        var retryBtn: Sprite? = null

        if (!Multiplayer.isMultiplayer) {
            retryBtn = object : Sprite(Utils.toRes(580f), Utils.toRes(400f), ResourceManager.getInstance().getTexture("ranking-retry")) {
                override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                    if (pSceneTouchEvent.isActionDown) {
                        setColor(0.7f, 0.7f, 0.7f)
                        ResourceManager.getInstance().getSound("menuback").play()
                        return true
                    }
                    if (pSceneTouchEvent.isActionUp) {
                        ResourceManager.getInstance().getSound("applause").stop()
                        engine.setScene(menu.scene)
                        game.startGame(null, null)
                        scene = null
                        stopMusic()
                        return true
                    }
                    return false
                }
            }
        }

        var replayBtn: Sprite? = null

        if (!Multiplayer.isMultiplayer) {
            replayBtn = object : Sprite(Utils.toRes(580f), Utils.toRes(400f), ResourceManager.getInstance().getTexture("ranking-replay")) {
                override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                    if (pSceneTouchEvent.isActionDown) {
                        setColor(0.7f, 0.7f, 0.7f)
                        ResourceManager.getInstance().getSound("menuback").play()
                        return true
                    }
                    if (pSceneTouchEvent.isActionUp) {
                        ResourceManager.getInstance().getSound("applause").stop()
                        SongMenu.stopMusicStatic()
                        engine.setScene(menu.scene)

                        Replay.oldMod = ModMenu.getInstance().getMod()
                        Replay.oldChangeSpeed = ModMenu.getInstance().getChangeSpeed()

                        Replay.oldCustomAR = ModMenu.getInstance().getCustomAR()
                        Replay.oldCustomOD = ModMenu.getInstance().getCustomOD()
                        Replay.oldCustomCS = ModMenu.getInstance().getCustomCS()
                        Replay.oldCustomHP = ModMenu.getInstance().getCustomHP()

                        Replay.oldFLFollowDelay = ModMenu.getInstance().FLfollowDelay

                        ModMenu.getInstance().setMod(stat.mod)
                        ModMenu.getInstance().setChangeSpeed(stat.changeSpeed)
                        ModMenu.getInstance().FLfollowDelay = stat.flFollowDelay

                        ModMenu.getInstance().setCustomAR(stat.customAR)
                        ModMenu.getInstance().setCustomOD(stat.customOD)
                        ModMenu.getInstance().setCustomCS(stat.customCS)
                        ModMenu.getInstance().setCustomHP(stat.customHP)

                        game.startGame(trackToReplay, replay)

                        scene = null
                        stopMusic()
                        return true
                    }
                    return false
                }
            }
        }

        if (stat.getAccuracy() == 1f || stat.maxCombo == this.track!!.maxCombo || stat.isPerfect) {
            val perfect = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("ranking-perfect"))
            perfect.setPosition(0f, accuracy.y + accuracy.height + 10)
            scene!!.attachChild(perfect)
        }
        if (track != null && retryBtn != null) {
            retryBtn.setPosition(Config.getRES_WIDTH() - backBtn.width - 10, backBtn.y - retryBtn.height - 10)
            scene!!.attachChild(retryBtn)
        } else if (replay != null && replayBtn != null) {
            replayBtn.setPosition(Config.getRES_WIDTH() - backBtn.width - 10, backBtn.y - replayBtn.height - 10)
            scene!!.attachChild(replayBtn)
        }

        scene!!.setTouchAreaBindingEnabled(true)
        if (track != null && retryBtn != null) {
            scene!!.registerTouchArea(retryBtn)
        } else if (replay != null && replayBtn != null) {
            scene!!.registerTouchArea(replayBtn)
        }
        scene!!.registerTouchArea(backBtn)
        scene!!.attachChild(mark)

        var modX = mark.x - 30
        val modY = mark.y + mark.height * 2 / 3
        if (stat.mod.contains(GameMod.MOD_SCOREV2)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-scorev2"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }
        if (stat.mod.contains(GameMod.MOD_HARDROCK)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-hardrock"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_EASY)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-easy"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }

        if (stat.mod.contains(GameMod.MOD_HIDDEN)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-hidden"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }

        if (stat.mod.contains(GameMod.MOD_FLASHLIGHT)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-flashlight"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }
        if (stat.mod.contains(GameMod.MOD_NOFAIL)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-nofail"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_SUDDENDEATH)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-suddendeath"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_PERFECT)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-perfect"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }
        if (stat.mod.contains(GameMod.MOD_AUTO)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-autoplay"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_AUTOPILOT)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-relax2"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_RELAX)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-relax"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }
        if (stat.mod.contains(GameMod.MOD_DOUBLETIME)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-doubletime"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_NIGHTCORE)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-nightcore"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        } else if (stat.mod.contains(GameMod.MOD_HALFTIME)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-halftime"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }

        if (stat.mod.contains(GameMod.MOD_PRECISE)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-precise"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }
        if (stat.mod.contains(GameMod.MOD_REALLYEASY)) {
            val modSprite = Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-reallyeasy"))
            modX -= Utils.toRes(30f)
            scene!!.attachChild(modSprite)
        }

        val infoStr = (if (trackInfo?.beatmap?.artistUnicode == null || Config.isForceRomanized()) trackInfo?.beatmap?.artist else trackInfo?.beatmap?.artistUnicode) + " - " +
                (if (trackInfo?.beatmap?.titleUnicode == null || Config.isForceRomanized()) trackInfo?.beatmap?.title else trackInfo?.beatmap?.titleUnicode) + " [" + trackInfo?.mode + "]"
        val mapperStr = StringBuilder("Beatmap by ${trackInfo?.creator}")
        var playerStr = "Played by ${stat.playerName} on " +
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(stat.time))
        playerStr += if (BuildConfig.BUILD_TYPE == "release")
            String.format(" %s", BuildConfig.VERSION_NAME)
        else
            String.format(" %s(%s)", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE)
        if (stat.changeSpeed != 1f ||
            stat.isCustomAR() ||
            stat.isCustomOD() ||
            stat.isCustomCS() ||
            stat.isCustomHP() ||
            stat.flFollowDelay != FlashLightEntity.defaultMoveDelayS &&
            stat.mod.contains(GameMod.MOD_FLASHLIGHT)
        ) {

            mapperStr.append(" [")
            if (stat.changeSpeed != 1f) {
                mapperStr.append(String.format(Locale.ENGLISH, "%.2fx,", stat.changeSpeed))
            }
            if (stat.isCustomAR()) {
                mapperStr.append(String.format(Locale.ENGLISH, "AR%.1f,", stat.customAR))
            }
            if (stat.isCustomOD()) {
                mapperStr.append(String.format(Locale.ENGLISH, "OD%.1f,", stat.customOD))
            }
            if (stat.isCustomCS()) {
                mapperStr.append(String.format(Locale.ENGLISH, "CS%.1f,", stat.customCS))
            }
            if (stat.isCustomHP()) {
                mapperStr.append(String.format(Locale.ENGLISH, "HP%.1f,", stat.customHP))
            }
            if (stat.flFollowDelay != FlashLightEntity.defaultMoveDelayS && stat.mod.contains(GameMod.MOD_FLASHLIGHT)) {
                mapperStr.append(String.format(Locale.ENGLISH, "FLD%.2f,", stat.flFollowDelay))
            }
            if (mapperStr.endsWith(",")) {
                mapperStr.deleteCharAt(mapperStr.length - 1)
            }
            mapperStr.append("]")
        }
        Debug.i("playedtime ${stat.time}")
        val beatmapInfo = Text(Utils.toRes(4f), Utils.toRes(2f), ResourceManager.getInstance().getFont("font"), infoStr)
        val mapperInfo = Text(Utils.toRes(4f), beatmapInfo.y + beatmapInfo.height + Utils.toRes(2f), ResourceManager.getInstance().getFont("smallFont"), mapperStr.toString())
        val playerInfo = Text(Utils.toRes(4f), mapperInfo.y + mapperInfo.height + Utils.toRes(2f), ResourceManager.getInstance().getFont("smallFont"), playerStr)
        if (Config.isDisplayScoreStatistics()) {
            val ppinfo = StringBuilder()
            val beatmapData: BeatmapData? = BeatmapParser(this.track!!.filename!!).setCalculator(true).parse(true)

            if (beatmapData != null) {
                val difficultyAttributes: DifficultyAttributes = BeatmapDifficultyCalculator.calculateDifficulty(beatmapData, stat)
                val performanceAttributes: PerformanceAttributes = BeatmapDifficultyCalculator.calculatePerformance(difficultyAttributes, stat)
                val maxPerformanceAttributes: PerformanceAttributes = BeatmapDifficultyCalculator.calculatePerformance(difficultyAttributes)
                ppinfo.append(String.format(Locale.ENGLISH, "%.2f★ | %.2f/%.2fpp", difficultyAttributes.starRating, performanceAttributes.total, maxPerformanceAttributes.total))
            }
            if (stat.unstableRate > 0) {
                if (beatmapData != null) {
                    ppinfo.append("\n")
                }
                ppinfo.append(String.format(Locale.ENGLISH, "Error: %.2fms - %.2fms avg", stat.negativeHitError, stat.positiveHitError))
                ppinfo.append("\n")
                ppinfo.append(String.format(Locale.ENGLISH, "Unstable Rate: %.2f", stat.unstableRate))
            }
            val ppInfo = Text(Utils.toRes(4f), Config.getRES_HEIGHT() - playerInfo.height - Utils.toRes(2f), ResourceManager.getInstance().getFont("smallFont"), ppinfo.toString())
            ppInfo.setPosition(Utils.toRes(244f), Config.getRES_HEIGHT() - ppInfo.height - Utils.toRes(2f))
            val statisticRectangle = Rectangle(Utils.toRes(240f), Config.getRES_HEIGHT() - ppInfo.height - Utils.toRes(4f), ppInfo.width + Utils.toRes(12f), ppInfo.height + Utils.toRes(4f))
            statisticRectangle.setColor(0f, 0f, 0f, 0.5f)
            scene!!.attachChild(statisticRectangle)
            scene!!.attachChild(ppInfo)
        }
        scene!!.attachChild(beatmapInfo)
        scene!!.attachChild(mapperInfo)
        scene!!.attachChild(playerInfo)

        if (Multiplayer.isMultiplayer) {
            updateLeaderboard()
        }

        if (track != null && track.md5 != null && track.md5 == mapMD5) {
            ResourceManager.getInstance().getSound("applause").play()
            if (!Multiplayer.isMultiplayer || !GlobalManager.getInstance().gameScene!!.hasFailed) {
                ScoreLibrary.getInstance().addScore(track.filename ?: "", stat, replay)
            }

            if (stat.totalScoreWithMultiplier > 0 && OnlineManager.getInstance().isStayOnline &&
                OnlineManager.getInstance().isReadyToSend
            ) {

                if (GlobalManager.getInstance().gameScene!!.hasFailed ||
                    (Multiplayer.isMultiplayer && !Config.isSubmitScoreOnMultiplayer())
                )
                    return

                val hasUnrankedMod = SmartIterator.wrap(stat.mod.iterator()).applyFilter { m -> m.unranked }.hasNext()
                if (hasUnrankedMod
                    || ModMenu.getInstance().isCustomAR()
                    || ModMenu.getInstance().isCustomOD()
                    || ModMenu.getInstance().isCustomCS()
                    || ModMenu.getInstance().isCustomHP()
                    || !ModMenu.getInstance().isDefaultFLFollowDelay()
                ) {
                    return
                }

                val sendingPanel = SendingPanel(
                    OnlineManager.getInstance().rank,
                    OnlineManager.getInstance().score,
                    OnlineManager.getInstance().accuracy
                )
                sendingPanel.setPosition((Config.getRES_WIDTH() / 2 - 400).toFloat(), Utils.toRes(-300f))
                scene!!.registerTouchArea(sendingPanel.getDismissTouchArea())
                scene!!.attachChild(sendingPanel)
                ScoreLibrary.getInstance().sendScoreOnline(stat, replay ?: "", sendingPanel)
            }
        }
    }

    fun updateLeaderboard() {
        if (Multiplayer.finalData != null) {
            if (selector != null) {
                val oldSelector = selector!!
                Execution.updateThread {
                    oldSelector.detachSelf()
                    oldSelector.detachChildren()
                    if (scene != null)
                        scene!!.unregisterTouchArea(oldSelector)
                }
            }

            selector = StatisticSelector(Multiplayer.finalData)

            if (scene != null) {
                scene!!.attachChild(selector)
                scene!!.registerTouchArea(selector)
            }
        }
    }

    fun back() {
        ResourceManager.getInstance().getSound("applause").stop()
        Multiplayer.finalData = null
        currentStatistic = null

        if (Multiplayer.isMultiplayer) {
            if (!Multiplayer.isConnected)
                RoomScene.back()
            else
                RoomScene.show()
            return
        }
        replayMusic()
        GlobalManager.getInstance().songMenu?.show()
        GlobalManager.getInstance().songMenu?.updateScore()
        setReplayID(-1)
    }

    fun getScene(): Scene? = scene

    fun stopMusic() {
        songService?.stop()
    }

    fun replayMusic() {
        songService?.let {
            it.stop()
            it.preLoadPreview(track!!.beatmap?.getMusic() ?: "")
            it.play()
        }
    }

    fun getReplayStat(): StatisticV2? = replayStat

    fun setReplayStat(replayStat: StatisticV2?) {
        this.replayStat = replayStat
    }

    fun getReplayID(): Int = replayID

    fun setReplayID(id: Int) {
        this.replayID = id
    }
}
