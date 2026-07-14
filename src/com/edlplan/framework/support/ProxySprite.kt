package com.edlplan.framework.support

import com.edlplan.framework.support.graphics.BaseCanvas

class ProxySprite(width: Float, height: Float) : SupportSprite(width, height) {
    var drawProxy: DrawProxy? = null

    override fun onSupportDraw(canvas: BaseCanvas) {
        super.onSupportDraw(canvas)
        drawProxy?.onSupportDraw(canvas)
    }

    interface DrawProxy {
        fun onSupportDraw(canvas: BaseCanvas)
    }
}