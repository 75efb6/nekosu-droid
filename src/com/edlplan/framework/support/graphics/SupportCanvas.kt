package com.edlplan.framework.support.graphics

import com.edlplan.framework.math.Color4
import com.edlplan.framework.math.Mat4

class SupportCanvas(private val supportInfo: SupportInfo) : BaseCanvas() {

    init {
        initial()
    }

    override fun onPrepare() {

    }

    override fun onUnprepare() {

    }

    override fun getBlendSetting(): BlendSetting {
        return GLWrapped.blend
    }

    override fun checkCanDraw() {

    }

    override fun getDefData(): CanvasData {
        val d = CanvasData()
        d.setCurrentProjMatrix(Mat4.createIdentity())
        d.setCurrentMaskMatrix(Mat4.createIdentity())
        d.setHeight(supportInfo.supportHeight)
        d.setWidth(supportInfo.supportWidth)
        return d
    }

    override fun clearBuffer() {
        GLWrapped.clearDepthAndColorBuffer()
    }

    override fun clearColor(c: Color4) {
        GLWrapped.setClearColor(c)
        GLWrapped.clearColorBuffer()
    }

    class SupportInfo {
        @JvmField
        var supportWidth: Float = 0f
        @JvmField
        var supportHeight: Float = 0f
    }

}
