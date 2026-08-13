package ru.nsu.ccfit.zuev.osu

import androidx.annotation.NonNull
import org.anddev.andengine.entity.Entity

open class RGBColor {
    private var cr: Float = 0f
    private var cg: Float = 0f
    private var cb: Float = 0f

    constructor(copy: RGBColor) : this(copy.cr, copy.cg, copy.cb)

    constructor() {
        cr = 0f
        cg = 0f
        cb = 0f
    }

    constructor(r: Float, g: Float, b: Float) {
        cr = r
        cg = g
        cb = b
    }

    fun r(): Float = cr

    fun g(): Float = cg

    fun b(): Float = cb

    fun set(r: Float, g: Float, b: Float) {
        cr = r
        cg = g
        cb = b
    }

    fun apply(@NonNull entity: Entity) {
        entity.setColor(cr, cg, cb)
    }

    fun applyAll(@NonNull vararg entities: Entity) {
        for (entity in entities) {
            entity.setColor(cr, cg, cb)
        }
    }

    companion object {
        @JvmStatic
        fun hex2Rgb(colorStr: String): RGBColor {
            return RGBColor(
                colorStr.substring(1, 3).toInt(16) / 255.0f,
                colorStr.substring(3, 5).toInt(16) / 255.0f,
                colorStr.substring(5, 7).toInt(16) / 255.0f
            )
        }
    }
}
