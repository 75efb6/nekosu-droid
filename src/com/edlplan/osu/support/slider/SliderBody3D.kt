package com.edlplan.osu.support.slider

import com.edlplan.andengine.Triangle3DBuilder
import com.edlplan.andengine.Triangle3DPack
import com.edlplan.framework.math.line.LinePath
import org.anddev.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.RGBColor

class SliderBody3D(path: LinePath) : AbstractSliderBody(path) {

    private var body: Triangle3DPack? = null
    private var border: Triangle3DPack? = null
    private var bodyMask: Triangle3DPack? = null
    private var borderMask: Triangle3DPack? = null

    private val bodyColor = RGBColor()
    private val borderColor = RGBColor()

    private var bodyWidth: Float = 0f
    private var borderWidth: Float = 0f

    private var startLength: Float = 0f
    private var endLength: Float = 0f
    private var dirty: Boolean = true

    private val pathBuilder = Draw3DLinePath()
    private val sharedBuilder = Triangle3DBuilder()

    override fun onUpdate() {
        if (body == null || border == null || !dirty) return
        dirty = false

        val sub = path.cutPath(startLength, endLength).fitToLinePath()

        val alpha = endLength / path.measurer.maxLength()

        pathBuilder.prepareForPath(sub)

        body!!.setVertices(pathBuilder.buildForWidth(bodyWidth, 1f, 1f, sharedBuilder).getVertex())
        body!!.setAlpha(0.7f * alpha)

        border!!.setVertices(pathBuilder.buildForWidth(borderWidth, -1f, -1f, sharedBuilder).getVertex())
        border!!.setAlpha(alpha)
    }

    override fun setBodyWidth(width: Float) {
        bodyWidth = width
    }

    override fun setBorderWidth(width: Float) {
        borderWidth = width
    }

    override fun setBodyColor(r: Float, g: Float, b: Float) {
        bodyColor.set(r, g, b)
        body?.setColor(r, g, b)
    }

    override fun setBorderColor(r: Float, g: Float, b: Float) {
        borderColor.set(r, g, b)
        border?.setColor(r, g, b)
    }

    override fun setStartLength(length: Float) {
        if (startLength != length) {
            startLength = length
            dirty = true
        }
    }

    override fun setEndLength(length: Float) {
        if (endLength != length) {
            endLength = length
            dirty = true
        }
    }

    override fun applyToScene(scene: Scene, emptyOnStart: Boolean) {
        if (!emptyOnStart) {
            startLength = 0f
            endLength = path.measurer.maxLength()
        }

        body = Triangle3DPack(
            0f, 0f,
            if (emptyOnStart) {
                FloatArray(0)
            } else {
                Draw3DLinePath(path, bodyWidth, Z_END, zBody).getTriangles().getVertex()
            }
        )
        body!!.setClearDepthOnStart(true)

        border = Triangle3DPack(
            0f, 0f,
            if (emptyOnStart) {
                FloatArray(0)
            } else {
                Draw3DLinePath(path, borderWidth, Z_END, Z_START).getTriangles().getVertex()
            }
        )

        body!!.setColor(bodyColor.r(), bodyColor.g(), bodyColor.b())
        border!!.setColor(borderColor.r(), borderColor.g(), borderColor.b())

        scene.attachChild(border, 0)
        scene.attachChild(body, 0)

        dirty = false
    }

    override fun removeFromScene(scene: Scene) {
        body?.detachSelf()
        border?.detachSelf()
        bodyMask?.detachSelf()
        borderMask?.detachSelf()
    }

    private val zBody: Float
        get() = -bodyWidth / borderWidth + Z_OFF

    companion object {
        private const val Z_OFF = 0.001f
        private val Z_START = -1 + Z_OFF
        private const val Z_END = 1f
    }
}
