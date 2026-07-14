package ru.nsu.ccfit.zuev.osu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.framework.data.IniReader
import com.reco1l.legacy.data.convertToJson
import com.reco1l.legacy.data.ensureOptionalTexture
import com.reco1l.legacy.data.ensureTexture
import com.reco1l.legacy.engine.BlankTextureRegion
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.font.FontFactory
import org.anddev.andengine.opengl.font.StrokeFont
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.StreamUtils
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.HashMap
import java.util.Objects
import java.util.regex.Pattern
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource
import ru.nsu.ccfit.zuev.osu.helper.ScaledBitmapSource
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinJsonReader
import ru.nsu.ccfit.zuev.skins.SkinManager
import ru.nsu.ccfit.zuev.skins.StringSkinData

class ResourceManager private constructor() {
    private val fonts: MutableMap<String, Font> = HashMap()
    private val textures: MutableMap<String, TextureRegion?> = HashMap()
    private val sounds: MutableMap<String, BassSoundProvider> = HashMap()
    private val customSounds: MutableMap<String, BassSoundProvider> = HashMap()
    private val customTextures: MutableMap<String, TextureRegion> = HashMap()
    private val customFrameCount: MutableMap<String, Int> = HashMap()
    private val emptySound = BassSoundProvider()
    var engine: Engine? = null
        private set
    private var context: Context? = null

    fun Init(engine: Engine, context: Context) {
        this.engine = engine
        this.context = context

        fonts.clear()
        textures.clear()
        sounds.clear()

        customSounds.clear()
        customTextures.clear()
        customFrameCount.clear()

        initSecurityUtils()
    }

    fun loadSkin(folder: String) {
        var folderVar = folder
        loadFont("smallFont", null, 21, Color.WHITE)
        loadFont("middleFont", null, 24, Color.WHITE)
        loadFont("bigFont", null, 36, Color.WHITE)
        loadFont("font", null, 28, Color.WHITE)
        loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE)
        loadFont("CaptionFont", null, 35, Color.WHITE)

        if (!folderVar.endsWith("/")) folderVar = "$folderVar/"

        loadCustomSkin(folderVar)

        loadTexture("::track", "gfx/hitcircle.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        loadTexture("::track2", "gfx/slidertrack.png", false)
        loadTexture("::trackborder", "gfx/sliderborder.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        loadTexture("ranking_enabled", "ranking_enabled.png", false)
        loadTexture("ranking_disabled", "ranking_disabled.png", false)
        loadTexture("flashlight_cursor", "flashlight_cursor.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA)

        if (!textures.containsKey("lighting"))
            textures["lighting"] = null
    }

    fun loadCustomSkin(folder: String) {
        var folderVar = folder
        if (!folderVar.endsWith("/")) folderVar += "/"

        var skinFiles: Array<File>? = null
        var skinFolder = File(folderVar)
        if (!skinFolder.exists()) {
            skinFolder = File(folderVar) // will be treated as null below
        } else {
            skinFiles = FileUtils.listFiles(skinFolder)
        }
        if (skinFiles != null) {
            var skinjson: JSONObject? = null
            val jsonFile = File(folderVar, "skin.json")
            if (jsonFile.exists()) {
                try {
                    skinjson = JSONObject(OsuSkin.readFull(jsonFile))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val iniFile = File(folderVar, "skin.ini")
                if (iniFile.exists()) {
                    GlobalManager.getInstance().info = "Reading skin.ini..."
                    try {
                        val ini = IniReader(iniFile)
                        skinjson = convertToJson(ini)
                        ini.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    ensureOptionalTexture(File(folderVar, "sliderendcircle.png"))
                    ensureOptionalTexture(File(folderVar, "sliderendcircleoverlay.png"))
                    ensureTexture(File(folderVar, "selection-mods.png"))
                    ensureTexture(File(folderVar, "selection-random.png"))
                    ensureTexture(File(folderVar, "selection-options.png"))

                    skinFiles = FileUtils.listFiles(skinFolder)
                }
            }
            if (skinjson == null) {
                try {
                    skinjson = JSONObject(StreamUtils.readFully(context!!.assets.open("default-skin.json")))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (skinjson == null) skinjson = JSONObject()
            SkinJsonReader.getReader().supplyJson(skinjson)
        } else {
            var skinjson: JSONObject? = null
            try {
                skinjson = JSONObject(StreamUtils.readFully(context!!.assets.open("default-skin.json")))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (skinjson == null) skinjson = JSONObject()
            SkinJsonReader.getReader().supplyJson(skinjson)
        }
        val availableFiles: MutableMap<String, File> = HashMap()
        if (skinFiles != null) {
            for (f in skinFiles) {
                if (f.isFile) {
                    if (f.name.startsWith("comboburst") && (f.name.endsWith(".wav") || f.name.endsWith(".mp3"))) {
                        continue
                    }
                    if (f.name.length < 5) {
                        continue
                    }
                    if (f.length() == 0L) {
                        continue
                    }
                    val filename = f.name.substring(0, f.name.length - 4)
                    availableFiles[filename] = f

                    if (filename == "hitcircle") {
                        if (!availableFiles.containsKey("sliderstartcircle")) {
                            availableFiles["sliderstartcircle"] = f
                        }
                        if (!availableFiles.containsKey("sliderendcircle")) {
                            availableFiles["sliderendcircle"] = f
                        }
                    }
                    if (filename == "hitcircleoverlay") {
                        if (!availableFiles.containsKey("sliderstartcircleoverlay")) {
                            availableFiles["sliderstartcircleoverlay"] = f
                        }
                        if (!availableFiles.containsKey("sliderendcircleoverlay")) {
                            availableFiles["sliderendcircleoverlay"] = f
                        }
                    }
                }
            }
        }

        customFrameCount.clear()

        try {
            for (s in context!!.assets.list("gfx") ?: emptyArray()) {
                val name = s.substring(0, s.length - 4)
                if (!Config.isCorovans()) {
                    if (name == "count1" || name == "count2" || name == "count3" || name == "go" || name == "ready") {
                        continue
                    }
                }
                if (availableFiles.containsKey(name)) {
                    loadTexture(name, availableFiles[name]!!.path, true)
                    if (Character.isDigit(name[name.length - 1])) {
                        noticeFrameCount(name)
                    }
                } else {
                    loadTexture(name, "gfx/$s", false)
                }
            }
            if (availableFiles.containsKey("scorebar-kidanger")) {
                loadTexture("scorebar-kidanger", availableFiles["scorebar-kidanger"]!!.path, true)
                loadTexture(
                    "scorebar-kidanger2",
                    availableFiles[
                        if (availableFiles.containsKey("scorebar-kidanger2")) "scorebar-kidanger2" else "scorebar-kidanger"
                    ]!!.path, true
                )
            }
            if (availableFiles.containsKey("comboburst"))
                loadTexture("comboburst", availableFiles["comboburst"]!!.path, true)
            else unloadTexture("comboburst")

            for (i in 0..9) {
                val textureName = "comboburst-$i"
                if (availableFiles.containsKey(textureName)) {
                    val file = availableFiles[textureName]
                    if (file != null) {
                        loadTexture(textureName, file.path, true)
                    } else {
                        unloadTexture(textureName)
                    }
                }
            }

            val names = arrayOf("play-skip-", "menu-back-", "scorebar-colour-", "hit0-", "hit50-", "hit100-", "hit100k-", "hit300-", "hit300k-", "hit300g-")
            for (name in names) {
                for (i in 0..59) {
                    val textureName = "$name$i"
                    if (availableFiles.containsKey(textureName)) {
                        val file = availableFiles[textureName]
                        if (file != null) {
                            loadTexture(textureName, file.path, true)
                        } else {
                            unloadTexture(textureName)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Debug.e("Resources: ${e.message}", e)
        }

        SkinManager.getInstance().presetFrameCount()

        try {
            for (s in context!!.assets.list("sfx") ?: emptyArray()) {
                val name = s.substring(0, s.length - 4)
                if (availableFiles.containsKey(name)) {
                    loadSound(name, availableFiles[name]!!.path, true)
                } else {
                    loadSound(name, "sfx/$s", false)
                }
            }
            loadSound("comboburst", "${folderVar}comboburst.wav", true)
            for (i in 0..9) {
                loadSound("comboburst-$i", "${folderVar}comboburst-$i.wav", true)
            }
        } catch (e: IOException) {
            Debug.e("Resources: ${e.message}", e)
        }

        loadTexture("::track", "gfx/hitcircle.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        loadTexture("::track2", "gfx/slidertrack.png", false)
        loadTexture("::trackborder", "gfx/sliderborder.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        loadTexture("ranking_button", "ranking_button.png", false)
        loadTexture("ranking_enabled", "ranking_enabled.png", false)
        loadTexture("ranking_disabled", "ranking_disabled.png", false)
        loadTexture("selection-approved", "selection-approved.png", false)
        loadTexture("selection-loved", "selection-loved.png", false)
        loadTexture("selection-question", "selection-question.png", false)
        loadTexture("selection-ranked", "selection-ranked.png", false)
        if (!textures.containsKey("lighting"))
            textures["lighting"] = null
    }

    private fun noticeFrameCount(name: String) {
        val resnameWN: String = if (!name.contains("-")) {
            name.substring(0, name.length - 1)
        } else {
            name.substring(0, name.lastIndexOf('-'))
        }
        val frameNum: Int = try {
            Integer.parseInt(name.substring(resnameWN.length))
        } catch (e: NumberFormatException) {
            return
        }
        val absFrameNum = if (frameNum < 0) frameNum * -1 else frameNum
        if (!customFrameCount.containsKey(resnameWN) || Objects.requireNonNull(customFrameCount[resnameWN])!! < absFrameNum) {
            customFrameCount[resnameWN] = absFrameNum
        }
    }

    fun loadFont(resname: String, file: String?, size: Int, color: Int): Font {
        val actualSize = size.toFloat() / Config.getTextureQuality()
        val texture = BitmapTextureAtlas(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        val font: Font = if (file == null) {
            Font(texture, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), actualSize, true, color)
        } else {
            FontFactory.createFromAsset(texture, context, "fonts/$file", actualSize, true, color)
        }
        engine!!.textureManager.loadTexture(texture)
        engine!!.fontManager.loadFont(font)
        fonts[resname] = font
        return font
    }

    fun loadStrokeFont(resname: String, file: String?, size: Int, color1: Int, color2: Int): StrokeFont {
        val actualSize = size.toFloat() / Config.getTextureQuality()
        val texture = BitmapTextureAtlas(512, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        val font: StrokeFont = if (file == null) {
            StrokeFont(texture, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), actualSize, true, color1,
                if (Config.getTextureQuality() == 1) 2f else 0.75f, color2)
        } else {
            FontFactory.createStrokeFromAsset(texture, context, "fonts/$file", actualSize, true, color1,
                2f / Config.getTextureQuality(), color2)
        }
        engine!!.textureManager.loadTexture(texture)
        engine!!.fontManager.loadFont(font)
        fonts[resname] = font
        return font
    }

    fun getFont(resname: String): Font {
        if (!fonts.containsKey(resname)) {
            loadFont(resname, null, 35, Color.WHITE)
        }
        return fonts[resname]!!
    }

    fun loadTexture(resname: String, file: String, external: Boolean, opt: TextureOptions): TextureRegion? {
        return loadTexture(resname, file, external, opt, engine!!)
    }

    fun loadTexture(resname: String, file: String, external: Boolean): TextureRegion? {
        return loadTexture(resname, file, external, TextureOptions.BILINEAR, engine!!)
    }

    fun loadTexture(resname: String, file: String, external: Boolean, engine: Engine): TextureRegion? {
        return loadTexture(resname, file, external, TextureOptions.BILINEAR, engine)
    }

    fun loadBackground(file: String): TextureRegion? {
        return loadBackground(file, engine!!)
    }

    fun loadBackground(file: String, engine: Engine): TextureRegion? {
        if (textures.containsKey("::background")) {
            engine.textureManager.unloadTexture(
                Objects.requireNonNull(textures["::background"])!!.texture
            )
        }
        if (file == "null") {
            return textures["menu-background"]
        }
        var tw = 16
        var th = 16
        val region: TextureRegion
        val source = ScaledBitmapSource(File(file))
        if (source.width == 0 || source.height == 0) {
            return textures["menu-background"]
        }
        while (tw < source.width) { tw *= 2 }
        while (th < source.height) { th *= 2 }
        if (!source.preload()) {
            textures["::background"] = textures["menu-background"]
            return textures["::background"]
        }
        val tex = BitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)
        region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        engine.textureManager.loadTexture(tex)
        textures["::background"] = region
        return region
    }

    fun loadTexture(resname: String, file: String, external: Boolean, opt: TextureOptions, engine: Engine): TextureRegion? {
        var tw = 4
        var th = 4
        val region: TextureRegion
        if (external) {
            var texFile = File(file)
            var isHDTexture = false

            if (!texFile.exists()) {
                val dotIndex = file.lastIndexOf('.')
                texFile = File(file.substring(0, dotIndex) + "@2x" + file.substring(dotIndex))
                isHDTexture = texFile.exists()

                if (!isHDTexture) return BlankTextureRegion()
            }
            val source = QualityFileBitmapSource(texFile, if (isHDTexture) 2 else 1)
            if (source.width == 0 || source.height == 0) {
                return null
            }
            while (tw < source.width) { tw *= 2 }
            while (th < source.height) { th *= 2 }

            var errorCount = 0
            while (!source.preload() && errorCount < 3) {
                errorCount++
            }
            if (errorCount >= 3) {
                return null
            }
            val tex = BitmapTextureAtlas(tw, th, opt)
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
            engine.textureManager.loadTexture(tex)
            textures[resname] = region
        } else {
            val source: QualityAssetBitmapSource
            try {
                source = QualityAssetBitmapSource(context!!, file)
            } catch (e: NullPointerException) {
                return textures.values.iterator().next()
            }

            if (source.width == 0 || source.height == 0) {
                return null
            }
            while (tw < source.width) { tw *= 2 }
            while (th < source.height) { th *= 2 }
            var errorCount = 0
            while (!source.preload() && errorCount < 3) {
                errorCount++
            }
            if (errorCount >= 3) {
                return null
            }
            val tex = BitmapTextureAtlas(tw, th, opt)
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
            engine.textureManager.loadTexture(tex)
            textures[resname] = region
        }

        if (region.width > 1) {
            region.width = region.width - 1
        }
        if (region.height > 1) {
            region.height = region.height - 1
        }

        return region
    }

    fun loadHighQualityAsset(resname: String, file: String): TextureRegion? {
        var tw = 16
        var th = 16

        val source = AssetBitmapTextureAtlasSource(context, file)
        if (source.width == 0 || source.height == 0) {
            return null
        }
        while (tw < source.width) { tw *= 2 }
        while (th < source.height) { th *= 2 }
        val tex = BitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)
        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        engine!!.textureManager.loadTexture(tex)
        textures[resname] = region
        return region
    }

    fun loadHighQualityFile(resname: String, file: File): TextureRegion? {
        var tw = 16
        var th = 16

        val source = FileBitmapTextureAtlasSource(file)
        if (source.width == 0 || source.height == 0) {
            return null
        }
        while (tw < source.width) { tw *= 2 }
        while (th < source.height) { th *= 2 }
        val tex = BitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)
        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        engine!!.textureManager.loadTexture(tex)
        textures[resname] = region
        region.width = region.width - 1
        region.height = region.height - 1
        return region
    }

    fun loadHighQualityFileUnderFolder(folder: File) {
        val files = FileUtils.listFiles(folder, arrayOf(".png", ".jpg", ".bmp")) ?: emptyArray()
        for (file in files) {
            if (file.isDirectory) {
                loadHighQualityFileUnderFolder(file)
            } else {
                Log.i("texture", "load: ${file.path}")
                loadHighQualityFile(file.path, file)
            }
        }
    }

    fun getTextureWithPrefix(prefix: StringSkinData, name: String): TextureRegion? {
        val defaultName = prefix.defaultValue + "-" + name
        if (SkinManager.isSkinEnabled() && customTextures.containsKey(defaultName)) {
            return customTextures[defaultName]
        }

        val customName = prefix.currentValue + "-" + name

        if (!textures.containsKey(customName)) {
            loadTexture(customName, Config.getSkinPath() + customName.replace("\\", "") + ".png", true)
        }

        if (textures[customName] != null) {
            return textures[customName]
        }
        return textures[defaultName]
    }

    fun getTexture(resname: String): TextureRegion? {
        if (SkinManager.isSkinEnabled()) {
            val custom = customTextures[resname]
            if (custom != null) {
                return custom
            }
        }
        val texture = textures[resname]
        if (texture == null) {
            Debug.i("Loading texture: $resname")
            return loadTexture(resname, "gfx/$resname.png", false)
        }
        return texture
    }

    fun getAvatarTextureIfLoaded(avatarURL: String): TextureRegion? {
        return getTextureIfLoaded(MD5Calculator.getStringMD5(avatarURL))
    }

    fun getBannerTextureIfLoaded(bannerUrl: String): TextureRegion? {
        return getTextureIfLoaded(MD5Calculator.getStringMD5(bannerUrl))
    }

    fun getTextureIfLoaded(resname: String): TextureRegion? = textures[resname]

    fun isTextureLoaded(resname: String): Boolean = textures.containsKey(resname)

    fun loadSound(resname: String, file: String, external: Boolean): BassSoundProvider? {
        val snd = BassSoundProvider()
        if (external) {
            try {
                if (!snd.prepare(file)) {
                    val shortName = file.substring(file.lastIndexOf("/") + 1)
                    if (!snd.prepare(context!!.assets, "sfx/$shortName")) {
                        return null
                    }
                }
            } catch (e: Exception) {
                Debug.e("ResourceManager.loadSoundFromExternal: ${e.message}", e)
                return null
            }
        } else {
            try {
                if (!snd.prepare(context!!.assets, file)) {
                    return null
                }
            } catch (e: Exception) {
                Debug.e("ResourceManager.loadSound: ${e.message}", e)
                return null
            }
        }

        sounds[resname] = snd
        return snd
    }

    fun getSound(resname: String): BassSoundProvider {
        val sound = sounds[resname]
        return sound ?: emptySound
    }

    fun loadCustomSound(file: File) {
        val snd = BassSoundProvider()
        var resName = file.name
        resName = resName.substring(0, resName.length - 4)
        if (resName.isEmpty()) {
            return
        }
        val matcher = SOUND_NAME_PATTERN.matcher(resName)
        if (matcher.find()) {
            val setName = matcher.group(1)
            if (!sounds.containsKey(setName)) {
                return
            }
        }
        try {
            if (!snd.prepare(file.path)) {
                return
            }
        } catch (e: Exception) {
            Debug.e("ResourceManager.loadCustomSound: ${e.message}", e)
            return
        }

        customSounds[resName] = snd
    }

    fun getCustomSound(resname: String, set: Int): BassSoundProvider {
        if (!SkinManager.isSkinEnabled()) {
            return getSound(resname)
        }
        if (set >= 2) {
            val fullName = resname + set
            val custom = customSounds[fullName]
            if (custom != null) {
                return custom
            }
            return sounds[resname]!!
        }
        val custom = customSounds[resname]
        if (custom != null) {
            return custom
        }
        return sounds[resname]!!
    }

    fun loadCustomTexture(file: File) {
        var resname = file.name.substring(0, file.name.length - 4).lowercase()
        var multiframe = false
        var delimiter = "-"

        if (Character.isDigit(resname[resname.length - 1])) {
            val resnameWN: String = if (!resname.contains("-")) {
                resname.substring(0, resname.length - 1)
            } else {
                resname.substring(0, resname.lastIndexOf('-'))
            }

            if (!textures.containsKey(resname) && SkinManager.getFrames(resnameWN) == 0) {
                return
            }

            if (textures.containsKey(resnameWN) || textures.containsKey("$resnameWN-0") || textures.containsKey("${resnameWN}0")) {
                var frameNum = Integer.parseInt(resname.substring(resnameWN.length))
                if (frameNum < 0) frameNum *= -1
                if (!customFrameCount.containsKey(resnameWN) || Objects.requireNonNull(customFrameCount[resnameWN])!! < frameNum) {
                    customFrameCount[resnameWN] = frameNum
                }
            }
        } else if (!textures.containsKey(resname)) {
            if (textures.containsKey("$resname-0") || textures.containsKey("${resname}0")) {
                if (textures.containsKey("${resname}0")) delimiter = ""
                if (SkinManager.getFrames(resname) != 0) {
                    customFrameCount[resname] = 1
                }
                multiframe = true
            } else {
                return
            }
        }
        var tw = 16
        var th = 16
        val source = QualityFileBitmapSource(file)
        while (tw < source.width) { tw *= 2 }
        while (th < source.height) { th *= 2 }
        if (!source.preload()) {
            return
        }
        val tex = BitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)
        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        engine!!.textureManager.loadTexture(tex)
        if (region.width > 1) {
            region.width = region.width - 1
        }
        if (region.height > 1) {
            region.height = region.height - 1
        }
        if (multiframe) {
            var i = 0
            while (textures.containsKey("$resname$delimiter$i")) {
                customTextures["$resname$delimiter$i"] = region
                i++
            }
        } else {
            customTextures[resname] = region

            if (resname == "hitcircle") {
                if (!customTextures.containsKey("sliderstartcircle")) {
                    customTextures["sliderstartcircle"] = region
                }
                if (!customTextures.containsKey("sliderendcircle")) {
                    customTextures["sliderendcircle"] = region
                }
            }

            if (resname == "hitcircleoverlay") {
                if (!customTextures.containsKey("sliderstartcircleoverlay")) {
                    customTextures["sliderstartcircleoverlay"] = region
                }
                if (!customTextures.containsKey("sliderendcircleoverlay")) {
                    customTextures["sliderendcircleoverlay"] = region
                }
            }
        }
    }

    fun unloadTexture(name: String) {
        if (textures[name] != null) {
            engine!!.textureManager.unloadTexture(
                Objects.requireNonNull(textures[name])!!.texture
            )
            textures.remove(name)
            Debug.i("Texture \"$name\"unloaded")
        }
    }

    fun unloadTexture(texture: TextureRegion) {
        engine!!.textureManager.unloadTexture(texture.texture)

        val toRemove = mutableListOf<String>()
        for (entry in textures.entries) {
            if (entry.value === texture) {
                toRemove.add(entry.key)
            }
        }
        for (key in toRemove) {
            textures.remove(key)
        }
    }

    fun initSecurityUtils() {
        SecurityUtils.getAppSignature(context!!, context!!.packageName)
    }

    fun clearCustomResources() {
        for (s in customSounds.values) {
            s.free()
        }
        val texnames = customTextures.keys.toSet()
        for (s in texnames) {
            val tex = customTextures[s]
            if (tex != null && tex.texture != null && tex.texture.isLoadedToHardware) {
                engine!!.textureManager.unloadTexture(tex.texture)
            }
        }
        customTextures.clear()
        customSounds.clear()
        customFrameCount.clear()
    }

    fun getFrameCount(texname: String): Int {
        return if (!customFrameCount.containsKey(texname)) {
            -1
        } else {
            Objects.requireNonNull(customFrameCount[texname])!!
        }
    }

    fun checkSpinnerTextures() {
        val names = arrayOf("spinner-background", "spinner-circle", "spinner-metre", "spinner-approachcircle", "spinner-spin")
        for (s in names) {
            val tex = textures[s]
            if (tex != null && tex.texture != null && !tex.texture.isLoadedToHardware) {
                engine!!.textureManager.reloadTextures()
                break
            }
        }
    }

    companion object {
        @JvmField
        val SOUND_NAME_PATTERN: Pattern = Pattern.compile("([^\\d.]+)")
        private val mgr = ResourceManager()

        @JvmStatic
        fun getInstance(): ResourceManager = mgr
    }
}
