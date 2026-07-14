package com.edlplan.framework.support.graphics

import com.edlplan.framework.math.Color4
import com.edlplan.framework.support.batch.BatchEngine
import com.edlplan.framework.utils.AbstractSRable

abstract class BaseCanvas() : AbstractSRable<CanvasData>() {

    fun translate(tx: Float, ty: Float): BaseCanvas {
        getData().translate(tx, ty)
        BatchEngine.setGlobalCamera(getData().getCamera())
        return this
    }

    fun rotate(r: Float): BaseCanvas {
        getData().rotate(r)
        BatchEngine.setGlobalCamera(getData().getCamera())
        return this
    }

    fun rotate(ox: Float, oy: Float, r: Float): BaseCanvas {
        getData().rotate(ox, oy, r)
        BatchEngine.setGlobalCamera(getData().getCamera())
        return this
    }

    fun scale(x: Float, y: Float): BaseCanvas {
        getData().scale(x, y)
        BatchEngine.setGlobalCamera(getData().getCamera())
        return this
    }

    fun expendAxis(s: Float): BaseCanvas {
        getData().expendAxis(s)
        BatchEngine.setGlobalCamera(getData().getCamera())
        return this
    }

    fun clip(w: Float, h: Float): BaseCanvas {
        getData().clip(w, h)
        BatchEngine.setGlobalCamera(getData().getCamera())
        return this
    }

    fun getPixelDensity(): Float {
        return getData().getPixelDensity()
    }

    fun getWidth(): Float {
        return getData().getWidth()
    }

    fun getHeight(): Float {
        return getData().getHeight()
    }

    fun getCamera(): Camera {
        return getData().getCamera()
    }

    fun getCanvasAlpha(): Float {
        return getData().getCanvasAlpha()
    }

    fun setCanvasAlpha(a: Float) {
        if (Math.abs(a - getData().getCanvasAlpha()) > 0.0001) {
            BatchEngine.flush()
        }
        getData().setCanvasAlpha(a)
        BatchEngine.getShaderGlobals().alpha = a
    }

    fun checkPrepared(msg: String, p: Boolean) {
        if (p != isPrepared()) {
            throw GLException("prepare err [n,c]=[$p,${isPrepared()}] msg: $msg")
        }
    }

    fun isPrepared(): Boolean {
        return this === GLWrapped.getUsingCanvas()
    }

    fun prepare() {
        GLWrapped.prepareCanvas(this)
    }

    internal abstract fun onPrepare()

    fun unprepare() {
        flush()
        GLWrapped.unprepareCanvas(this)
    }

    internal abstract fun onUnprepare()

    override fun onSave(t: CanvasData) {
        BatchEngine.flush()
    }

    override fun onRestore(now: CanvasData, pre: CanvasData) {
        BatchEngine.setGlobalCamera(now.getCamera())
        BatchEngine.getShaderGlobals().alpha = now.getCanvasAlpha()
        pre.recycle()
    }

    fun supportClip(): Boolean {
        return false
    }

    protected open fun clipCanvas(x: Float, y: Float, width: Float, height: Float): BaseCanvas? {
        return null
    }

    fun requestClipCanvas(x: Float, y: Float, width: Float, height: Float): BaseCanvas? {
        checkPrepared("you can only clip canvas when it is not working", false)
        return if (supportClip()) {
            clipCanvas(x, y, width, height)
        } else {
            null
        }
    }

    abstract fun getBlendSetting(): BlendSetting

    val blendSetting: BlendSetting get() = getBlendSetting()

    protected abstract fun checkCanDraw()

    abstract override fun getDefData(): CanvasData

    abstract fun clearBuffer()

    abstract fun clearColor(c: Color4)

    fun flush() {
        BatchEngine.flush()
    }

    override fun recycle() {

    }

    @Suppress("DEPRECATION")
    protected open fun finalize() {
    }
}
