package ru.nsu.ccfit.zuev.osu.menu

import com.edlplan.ui.fragment.InGameSettingMenu
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.framework.lang.Execution
import com.reco1l.legacy.data.modsToString
import com.reco1l.legacy.Multiplayer
import com.reco1l.api.ibancho.data.RoomMods
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.entity.modifier.AlphaModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.IModSwitcher
import ru.nsu.ccfit.zuev.osu.game.mods.ModButton
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.*

class ModMenu private constructor() : IModSwitcher {

    internal var scene: Scene? = null
    private var parent: Scene? = null
    private var bg: Rectangle? = null
    internal var mod: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)
    private var multiplierText: ChangeableText? = null
    private var selectedTrack: TrackInfo? = null
    private val modButtons: MutableMap<GameMod, ModButton> = TreeMap()
    internal var changeSpeed = 1.0f
    private var enableNCWhenSpeedChange = false
    private var modsRemoved = false
    var FLfollowDelay = DEFAULT_FL_FOLLOW_DELAY
    internal var customAR: Float? = null
    internal var customOD: Float? = null
    internal var customHP: Float? = null
    internal var customCS: Float? = null
    private var menu: InGameSettingMenu? = null

    fun reload() {
        mod = EnumSet.noneOf(GameMod::class.java)
        init()
    }

    fun show(scene: Scene, selectedTrack: TrackInfo?) {
        parent = scene
        setSelectedTrack(selectedTrack)
        scene.setChildScene(getScene(), false, true, true)
        if (menu == null) {
            menu = InGameSettingMenu()
        }
        bg?.let {
            it.setAlpha(0f)
            it.registerEntityModifier(AlphaModifier(0.25f, 0f, 0.7f))
        }
        Execution.mainThread { menu?.show() }
        update()
    }

    fun update() {
        synchronized(modButtons) {
            for (key in modButtons.keys) {
                val button = modButtons[key]
                button?.setModEnabled(mod.contains(key))
            }
            changeMultiplierText()
        }
    }

    fun setMods(mods: RoomMods, isFreeMods: Boolean, allowForceDifficultyStatistics: Boolean) {
        val modSet = mods.set
        if (!isFreeMods) {
            mod = modSet.clone()
            FLfollowDelay = mods.flFollowDelay
        }
        if (!isFreeMods || !allowForceDifficultyStatistics) {
            customAR = mods.customAR
            customOD = mods.customOD
            customCS = mods.customCS
            customHP = mods.customHP
        }
        changeSpeed = mods.speedMultiplier
        if (!Multiplayer.isRoomHost) {
            if (modSet.contains(GameMod.MOD_DOUBLETIME) || modSet.contains(GameMod.MOD_NIGHTCORE)) {
                mod.remove(if (Config.isUseNightcoreOnMultiplayer()) GameMod.MOD_DOUBLETIME else GameMod.MOD_NIGHTCORE)
                mod.add(if (Config.isUseNightcoreOnMultiplayer()) GameMod.MOD_NIGHTCORE else GameMod.MOD_DOUBLETIME)
            } else {
                mod.remove(GameMod.MOD_NIGHTCORE)
                mod.remove(GameMod.MOD_DOUBLETIME)
            }
        }
        if (modSet.contains(GameMod.MOD_SCOREV2)) mod.add(GameMod.MOD_SCOREV2) else mod.remove(GameMod.MOD_SCOREV2)
        if (modSet.contains(GameMod.MOD_HALFTIME)) mod.add(GameMod.MOD_HALFTIME) else mod.remove(GameMod.MOD_HALFTIME)
        update()
    }

    fun onBackPress() {
        if (menu?.tryDismissSettingPanel() == true) return
        hide()
    }

    fun hide() = hide(true)

    fun hide(updatePlayerMods: Boolean) {
        parent?.let {
            it.clearChildScene()
            parent = null
        }
        menu?.dismiss()
        if (Multiplayer.isConnected) {
            RoomScene.awaitModsChange = true
            val string = modsToString(mod)
            if (Multiplayer.isRoomHost) {
                RoomAPI.setRoomMods(string, changeSpeed, FLfollowDelay, customAR, customOD, customCS, customHP)
            } else if (updatePlayerMods) {
                RoomAPI.setPlayerMods(string, changeSpeed, FLfollowDelay, customAR, customOD, customCS, customHP)
            } else {
                RoomScene.awaitModsChange = false
            }
        }
    }

    fun hideByFrag() {
        parent?.let {
            it.clearChildScene()
            parent = null
        }
    }

    private fun addButton(x: Float, y: Float, texture: String, mod: GameMod) {
        val mButton = ModButton(x, y, texture, mod)
        mButton.setModEnabled(this.mod.contains(mod))
        mButton.setSwitcher(this)
        scene!!.attachChild(mButton)
        scene!!.registerTouchArea(mButton)
        modButtons[mod] = mButton
    }

    fun init() {
        modButtons.clear()
        scene = Scene()
        scene!!.setBackgroundEnabled(false)
        bg = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        bg!!.setColor(0.05f, 0.06f, 0.12f, 0.82f)
        scene!!.attachChild(bg)
        val headerH = Utils.toRes(90).toFloat()
        val headerPanel = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), headerH)
        headerPanel.setColor(0.08f, 0.08f, 0.18f, 0.95f)
        scene!!.attachChild(headerPanel)
        val headerAccent = Rectangle(0f, headerH - Utils.toRes(3), Config.getRES_WIDTH().toFloat(), Utils.toRes(3).toFloat())
        headerAccent.setColor(0.90f, 0.24f, 0.55f, 1.0f)
        scene!!.attachChild(headerAccent)
        multiplierText = ChangeableText(0f, Utils.toRes(28).toFloat(),
            ResourceManager.getInstance().getFont("CaptionFont"),
            StringTable.format(R.string.menu_mod_multiplier, 1f))
        multiplierText!!.setScale(1.2f)
        scene!!.attachChild(multiplierText)
        menu = InGameSettingMenu()
        changeMultiplierText()
        val offset = 100f
        val offsetGrowth = 130f
        val button: TextureRegion = ResourceManager.getInstance().getTexture("selection-mod-easy")!!
        addButton(offset.toFloat(), (Config.getRES_HEIGHT() / 2 - button.height * 3).toInt().toFloat(), "selection-mod-easy", GameMod.MOD_EASY)
        var factor = 1f
        addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height * 3).toInt().toFloat(), "selection-mod-nofail", GameMod.MOD_NOFAIL)
        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost) addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height * 3).toInt().toFloat(), "selection-mod-halftime", GameMod.MOD_HALFTIME)
        factor = 1f
        addButton(offset, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-hardrock", GameMod.MOD_HARDROCK)
        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost) addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-doubletime", GameMod.MOD_DOUBLETIME)
        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost) addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-nightcore", GameMod.MOD_NIGHTCORE)
        addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-hidden", GameMod.MOD_HIDDEN)
        addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-flashlight", GameMod.MOD_FLASHLIGHT)
        addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-suddendeath", GameMod.MOD_SUDDENDEATH)
        addButton(offset + offsetGrowth * factor, (Config.getRES_HEIGHT() / 2 - button.height / 2).toInt().toFloat(), "selection-mod-perfect", GameMod.MOD_PERFECT)
        factor = 1f
        addButton(offset, (Config.getRES_HEIGHT() / 2 + button.height * 2).toInt().toFloat(), "selection-mod-relax", GameMod.MOD_RELAX)
        addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 + button.height * 2).toInt().toFloat(), "selection-mod-relax2", GameMod.MOD_AUTOPILOT)
        if (!Multiplayer.isMultiplayer) addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 + button.height * 2).toInt().toFloat(), "selection-mod-autoplay", GameMod.MOD_AUTO)
        if (!Multiplayer.isMultiplayer) addButton(offset + offsetGrowth * factor++, (Config.getRES_HEIGHT() / 2 + button.height * 2).toInt().toFloat(), "selection-mod-scorev2", GameMod.MOD_SCOREV2)
        val resetText = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"), StringTable.get(R.string.menu_mod_reset)) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionUp) {
                    mod.clear()
                    changeMultiplierText()
                    for (btn in modButtons.values) btn.setModEnabled(false)
                    return true
                }
                return false
            }
        }
        if (!Multiplayer.isMultiplayer) {
            scene!!.attachChild(resetText)
            scene!!.registerTouchArea(resetText)
        }
        resetText.setScale(1.2f)
        resetText.setColor(0.3f, 0.32f, 0.42f)
        resetText.setAlpha(1.0f)
        val back = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"), StringTable.get(R.string.menu_mod_back)) {
            override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
                if (pSceneTouchEvent.isActionUp) {
                    Thread {
                        if (GlobalManager.getInstance().songMenu?.selectedTrack != null) {
                            val beatmapData = BeatmapParser(GlobalManager.getInstance().songMenu!!.selectedTrack!!.filename!!).setCalculator(true).parse(true)
                            if (beatmapData == null) {
                                GlobalManager.getInstance().songMenu?.setStarsDisplay(0f)
                                return@Thread
                            }
                            val parameters = ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator.constructDifficultyParameters(StatisticV2()) ?: return@Thread
                            parameters.mods = getMod()
                            parameters.customSpeedMultiplier = changeSpeed
                            if (isCustomCS()) parameters.customCS = customCS!!
                            if (isCustomAR()) parameters.customAR = customAR!!
                            if (isCustomOD()) parameters.customOD = customOD!!
                            val attributes = BeatmapDifficultyCalculator.calculateDifficulty(beatmapData, parameters)
                            GlobalManager.getInstance().songMenu?.setStarsDisplay(GameHelper.Round(attributes.starRating, 2).toFloat())
                        }
                    }.start()
                    hide()
                    return true
                }
                return false
            }
        }
        back.setScale(1.2f)
        back.width = resetText.getWidth().toFloat()
        back.height = resetText.getHeight().toFloat()
        back.setPosition(Config.getRES_WIDTH() - back.getWidth() - 60, Config.getRES_HEIGHT() - back.getHeight() - 30)
        back.setColor(0.90f, 0.24f, 0.55f)
        back.setAlpha(1.0f)
        resetText.setPosition(Config.getRES_WIDTH() - resetText.getWidth() - 60, back.y - resetText.getHeight() - 20)
        scene!!.attachChild(back)
        scene!!.registerTouchArea(back)
        scene!!.setTouchAreaBindingEnabled(true)
    }

    fun getScene(): Scene {
        if (scene == null) init()
        return scene!!
    }

    fun getMod(): EnumSet<GameMod> = mod.clone()

    fun setMod(mod: EnumSet<GameMod>) {
        this.mod = mod.clone()
    }

    private fun changeMultiplierText() {
        GlobalManager.getInstance().songMenu?.changeDimensionInfo(selectedTrack)
        var mult = 1f
        for (m in mod) {
            mult *= m.scoreMultiplier
        }
        if (changeSpeed != 1.0f) {
            mult *= StatisticV2.getSpeedChangeScoreMultiplier(speed, mod)
        }
        if (selectedTrack != null) {
            if (isCustomCS()) mult *= StatisticV2.getCustomCSScoreMultiplier(selectedTrack!!.circleSize, customCS!!)
            if (isCustomOD()) mult *= StatisticV2.getCustomODScoreMultiplier(selectedTrack!!.overallDifficulty, customOD!!)
        }
        multiplierText?.setText(StringTable.format(R.string.menu_mod_multiplier, mult))
        multiplierText?.setPosition(Config.getRES_WIDTH() / 2f - multiplierText!!.getWidth() / 2, multiplierText!!.getY())
        when {
            mult == 1f -> multiplierText?.setColor(1f, 1f, 1f)
            mult < 1f -> multiplierText?.setColor(1f, 150f / 255f, 0f)
            else -> multiplierText?.setColor(5 / 255f, 240 / 255f, 5 / 255f)
        }
    }

    fun handleModFlags(flag: GameMod, modToCheck: GameMod, modsToRemove: Array<GameMod>) {
        if (flag == modToCheck) {
            for (modToRemove in modsToRemove) {
                mod.remove(modToRemove)
                modsRemoved = true
            }
        }
    }

    fun handleCustomDifficultyStatisticsFlags(): Boolean {
        if (!isCustomCS() || !isCustomAR() || !isCustomOD() || !isCustomHP()) return false
        val modsToRemove = arrayOf(GameMod.MOD_HARDROCK, GameMod.MOD_EASY, GameMod.MOD_REALLYEASY)
        var removed = false
        for (gameMod in modsToRemove) {
            if (mod.contains(gameMod)) {
                mod.remove(gameMod)
                modButtons[gameMod]?.setModEnabled(false)
                removed = true
            }
        }
        if (removed) ToastLogger.showTextId(R.string.force_diffstat_mod_unpickable, false)
        return removed
    }

    override fun switchMod(flag: GameMod): Boolean {
        var returnValue = true
        if (mod.contains(flag)) {
            mod.remove(flag)
            if (flag == GameMod.MOD_FLASHLIGHT) resetFLFollowDelay()
            returnValue = false
        } else {
            mod.add(flag)
            if (handleCustomDifficultyStatisticsFlags()) return false
            handleModFlags(flag, GameMod.MOD_HARDROCK, arrayOf(GameMod.MOD_EASY))
            handleModFlags(flag, GameMod.MOD_EASY, arrayOf(GameMod.MOD_HARDROCK))
            handleModFlags(flag, GameMod.MOD_AUTOPILOT, arrayOf(GameMod.MOD_RELAX, GameMod.MOD_AUTO, GameMod.MOD_NOFAIL))
            handleModFlags(flag, GameMod.MOD_AUTO, arrayOf(GameMod.MOD_RELAX, GameMod.MOD_AUTOPILOT, GameMod.MOD_PERFECT, GameMod.MOD_SUDDENDEATH))
            handleModFlags(flag, GameMod.MOD_RELAX, arrayOf(GameMod.MOD_AUTO, GameMod.MOD_NOFAIL, GameMod.MOD_AUTOPILOT))
            handleModFlags(flag, GameMod.MOD_DOUBLETIME, arrayOf(GameMod.MOD_NIGHTCORE, GameMod.MOD_HALFTIME))
            handleModFlags(flag, GameMod.MOD_NIGHTCORE, arrayOf(GameMod.MOD_DOUBLETIME, GameMod.MOD_HALFTIME))
            handleModFlags(flag, GameMod.MOD_HALFTIME, arrayOf(GameMod.MOD_DOUBLETIME, GameMod.MOD_NIGHTCORE))
            handleModFlags(flag, GameMod.MOD_SUDDENDEATH, arrayOf(GameMod.MOD_NOFAIL, GameMod.MOD_PERFECT, GameMod.MOD_AUTO))
            handleModFlags(flag, GameMod.MOD_PERFECT, arrayOf(GameMod.MOD_NOFAIL, GameMod.MOD_SUDDENDEATH, GameMod.MOD_AUTO))
            handleModFlags(flag, GameMod.MOD_NOFAIL, arrayOf(GameMod.MOD_PERFECT, GameMod.MOD_SUDDENDEATH, GameMod.MOD_AUTOPILOT, GameMod.MOD_RELAX))
            if (modsRemoved) {
                for (gameMod in modButtons.keys) {
                    modButtons[gameMod]!!.setModEnabled(mod.contains(gameMod))
                }
            }
        }
        changeMultiplierText()
        menu?.updatePreviewSpeed()
        return returnValue
    }

    fun setSelectedTrack(selectedTrack: TrackInfo?) {
        this.selectedTrack = selectedTrack
        if (selectedTrack != null) changeMultiplierText()
    }

    val speed: Float
        get() {
            var speed = changeSpeed
            if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)) {
                speed *= 1.5f
            } else if (mod.contains(GameMod.MOD_HALFTIME)) {
                speed *= 0.75f
            }
            return speed
        }

    fun isChangeSpeed(): Boolean = changeSpeed != 1.0f

    fun getChangeSpeed(): Float = changeSpeed

    fun setChangeSpeed(speed: Float) {
        changeSpeed = speed
    }

    fun isDefaultFLFollowDelay(): Boolean = FLfollowDelay == DEFAULT_FL_FOLLOW_DELAY

    fun resetFLFollowDelay() {
        FLfollowDelay = DEFAULT_FL_FOLLOW_DELAY
    }

    var isEnableNCWhenSpeedChange: Boolean
        get() = enableNCWhenSpeedChange
        set(value) { enableNCWhenSpeedChange = value }

    fun updateMultiplierText() {
        changeMultiplierText()
    }

    fun isCustomAR(): Boolean = customAR != null
    fun getCustomAR(): Float? = customAR
    fun setCustomAR(customAR: Float?) {
        this.customAR = customAR
        handleCustomDifficultyStatisticsFlags()
    }

    fun isCustomOD(): Boolean = customOD != null
    fun getCustomOD(): Float? = customOD
    fun setCustomOD(customOD: Float?) {
        this.customOD = customOD
        handleCustomDifficultyStatisticsFlags()
    }

    fun isCustomHP(): Boolean = customHP != null
    fun getCustomHP(): Float? = customHP
    fun setCustomHP(customHP: Float?) {
        this.customHP = customHP
        handleCustomDifficultyStatisticsFlags()
    }

    fun isCustomCS(): Boolean = customCS != null
    fun getCustomCS(): Float? = customCS
    fun setCustomCS(customCS: Float?) {
        this.customCS = customCS
        handleCustomDifficultyStatisticsFlags()
    }

    companion object {
        const val DEFAULT_FL_FOLLOW_DELAY = 0.12f
        private val instance = ModMenu()

        @JvmStatic
        fun getInstance(): ModMenu = instance
    }
}
