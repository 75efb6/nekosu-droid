package ru.nsu.ccfit.zuev.skins

import com.edlplan.framework.utils.interfaces.Consumer
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.RGBColor

class SkinJsonReader private constructor() : SkinReader() {

    private var currentData: JSONObject? = null
    private var currentComboColorData: JSONObject? = null
    private var currentSliderData: JSONObject? = null
    private var currentUtilsData: JSONObject? = null
    private var currentLayoutData: JSONObject? = null
    private var currentColorData: JSONObject? = null
    private var currentCursorData: JSONObject? = null
    private var currentFontsData: JSONObject? = null

    fun supplyJson(json: JSONObject) {
        currentData = json
        loadSkin()
    }

    override fun loadSkinBase() {
        load("ComboColor", currentData!!) { c ->
            currentComboColorData = c
            loadComboColorSetting()
        }
        load("Slider", currentData!!) { c ->
            currentSliderData = c
            loadSlider()
        }
        load("Utils", currentData!!) { c ->
            currentUtilsData = c
            loadUtils()
        }
        load("Layout", currentData!!) { c ->
            currentLayoutData = c
            loadLayout()
        }
        load("Color", currentData!!) { c ->
            currentColorData = c
            loadColor()
        }
        load("Cursor", currentData!!) { c ->
            currentCursorData = c
            loadCursor()
        }
        load("Fonts", currentData!!) { c ->
            currentFontsData = c
            loadFonts()
        }
    }

    override fun loadFonts() {
        val skin = OsuSkin.get()
        skin.hitCirclePrefix.setFromJson(currentFontsData!!)
        skin.hitCircleOverlap.setFromJson(currentFontsData!!)
        skin.scorePrefix.setFromJson(currentFontsData!!)
        skin.comboPrefix.setFromJson(currentFontsData!!)
    }

    override fun loadComboColorSetting() {
        val skin = OsuSkin.get()
        val data = currentComboColorData!!
        skin.forceOverrideComboColor.setFromJson(data)
        skin.comboColor.clear()
        val array = data.optJSONArray("colors")
        if (array == null || array.length() == 0) {
            skin.comboColor.add(RGBColor.hex2Rgb(skin.DEFAULT_COLOR_HEX))
        } else {
            for (i in 0 until array.length()) {
                val hex = array.optString(i, skin.DEFAULT_COLOR_HEX)
                skin.comboColor.add(RGBColor.hex2Rgb(hex))
            }
        }
    }

    override fun loadSlider() {
        val skin = OsuSkin.get()
        val data = currentSliderData!!
        skin.sliderBodyWidth.setFromJson(data)
        skin.sliderBorderWidth.setFromJson(data)
        skin.sliderBodyBaseAlpha.setFromJson(data)
        skin.sliderHintWidth.setFromJson(data)
        skin.sliderHintShowMinLength.setFromJson(data)
        skin.sliderHintAlpha.setFromJson(data)
        skin.sliderFollowComboColor.setFromJson(data)
        skin.sliderHintEnable.setFromJson(data)
        skin.sliderBodyColor.setFromJson(data)
        skin.sliderBorderColor.setFromJson(data)
        skin.sliderHintColor.setFromJson(data)
    }

    override fun loadUtils() {
        val data = currentUtilsData!!
        val skin = OsuSkin.get()
        skin.limitComboTextLength.setFromJson(data)
        skin.disableKiai.setFromJson(data)
        skin.comboTextScale.setFromJson(data)
    }

    override fun loadLayout() {
        val skin = OsuSkin.get()
        val data = currentLayoutData!!
        skin.useNewLayout.setFromJson(data)
        val names = data.names() ?: return
        for (i in 0 until names.length()) {
            if (names.optString(i) == skin.useNewLayout.getTag()) {
                continue
            }
            val layoutJSON = data.optJSONObject(names.optString(i))
            if (layoutJSON != null) {
                putLayout(names.optString(i), SkinLayout.load(layoutJSON))
            }
        }
    }

    override fun loadColor() {
        val skin = OsuSkin.get()
        val data = currentColorData!!
        val names = data.names() ?: return
        for (i in 0 until names.length()) {
            skin.colorData[names.optString(i)] = RGBColor.hex2Rgb(data.optString(names.optString(i)))
        }
    }

    override fun loadCursor() {
        val skin = OsuSkin.get()
        val data = currentCursorData!!
        skin.rotateCursor.setFromJson(data)
    }

    fun load(tag: String, data: JSONObject, consumer: Consumer<JSONObject>) {
        var obj = data.optJSONObject(tag)
        if (obj == null) {
            obj = JSONObject()
        }
        consumer.consume(obj)
    }

    companion object {
        private val reader = SkinJsonReader()

        @JvmStatic
        fun getReader(): SkinJsonReader = reader
    }
}
