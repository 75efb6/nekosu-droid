package com.edlplan.framework.support

import com.edlplan.framework.support.batch.BatchEngine
import com.edlplan.framework.support.graphics.BaseCanvas
import com.edlplan.framework.support.graphics.BlendType
import com.edlplan.framework.support.graphics.GLWrapped
import com.edlplan.framework.support.graphics.SupportCanvas
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

open class SupportSprite(private val width: Float, private val height: Float) : Entity() {
    var draw: OnSupportDraw? = null
    private var canvas: SupportCanvas? = null

    protected open fun onSupportDraw(canvas: BaseCanvas) {
        draw?.draw(canvas)
    }

    override fun doDraw(pGL: GL10, pCamera: Camera) {
        SupportState.setUsingSupportCamera(true)
        BatchEngine.pGL = pGL

        GLWrapped.blend.setBlendType(BlendType.Normal)
        GLWrapped.blend.apply()

        val camera = com.edlplan.framework.support.graphics.Camera()
        BatchEngine.setGlobalCamera(camera)

        if (canvas == null) {
            val info = SupportCanvas.SupportInfo()
            info.supportWidth = width
            info.supportHeight = height
            canvas = SupportCanvas(info)
        }
        canvas!!.prepare()
        val count = canvas!!.save()
        val count2 = canvas!!.blendSetting.save()

        onSupportDraw(canvas!!)

        canvas!!.blendSetting.restoreToCount(count2)
        canvas!!.restoreToCount(count)
        canvas!!.unprepare()

        SupportState.setUsingSupportCamera(false)
        GLHelper.blendFunction(pGL, BlendType.Normal.srcTypePreM, BlendType.Normal.dstTypePreM)
    }

    interface OnSupportDraw {
        fun draw(canvas: BaseCanvas)
    }
}