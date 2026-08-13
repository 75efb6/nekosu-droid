package ru.nsu.ccfit.zuev.osu

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.preference.PreferenceManager
import com.edlplan.favorite.FavoriteLibrary
import com.edlplan.framework.math.FMath
import com.google.firebase.messaging.FirebaseMessaging
import com.reco1l.legacy.Multiplayer
import net.margaritov.preference.colorpicker.ColorPickerPreference
import org.anddev.andengine.util.Debug
import java.io.File
import java.util.HashMap
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.game.GameHelper

object Config {
    private var corePath: String? = null
    private var defaultCorePath: String? = null
    private var beatmapPath: String? = null
    private var cachePath: String? = null
    private var skinPath: String? = null
    private var skinTopPath: String? = null
    private var scorePath: String? = null
    private var localUsername: String? = null
    private var onlineUsername: String? = null
    private var onlinePassword: String? = null
    private var onlineDeviceID: String? = null
    private var discordToken: String? = null

    private var DELETE_OSZ = false
    private var SCAN_DOWNLOAD = false
    private var deleteUnimportedBeatmaps = false
    private var showFirstApproachCircle = false
    private var comboburst = false
    private var useCustomSkins = false
    private var useCustomSounds = false
    private var corovans = false
    private var showFPS = false
    private var complexAnimations = false
    private var snakingInSliders = false
    private var playMusicPreview = false
    private var showCursor = false
    private var shrinkPlayfieldDownwards = false
    private var hideNaviBar = false
    private var showScoreboard = false
    private var enablePP = false
    private var enableExtension = false
    private var seasonalBg = false
    private var stayOnline = false
    private var syncMusic = false
    private var burstEffects = false
    private var hitLighting = false
    private var useDither = false
    private var useParticles = false
    private var useCustomComboColors = false
    private var forceRomanized = false
    private var fixFrameOffset = false
    private var displayScoreStatistics = false
    private var hideReplayMarquee = false
    private var hideInGameUI = false
    private var receiveAnnouncements = false
    private var enableStoryboard = false
    private var safeBeatmapBg = false
    private var trianglesAnimation = false
    private var displayRealTimePPCounter = false
    private var useNightcoreOnMultiplayer = false
    private var videoEnabled = false
    private var deleteUnsupportedVideos = false
    private var submitScoreOnMultiplayer = false
    private var keepBackgroundAspectRatio = false
    private var noChangeDimInBreaks = false
    private var discordRichPresence = false

    private var RES_WIDTH = 0
    private var RES_HEIGHT = 0
    private var errorMeter = 0
    private var backgroundQuality = 0
    private var metronomeSwitch = 0

    private var soundVolume = 0f
    private var bgmVolume = 0f
    private var offset = 0f
    private var backgroundBrightness = 0f
    private var scaleMultiplier = 0f
    private var playfieldSize = 0f
    private var cursorSize = 0f

    private var skins: MutableMap<String, String>? = null
    private var comboColors: Array<RGBColor?> = arrayOfNulls(4)
    private lateinit var context: Context

    @JvmStatic
    fun loadConfig(context: Context) {
        this.context = context.applicationContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var s: String

        s = prefs.getString("background", "2") ?: "2"
        backgroundQuality = s.toInt()
        useCustomSkins = prefs.getBoolean("skin", false)
        useCustomSounds = prefs.getBoolean("beatmapSounds", true)
        comboburst = prefs.getBoolean("comboburst", false)
        corovans = prefs.getBoolean("images", false)
        showFPS = prefs.getBoolean("fps", false)
        errorMeter = (prefs.getString("errormeter", "0") ?: "0").toInt()
        showFirstApproachCircle = prefs.getBoolean("showfirstapproachcircle", false)
        metronomeSwitch = (prefs.getString("metronomeswitch", "1") ?: "1").toInt()
        showScoreboard = prefs.getBoolean("showscoreboard", true)
        enableStoryboard = prefs.getBoolean("enableStoryboard", false)
        trianglesAnimation = prefs.getBoolean("trianglesAnimation", true)
        videoEnabled = prefs.getBoolean("enableVideo", false)
        keepBackgroundAspectRatio = prefs.getBoolean("keepBackgroundAspectRatio", false)
        noChangeDimInBreaks = prefs.getBoolean("noChangeDimInBreaks", false)

        setSize()
        setPlayfieldSize(prefs.getInt("playfieldSize", 100) / 100f)

        shrinkPlayfieldDownwards = prefs.getBoolean("shrinkPlayfieldDownwards", true)
        complexAnimations = prefs.getBoolean("complexanimations", true)
        snakingInSliders = prefs.getBoolean("snakingInSliders", true)

        try {
            offset = FMath.clamp(prefs.getInt("offset", 0).toFloat(), -250f, 250f)
            backgroundBrightness = prefs.getInt("bgbrightness", 25) / 100f
            soundVolume = prefs.getInt("soundvolume", 100) / 100f
            bgmVolume = prefs.getInt("bgmvolume", 100) / 100f
            cursorSize = prefs.getInt("cursorSize", 50) / 100f
        } catch (e: RuntimeException) {
            prefs.edit()
                .putInt("offset", 0)
                .putInt("bgbrightness", 25)
                .putInt("soundvolume", 100)
                .putInt("bgmvolume", 100)
                .putInt("cursorSize", 50)
                .commit()
            loadConfig(context)
            return
        }

        defaultCorePath = Environment.getExternalStorageDirectory().toString() + "/osu!droid/"
        corePath = prefs.getString("corePath", defaultCorePath)
        if (corePath.isNullOrEmpty()) {
            corePath = defaultCorePath
        }
        if (corePath!![corePath!!.length - 1] != '/') {
            corePath += "/"
        }
        scorePath = corePath + "Scores/"

        skinPath = prefs.getString("skinPath", corePath + "Skin/")
        if (skinPath.isNullOrEmpty()) {
            skinPath = corePath + "Skin/"
        }
        if (skinPath!![skinPath!!.length - 1] != '/') {
            skinPath += "/"
        }

        skinTopPath = prefs.getString("skinTopPath", skinPath)
        if (skinTopPath.isNullOrEmpty()) {
            skinTopPath = skinPath
        }
        if (skinTopPath!![skinTopPath!!.length - 1] != '/') {
            skinTopPath += "/"
        }

        syncMusic = prefs.getBoolean("syncMusic", syncMusic)
        enableExtension = false
        cachePath = context.cacheDir.path
        burstEffects = prefs.getBoolean("bursts", burstEffects)
        hitLighting = prefs.getBoolean("hitlighting", hitLighting)
        useDither = prefs.getBoolean("dither", useDither)
        useParticles = prefs.getBoolean("particles", useParticles)
        useCustomComboColors = prefs.getBoolean("useCustomColors", useCustomComboColors)
        comboColors = arrayOfNulls(4)
        for (i in 1..4) {
            comboColors[i - 1] = RGBColor.hex2Rgb(ColorPickerPreference.convertToRGB(prefs.getInt("combo$i", -0x1000000)))
        }

        DELETE_OSZ = prefs.getBoolean("deleteosz", true)
        SCAN_DOWNLOAD = prefs.getBoolean("scandownload", false)
        deleteUnimportedBeatmaps = prefs.getBoolean("deleteUnimportedBeatmaps", false)
        forceRomanized = prefs.getBoolean("forceromanized", false)
        beatmapPath = prefs.getString("directory", corePath + "Songs/")
        if (beatmapPath.isNullOrEmpty()) {
            beatmapPath = corePath + "Songs/"
        }
        if (beatmapPath!![beatmapPath!!.length - 1] != '/') {
            beatmapPath += "/"
        }
        deleteUnsupportedVideos = prefs.getBoolean("deleteUnsupportedVideos", true)

        playMusicPreview = prefs.getBoolean("musicpreview", true)
        localUsername = prefs.getString("playername", "")
        showCursor = prefs.getBoolean("showcursor", false)
        hideNaviBar = prefs.getBoolean("hidenavibar", false)
        enablePP = false
        fixFrameOffset = prefs.getBoolean("fixFrameOffset", true)
        displayScoreStatistics = prefs.getBoolean("displayScoreStatistics", false)
        hideReplayMarquee = prefs.getBoolean("hideReplayMarquee", false)
        hideInGameUI = prefs.getBoolean("hideInGameUI", false)
        receiveAnnouncements = prefs.getBoolean("receiveAnnouncements", true)
        safeBeatmapBg = prefs.getBoolean("safebeatmapbg", false)
        displayRealTimePPCounter = prefs.getBoolean("displayRealTimePPCounter", false)

        discordRichPresence = prefs.getBoolean("discordRichPresence", false)
        discordToken = prefs.getString("discordToken", null)

        useNightcoreOnMultiplayer = prefs.getBoolean("player_nightcore", false)
        submitScoreOnMultiplayer = prefs.getBoolean("player_submitScore", true)

        if (receiveAnnouncements) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements")
        }

        onlineDeviceID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        loadOnlineConfig(context)
        FavoriteLibrary.get().load()
        KeyboardConfig.loadConfig(context)
    }

    @JvmStatic
    fun loadOnlineConfig(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        onlineUsername = prefs.getString("onlineUsername", "")
        onlinePassword = prefs.getString("onlinePassword", null)
        stayOnline = prefs.getBoolean("stayOnline", false)
        seasonalBg = prefs.getBoolean("seasonalBg", true)
    }

    @JvmStatic
    fun setSize() {
        val dm = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.getMetrics(dm)

        val width = Math.max(dm.widthPixels, dm.heightPixels)
        val height = Math.min(dm.widthPixels, dm.heightPixels)
        setSize(width, height)
        Debug.i("width=${dm.widthPixels} height=${dm.heightPixels}")
    }

    @JvmStatic
    fun setSize(width: Int, height: Int) {
        RES_WIDTH = 1280
        RES_HEIGHT = 1280 * height / width
    }

    @JvmStatic
    fun isEnableStoryboard(): Boolean = backgroundBrightness > 0.02f && enableStoryboard

    @JvmStatic
    fun setEnableStoryboard(enableStoryboard: Boolean) {
        Config.enableStoryboard = enableStoryboard
    }

    @JvmStatic
    fun isFixFrameOffset(): Boolean = fixFrameOffset

    @JvmStatic
    fun isDisplayScoreStatistics(): Boolean = displayScoreStatistics

    @JvmStatic
    fun isDisplayRealTimePPCounter(): Boolean = displayRealTimePPCounter

    @JvmStatic
    fun isEnableExtension(): Boolean = enableExtension

    @JvmStatic
    fun setEnableExtension(enableExtension: Boolean) {
        Config.enableExtension = enableExtension
    }

    @JvmStatic
    fun isShowFPS(): Boolean = showFPS

    @JvmStatic
    fun setShowFPS(showFPS: Boolean) {
        Config.showFPS = showFPS
    }

    @JvmStatic
    fun isShowScoreboard(): Boolean = showScoreboard

    @JvmStatic
    fun setShowScoreboard(showScoreboard: Boolean) {
        Config.showScoreboard = showScoreboard
    }

    @JvmStatic
    fun isCorovans(): Boolean = corovans

    @JvmStatic
    fun setCorovans(corovans: Boolean) {
        Config.corovans = corovans
    }

    @JvmStatic
    fun getSoundVolume(): Float = soundVolume

    @JvmStatic
    fun setSoundVolume(volume: Float) {
        soundVolume = volume
    }

    @JvmStatic
    fun getBgmVolume(): Float = bgmVolume

    @JvmStatic
    fun setBgmVolume(bgmVolume: Float) {
        Config.bgmVolume = bgmVolume
    }

    @JvmStatic
    fun getOffset(): Float = offset

    @JvmStatic
    fun setOffset(offset: Float) {
        Config.offset = offset
    }

    @JvmStatic
    fun getBackgroundQuality(): Int = backgroundQuality

    @JvmStatic
    fun setBackgroundQuality(backgroundQuality: Int) {
        Config.backgroundQuality = backgroundQuality
    }

    @JvmStatic
    fun getCorePath(): String = corePath ?: ""

    @JvmStatic
    fun setCorePath(corePath: String) {
        Config.corePath = corePath
    }

    @JvmStatic
    fun getBeatmapPath(): String = beatmapPath ?: ""

    @JvmStatic
    fun setBeatmapPath(path: String) {
        beatmapPath = path
    }

    @JvmStatic
    fun getRES_WIDTH(): Int = RES_WIDTH

    @JvmStatic
    fun setRES_WIDTH(rES_WIDTH: Int) {
        RES_WIDTH = rES_WIDTH
    }

    @JvmStatic
    fun getRES_HEIGHT(): Int = RES_HEIGHT

    @JvmStatic
    fun setRES_HEIGHT(rES_HEIGHT: Int) {
        RES_HEIGHT = rES_HEIGHT
    }

    @JvmStatic
    fun isDELETE_OSZ(): Boolean = DELETE_OSZ

    @JvmStatic
    fun setDELETE_OSZ(dELETE_OSZ: Boolean) {
        DELETE_OSZ = dELETE_OSZ
    }

    @JvmStatic
    fun isSCAN_DOWNLOAD(): Boolean = SCAN_DOWNLOAD

    @JvmStatic
    fun setSCAN_DOWNLOAD(sCAN_DOWNLOAD: Boolean) {
        SCAN_DOWNLOAD = sCAN_DOWNLOAD
    }

    @JvmStatic
    fun isDeleteUnimportedBeatmaps(): Boolean = deleteUnimportedBeatmaps

    @JvmStatic
    fun setDeleteUnimportedBeatmaps(deleteUnimportedBeatmaps: Boolean) {
        Config.deleteUnimportedBeatmaps = deleteUnimportedBeatmaps
    }

    @JvmStatic
    fun isUseCustomSkins(): Boolean = useCustomSkins

    @JvmStatic
    fun setUseCustomSkins(useCustomSkins: Boolean) {
        Config.useCustomSkins = useCustomSkins
    }

    @JvmStatic
    fun isUseCustomSounds(): Boolean = useCustomSounds

    @JvmStatic
    fun setUseCustomSounds(useCustomSounds: Boolean) {
        Config.useCustomSounds = useCustomSounds
    }

    @JvmStatic
    fun getTextureQuality(): Int = 1

    @JvmStatic
    fun getBackgroundBrightness(): Float = backgroundBrightness

    @JvmStatic
    fun setBackgroundBrightness(backgroundBrightness: Float) {
        Config.backgroundBrightness = backgroundBrightness
    }

    @JvmStatic
    fun isComplexAnimations(): Boolean = complexAnimations

    @JvmStatic
    fun isSnakingInSliders(): Boolean = snakingInSliders

    @JvmStatic
    fun setComplexAnimations(complexAnimations: Boolean) {
        Config.complexAnimations = complexAnimations
    }

    @JvmStatic
    fun isPlayMusicPreview(): Boolean = playMusicPreview

    @JvmStatic
    fun setPlayMusicPreview(playMusicPreview: Boolean) {
        Config.playMusicPreview = playMusicPreview
    }

    @JvmStatic
    fun getLocalUsername(): String = localUsername ?: ""

    @JvmStatic
    fun setLocalUsername(localUsername: String) {
        Config.localUsername = localUsername
    }

    @JvmStatic
    fun isShowCursor(): Boolean = showCursor

    @JvmStatic
    fun setShowCursor(showCursor: Boolean) {
        Config.showCursor = showCursor
    }

    @JvmStatic
    fun getScaleMultiplier(): Float = scaleMultiplier

    @JvmStatic
    fun setScaleMultiplier(scaleMultiplier: Float) {
        Config.scaleMultiplier = scaleMultiplier
    }

    @JvmStatic
    fun getOnlineUsername(): String = onlineUsername ?: ""

    @JvmStatic
    fun setOnlineUsername(onlineUsername: String) {
        Config.onlineUsername = onlineUsername
    }

    @JvmStatic
    fun getOnlinePassword(): String? = onlinePassword

    @JvmStatic
    fun setOnlinePassword(onlinePassword: String) {
        Config.onlinePassword = onlinePassword
    }

    @JvmStatic
    var isStayOnline: Boolean
        get() = stayOnline && BuildType.hasOnlineAccess()
        set(value) { Config.stayOnline = value }

    @JvmStatic
    fun isSeasonalBg(): Boolean = seasonalBg

    @JvmStatic
    fun setSeasonalBg(seasonalBg: Boolean) {
        Config.seasonalBg = seasonalBg
    }

    @JvmStatic
    fun getOnlineDeviceID(): String = onlineDeviceID ?: ""

    @JvmStatic
    fun isSyncMusic(): Boolean = syncMusic

    @JvmStatic
    fun setSyncMusic(syncMusic: Boolean) {
        Config.syncMusic = syncMusic
    }

    @JvmStatic
    fun getCachePath(): String = cachePath ?: ""

    @JvmStatic
    fun setCachePath(cachePath: String) {
        Config.cachePath = cachePath
    }

    @JvmStatic
    fun isBurstEffects(): Boolean = burstEffects

    @JvmStatic
    fun setBurstEffects(burstEffects: Boolean) {
        Config.burstEffects = burstEffects
    }

    @JvmStatic
    fun isHitLighting(): Boolean = hitLighting

    @JvmStatic
    fun setHitLighting(hitLighting: Boolean) {
        Config.hitLighting = hitLighting
    }

    @JvmStatic
    fun isUseDither(): Boolean = useDither

    @JvmStatic
    fun setUseDither(useDither: Boolean) {
        Config.useDither = useDither
    }

    @JvmStatic
    fun isUseParticles(): Boolean = useParticles

    @JvmStatic
    fun setUseParticles(useParticles: Boolean) {
        Config.useParticles = useParticles
    }

    @JvmStatic
    fun getSkinPath(): String = skinPath ?: ""

    @JvmStatic
    fun setSkinPath(skinPath: String) {
        Config.skinPath = skinPath
    }

    @JvmStatic
    fun getSkinTopPath(): String = skinTopPath ?: ""

    @JvmStatic
    fun setSkinTopPath(skinTopPath: String) {
        Config.skinTopPath = skinTopPath
    }

    @JvmStatic
    fun isHideNaviBar(): Boolean = hideNaviBar

    @JvmStatic
    fun setHideNaviBar(hideNaviBar: Boolean) {
        Config.hideNaviBar = hideNaviBar
    }

    @JvmStatic
    fun isEnablePP(): Boolean = enablePP

    @JvmStatic
    fun setEnablePP(enablePP: Boolean) {
        Config.enablePP = enablePP
    }

    @JvmStatic
    fun getScorePath(): String = scorePath ?: ""

    @JvmStatic
    fun setScorePath(scorePath: String) {
        Config.scorePath = scorePath
    }

    @JvmStatic
    fun isUseCustomComboColors(): Boolean = useCustomComboColors

    @JvmStatic
    fun setUseCustomComboColors(useCustomComboColors: Boolean) {
        Config.useCustomComboColors = useCustomComboColors
    }

    @JvmStatic
    fun getComboColors(): Array<RGBColor?> = comboColors

    @JvmStatic
    fun getErrorMeter(): Int = errorMeter

    @JvmStatic
    fun setErrorMeter(errorMeter: Int) {
        Config.errorMeter = errorMeter
    }

    @JvmStatic
    fun isShowFirstApproachCircle(): Boolean = showFirstApproachCircle

    @JvmStatic
    fun setShowFirstApproachCircle(showFirstApproachCircle: Boolean) {
        Config.showFirstApproachCircle = showFirstApproachCircle
    }

    @JvmStatic
    fun getMetronomeSwitch(): Int = metronomeSwitch

    @JvmStatic
    fun setMetronomeSwitch(metronomeSwitch: Int) {
        Config.metronomeSwitch = metronomeSwitch
    }

    @JvmStatic
    fun isComboburst(): Boolean = comboburst

    @JvmStatic
    fun setComboburst(comboburst: Boolean) {
        Config.comboburst = comboburst
    }

    @JvmStatic
    fun isForceRomanized(): Boolean = forceRomanized

    @JvmStatic
    fun setForceRomanized(forceRomanized: Boolean) {
        Config.forceRomanized = forceRomanized
    }

    @JvmStatic
    fun getCursorSize(): Float = cursorSize

    @JvmStatic
    fun setCursorSize() {
        Config.cursorSize = cursorSize
    }

    @JvmStatic
    fun getPlayfieldSize(): Float = playfieldSize

    @JvmStatic
    fun setPlayfieldSize(playfieldSize: Float) {
        Config.playfieldSize = playfieldSize
    }

    @JvmStatic
    fun isShrinkPlayfieldDownwards(): Boolean = shrinkPlayfieldDownwards

    @JvmStatic
    fun setShrinkPlayfieldDownwards(shrinkPlayfieldDownwards: Boolean) {
        Config.shrinkPlayfieldDownwards = shrinkPlayfieldDownwards
    }

    @JvmStatic
    fun isHideReplayMarquee(): Boolean = hideReplayMarquee

    @JvmStatic
    fun setHideReplayMarquee(hideReplayMarquee: Boolean) {
        Config.hideReplayMarquee = hideReplayMarquee
    }

    @JvmStatic
    fun isHideInGameUI(): Boolean = hideInGameUI

    @JvmStatic
    fun setHideInGameUI(hideInGameUI: Boolean) {
        Config.hideInGameUI = hideInGameUI
    }

    @JvmStatic
    fun isReceiveAnnouncements(): Boolean = receiveAnnouncements

    @JvmStatic
    fun setReceiveAnnouncements(receiveAnnouncements: Boolean) {
        Config.receiveAnnouncements = receiveAnnouncements
    }

    @JvmStatic
    fun isSafeBeatmapBg(): Boolean = safeBeatmapBg

    @JvmStatic
    fun setSafeBeatmapBg(safeBeatmapBg: Boolean) {
        Config.safeBeatmapBg = safeBeatmapBg
    }

    @JvmStatic
    fun isTrianglesAnimation(): Boolean = trianglesAnimation

    @JvmStatic
    fun setTrianglesAnimation(trianglesAnimation: Boolean) {
        Config.trianglesAnimation = trianglesAnimation
    }

    @JvmStatic
    fun getDefaultCorePath(): String = defaultCorePath ?: ""

    @JvmStatic
    fun loadSkins() {
        val folders = FileUtils.listFiles(File(skinTopPath!!), { file -> file.isDirectory && !file.name.startsWith(".") })
        skins = HashMap()
        for (folder in folders!!) {
            skins!![folder.name] = folder.path
            Debug.i("skins: ${folder.name} - ${folder.path}")
        }
    }

    @JvmStatic
    fun getSkins(): Map<String, String>? = skins

    @JvmStatic
    fun addSkin(name: String, path: String) {
        if (skins == null) skins = HashMap()
        skins!![name] = path
    }

    @JvmStatic
    fun isUseNightcoreOnMultiplayer(): Boolean = useNightcoreOnMultiplayer

    @JvmStatic
    fun setUseNightcoreOnMultiplayer(value: Boolean) {
        useNightcoreOnMultiplayer = value
    }

    @JvmStatic
    fun isVideoEnabled(): Boolean = videoEnabled

    @JvmStatic
    fun setVideoEnabled(value: Boolean) {
        videoEnabled = value
    }

    @JvmStatic
    fun isDeleteUnsupportedVideos(): Boolean = deleteUnsupportedVideos

    @JvmStatic
    fun isSubmitScoreOnMultiplayer(): Boolean = submitScoreOnMultiplayer

    @JvmStatic
    fun setSubmitScoreOnMultiplayer(submitScoreOnMultiplayer: Boolean) {
        Config.submitScoreOnMultiplayer = submitScoreOnMultiplayer
    }

    @JvmStatic
    fun isKeepBackgroundAspectRatio(): Boolean = keepBackgroundAspectRatio

    @JvmStatic
    fun isNoChangeDimInBreaks(): Boolean = noChangeDimInBreaks

    @JvmStatic
    fun isDiscordRichPresence(): Boolean = discordRichPresence

    @JvmStatic
    fun setDiscordRichPresence(discordRichPresence: Boolean) {
        Config.discordRichPresence = discordRichPresence
    }

    @JvmStatic
    fun getDiscordToken(): String? = discordToken

    @JvmStatic
    fun setDiscordToken(token: String) {
        discordToken = token
    }

    @JvmStatic
    fun getHitCircleRadius(): Float {
        val cs = 54.4f - 4.48f * playfieldSize
        return cs * (112.5f / 2) / (128.0f / 2)
    }

    @JvmStatic
    fun getDifficulty(): DifficultyData = DifficultyData
}

object DifficultyData {
    @JvmStatic
    fun getTimePre(): Float = GameHelper.objectTimePre

    @JvmStatic
    fun getFadeInTime(): Float = GameHelper.objectTimeFadeIn

    @JvmStatic
    fun getDifficultyRange(value: Float, min: Float, mid: Float, max: Float): Float {
        if (value > 5) return mid - (max - mid) * (value - 5) / 5
        return mid + (value - 5) * (mid - min) / 5
    }
}
