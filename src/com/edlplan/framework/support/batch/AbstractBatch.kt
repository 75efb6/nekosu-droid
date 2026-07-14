package com.edlplan.framework.support.batch

import com.edlplan.framework.support.SupportState
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

abstract class AbstractBatch<T> {

    protected abstract fun onBind()

    protected abstract fun onUnbind()

    abstract fun add(t: T)

    protected abstract fun clearData()

    protected abstract fun applyToGL(): Boolean

    protected fun checkForBind() {
        if (!isBind()) {
            bind()
        }
    }

    fun bind() {
        BatchEngine.bind(this)
        onBind()
    }

    fun unbind() {
        BatchEngine.unbind(this)
        onUnbind()
    }

    fun isBind(): Boolean {
        return BatchEngine.currentBatch() === this
    }

    fun flush() {
        val pGL: GL10 = BatchEngine.pGL!!
        val type = GLHelper.getCurrentMatrixType()
        if (SupportState.isUsingSupportCamera()) {
            pGL.glMatrixMode(GL10.GL_PROJECTION)
            pGL.glPushMatrix()
            pGL.glMultMatrixf(BatchEngine.shaderGlobals.camera.getProjectionMatrix().data, 0)

            pGL.glMatrixMode(GL10.GL_MODELVIEW)
            pGL.glPushMatrix()
            pGL.glMultMatrixf(BatchEngine.shaderGlobals.camera.getMaskMatrix().data, 0)
        }
        if (applyToGL()) {
            clearData()
        }
        if (SupportState.isUsingSupportCamera()) {
            pGL.glMatrixMode(GL10.GL_PROJECTION)
            pGL.glPopMatrix()
            pGL.glMatrixMode(GL10.GL_MODELVIEW)
            pGL.glPopMatrix()
            pGL.glMatrixMode(type)
        }
    }
}
