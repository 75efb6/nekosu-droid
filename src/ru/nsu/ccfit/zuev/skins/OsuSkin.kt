package ru.nsu.ccfit.zuev.skins

import okio.buffer
import okio.source
import ru.nsu.ccfit.zuev.osu.RGBColor
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class OsuSkin private constructor() {
    @JvmField
    internal val comboTextScale = FloatSkinData("comboTextScale", 1f)
    @JvmField
    internal val sliderHintWidth = FloatSkinData("sliderHintWidth", 3f)
    @JvmField
    internal val sliderBodyWidth = FloatSkinData("sliderBodyWidth", 61f)
    @JvmField
    internal val sliderBorderWidth = FloatSkinData("sliderBorderWidth", 5.2f)
    @JvmField
    internal val sliderBodyBaseAlpha = FloatSkinData("sliderBodyBaseAlpha", 0.7f)
    @JvmField
    internal val sliderHintAlpha = FloatSkinData("sliderHintAlpha")
    @JvmField
    internal val sliderHintShowMinLength = FloatSkinData("sliderHintShowMinLength", 300f)
    @JvmField
    internal val hitCircleOverlap = FloatSkinData("hitCircleOverlap", -2f)

    @JvmField
    internal val limitComboTextLength = BooleanSkinData("limitComboTextLength")
    @JvmField
    internal val disableKiai = BooleanSkinData("disableKiai")
    @JvmField
    internal val sliderHintEnable = BooleanSkinData("sliderHintEnable")
    @JvmField
    internal val sliderFollowComboColor = BooleanSkinData("sliderFollowComboColor", true)
    @JvmField
    internal val useNewLayout = BooleanSkinData("useNewLayout")
    @JvmField
    internal val forceOverrideComboColor = BooleanSkinData("forceOverride")
    @JvmField
    internal val rotateCursor = BooleanSkinData("rotateCursor", true)

    @JvmField
    internal val DEFAULT_COLOR_HEX = "#FFFFFF"
    @JvmField
    internal val comboColor: ArrayList<RGBColor> = ArrayList()

    @JvmField
    internal val sliderBorderColor = ColorSkinData("sliderBorderColor", DEFAULT_COLOR_HEX)
    @JvmField
    internal val sliderBodyColor = ColorSkinData("sliderBodyColor", DEFAULT_COLOR_HEX)
    @JvmField
    internal val sliderHintColor = ColorSkinData("sliderHintColor", DEFAULT_COLOR_HEX)

    @JvmField
    internal val hitCirclePrefix = StringSkinData("hitCirclePrefix", "default")
    @JvmField
    internal val scorePrefix = StringSkinData("scorePrefix", "score")
    @JvmField
    internal val comboPrefix = StringSkinData("comboPrefix", "score")

    @JvmField
    internal val layoutData: HashMap<String, SkinLayout> = HashMap()
    @JvmField
    internal val colorData: HashMap<String, RGBColor> = HashMap()

    fun isRotateCursor(): Boolean = rotateCursor.currentValue

    fun getComboTextScale(): Float = comboTextScale.currentValue

    fun isUseNewLayout(): Boolean = useNewLayout.currentValue

    fun isSliderHintEnable(): Boolean = sliderHintEnable.currentValue

    fun getSliderHintAlpha(): Float = sliderHintAlpha.currentValue

    fun getSliderHintWidth(): Float = sliderHintWidth.currentValue

    fun getSliderHintColor(): RGBColor = sliderHintColor.currentValue

    fun getSliderHintShowMinLength(): Float = sliderHintShowMinLength.currentValue

    fun getSliderBodyWidth(): Float = sliderBodyWidth.currentValue

    fun getSliderBorderWidth(): Float = sliderBorderWidth.currentValue

    fun isDisableKiai(): Boolean = disableKiai.currentValue

    fun isLimitComboTextLength(): Boolean = limitComboTextLength.currentValue

    fun getSliderBodyBaseAlpha(): Float = sliderBodyBaseAlpha.currentValue

    fun isForceOverrideComboColor(): Boolean = forceOverrideComboColor.currentValue

    fun getComboColor(): ArrayList<RGBColor> {
        if (comboColor.isEmpty()) {
            comboColor.add(RGBColor.hex2Rgb(DEFAULT_COLOR_HEX))
        }
        return comboColor
    }

    fun isForceOverrideSliderBorderColor(): Boolean = !sliderBorderColor.currentIsDefault()

    fun getSliderBorderColor(): RGBColor = sliderBorderColor.currentValue

    fun isSliderFollowComboColor(): Boolean = sliderFollowComboColor.currentValue

    fun getSliderBodyColor(): RGBColor = sliderBodyColor.currentValue

    fun getLayout(name: String): SkinLayout? = layoutData[name]

    fun getColor(name: String, fallback: RGBColor): RGBColor {
        val color = colorData[name]
        return color ?: fallback
    }

    fun getHitCirclePrefix(): StringSkinData = hitCirclePrefix

    fun getScorePrefix(): StringSkinData = scorePrefix

    fun getComboPrefix(): StringSkinData = comboPrefix

    fun getHitCircleOverlap(): Float = hitCircleOverlap.currentValue

    fun getTexture(name: String): org.anddev.andengine.opengl.texture.region.TextureRegion? {
        return ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance().getTexture(name)
    }

    fun isComboNumbers(): Boolean = true

    fun getComboCount(): Int {
        val colors = getComboColor()
        return colors.size
    }

    fun getComboColor(index: Int): RGBColor {
        val colors = getComboColor()
        if (colors.isEmpty()) return RGBColor(1f, 1f, 1f)
        return colors[index % colors.size]
    }

    fun reset() {
        layoutData.clear()
        colorData.clear()
    }

    companion object {
        private val skinJson = OsuSkin()

        @JvmStatic
        fun get(): OsuSkin = skinJson

        @JvmStatic
        @Throws(IOException::class)
        fun readFull(file: File): String {
            val source = file.source().buffer()
            val result = source.readUtf8()
            source.close()
            return result
        }
    }
}
