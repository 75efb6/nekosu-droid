package ru.nsu.ccfit.zuev.skins

import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import java.io.File
import java.util.HashMap

class SkinManager private constructor() {

    private val sliderColor = RGBColor(1f, 1f, 1f)
    private var skinname = ""

    fun getSliderColor(): RGBColor = sliderColor

    fun presetFrameCount() {
        stdframeCount["sliderb"] = 10
        stdframeCount["followpoint"] = 1
        stdframeCount["scorebar-colour"] = 4
        stdframeCount["play-skip"] = 1
        stdframeCount["sliderfollowcircle"] = 1

        for (s in stdframeCount.keys) {
            val fcount = ResourceManager.getInstance().getFrameCount(s)
            if (fcount >= 0) {
                stdframeCount[s] = fcount
            }
        }
        frameCount.clear()
        frameCount.putAll(stdframeCount)
    }

    fun loadBeatmapSkin(beatmapFolder: String) {
        skinEnabled = true
        if (skinname == beatmapFolder) {
            return
        }
        clearSkin()
        skinname = beatmapFolder
        val folderFile = File(beatmapFolder)
        val folderFiles = FileUtils.listFiles(folderFile, arrayOf(".wav", ".mp3", ".ogg", ".png", ".jpg")) ?: emptyArray()
        for (f in folderFiles) {
            if (!f.isFile) {
                continue
            }
            val nameLower = f.name.lowercase()
            if (Config.isUseCustomSounds()
                && (nameLower.endsWith(".wav")
                        || nameLower.endsWith(".mp3")
                        || nameLower.endsWith(".ogg"))
                && f.length() >= 1024
            ) {
                ResourceManager.getInstance().loadCustomSound(f)
            } else if (Config.isUseCustomSkins()
                && (nameLower.endsWith(".png")
                        || nameLower.endsWith(".jpg"))
            ) {
                ResourceManager.getInstance().loadCustomTexture(f)
            }
        }

        if (!Config.isUseCustomSkins()) return

        for (s in frameCount.keys) {
            val fcount = ResourceManager.getInstance().getFrameCount(s)
            if (fcount >= 0) {
                frameCount[s] = fcount
            }
        }
    }

    fun clearSkin() {
        if (skinname == "") {
            return
        }
        skinname = ""
        frameCount["sliderb"] = stdframeCount["sliderb"]!!
        frameCount["followpoint"] = stdframeCount["followpoint"]!!
        frameCount["scorebar-colour"] = stdframeCount["scorebar-colour"]!!
        frameCount["play-skip"] = stdframeCount["play-skip"]!!
        frameCount["sliderfollowcircle"] = stdframeCount["sliderfollowcircle"]!!
        ResourceManager.getInstance().clearCustomResources()
    }

    companion object {
        private val instance = SkinManager()
        private val frameCount: MutableMap<String, Int> = HashMap()
        private val stdframeCount: MutableMap<String, Int> = HashMap()
        private var skinEnabled = true

        init {
            stdframeCount["sliderb"] = 10
            stdframeCount["followpoint"] = 1
            stdframeCount["scorebar-colour"] = 4
            stdframeCount["play-skip"] = 1
            stdframeCount["sliderfollowcircle"] = 1
            frameCount.putAll(stdframeCount)
        }

        @JvmStatic
        fun getInstance(): SkinManager = instance

        @JvmStatic
        fun isSkinEnabled(): Boolean = skinEnabled

        @JvmStatic
        fun setSkinEnabled(skinEnabled: Boolean) {
            SkinManager.skinEnabled = skinEnabled
        }

        @JvmStatic
        fun getFrames(texname: String): Int {
            val count = frameCount[texname]
            return count ?: 0
        }

        @JvmStatic
        fun setFrames(texname: String, frames: Int) {
            frameCount[texname] = frames
        }
    }
}
