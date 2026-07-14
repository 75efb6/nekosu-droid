package com.edlplan.framework.support.graphics

import android.opengl.GLES10
import com.edlplan.framework.support.batch.BatchEngine
import com.edlplan.framework.utils.interfaces.Copyable

class BlendProperty : Copyable<BlendProperty> {

    var enable: Boolean = true

    var isPreM: Boolean = false

    var blendType: BlendType = BlendType.Normal

    constructor()

    constructor(b: BlendProperty) {
        set(b)
    }

    constructor(e: Boolean, isPreM: Boolean, t: BlendType) {
        this.isPreM = isPreM
        this.enable = e
        this.blendType = t
    }

    fun set(b: BlendProperty) {
        this.enable = b.enable
        this.blendType = b.blendType
        this.isPreM = b.isPreM
    }

    fun applyToGL() {
        BatchEngine.flush()
        if (enable) {
            GLES10.glEnable(GLES10.GL_BLEND)
            if (isPreM) {
                GLES10.glBlendFunc(blendType.srcTypePreM, blendType.dstTypePreM)
            } else {
                GLES10.glBlendFunc(blendType.srcType, blendType.dstType)
            }
        } else {
            GLES10.glDisable(GLES10.GL_BLEND)
        }
    }

    fun equals(_enable: Boolean, isPreM: Boolean, _blendType: BlendType): Boolean {
        return this.enable == _enable && this.blendType == _blendType && this.isPreM == isPreM
    }

    override fun equals(other: Any?): Boolean {
        if (other is BlendProperty) {
            return (enable == other.enable) && (blendType == other.blendType) && (isPreM == other.isPreM)
        } else return false
    }

    override fun copy(): BlendProperty {
        return BlendProperty(this)
    }
}
