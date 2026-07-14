package ru.nsu.ccfit.zuev.skins

import org.anddev.andengine.entity.sprite.Sprite
import org.json.JSONObject

class SkinLayout {
    @JvmField
    var property: JSONObject? = null
    @JvmField
    var width: Float = 0f
    @JvmField
    var height: Float = 0f
    @JvmField
    var xOffset: Float = 0f
    @JvmField
    var yOffset: Float = 0f
    @JvmField
    var scale: Float = 1f

    fun baseApply(entity: Sprite) {
        entity.setPosition(xOffset, yOffset)
        if (scale != -1f) {
            entity.setScale(scale)
        }
        if (width != -1f) {
            entity.setWidth(width)
        }
        if (height != -1f) {
            entity.setHeight(height)
        }
    }

    companion object {
        @JvmStatic
        fun load(`object`: JSONObject): SkinLayout {
            val layout = SkinLayout()
            layout.property = `object`
            layout.width = `object`.optDouble("w", -1.0).toFloat()
            layout.height = `object`.optDouble("h", -1.0).toFloat()
            layout.xOffset = `object`.optDouble("x", 0.0).toFloat()
            layout.yOffset = `object`.optDouble("y", 0.0).toFloat()
            layout.scale = `object`.optDouble("scale", -1.0).toFloat()
            return layout
        }
    }
}
