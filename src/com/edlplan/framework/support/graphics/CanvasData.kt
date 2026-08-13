package com.edlplan.framework.support.graphics

import com.edlplan.framework.math.Mat4
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.utils.interfaces.Copyable
import com.edlplan.framework.utils.interfaces.Recycleable

class CanvasData : Recycleable, Copyable<CanvasData> {

    private var width: Float = 0f

    private var height: Float = 0f

    private var camera: Camera? = null

    private var pixelDensity: Float = 1f

    private var canvasAlpha: Float = 1f

    private val theOrigin = Vec2()

    constructor(c: CanvasData) {
        this.camera = c.camera!!.copy()
        this.width = c.width
        this.height = c.height
        this.pixelDensity = c.pixelDensity
        this.canvasAlpha = c.canvasAlpha
        this.theOrigin.set(c.theOrigin)
    }

    constructor() {
        camera = Camera()
    }

    fun getTheOrigin(): Vec2 {
        return theOrigin
    }

    fun getCanvasAlpha(): Float {
        return canvasAlpha
    }

    fun setCanvasAlpha(canvasAlpha: Float) {
        this.canvasAlpha = canvasAlpha
    }

    fun getPixelDensity(): Float {
        return pixelDensity
    }

    fun getWidth(): Float {
        return width
    }

    fun setWidth(width: Float) {
        this.width = width
    }

    fun getHeight(): Float {
        return height
    }

    fun setHeight(height: Float) {
        this.height = height
    }

    fun getCurrentProjMatrix(): Mat4 {
        return camera!!.getProjectionMatrix()
    }

    fun setCurrentProjMatrix(projMatrix: Mat4) {
        this.camera!!.setProjectionMatrix(projMatrix)
        freshMatrix()
    }

    fun getCurrentMaskMatrix(): Mat4 {
        return camera!!.getMaskMatrix()
    }

    fun setCurrentMaskMatrix(matrix: Mat4) {
        this.camera!!.setMaskMatrix(matrix)
    }

    fun translate(tx: Float, ty: Float): CanvasData {
        getCurrentMaskMatrix().translate(tx, ty, 0f)
        theOrigin.add(tx, ty)
        freshMatrix()
        return this
    }

    fun rotate(rotation: Float): CanvasData {
        getCurrentMaskMatrix().rotate2D(0f, 0f, rotation, true)
        freshMatrix()
        return this
    }

    fun rotate(ox: Float, oy: Float, rotation: Float): CanvasData {
        getCurrentMaskMatrix().rotate2D(ox, oy, rotation, true)
        freshMatrix()
        return this
    }

    fun scale(sx: Float, sy: Float): CanvasData {
        getCurrentMaskMatrix().scale(sx, sy, 1f)
        theOrigin.x *= sx
        theOrigin.y *= sy
        freshMatrix()
        return this
    }

    fun expendAxis(s: Float): CanvasData {
        if (s == 0f)
            throw IllegalArgumentException("you can't scale content using a scale rate ==0")
        val rs = 1 / s
        scale(rs, rs)
        this.pixelDensity *= s
        return this
    }

    fun freshMatrix() {
        camera!!.refresh()
    }

    fun getCamera(): Camera {
        return camera!!
    }

    fun clip(w: Float, h: Float): CanvasData {
        setWidth(w)
        setHeight(h)
        return this
    }

    override fun recycle() {
        this.camera = null
    }

    override fun copy(): CanvasData {
        return CanvasData(this)
    }
}
