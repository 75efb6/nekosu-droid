package com.edlplan.framework.support.graphics

import com.edlplan.framework.utils.AbstractSRable

class BlendSetting : AbstractSRable<BlendProperty>() {

    fun setUp(): BlendSetting {
        initial()
        apply(getData())
        return this
    }

    private fun apply(p: BlendProperty) {
        p.applyToGL()
    }

    fun apply() {
        apply(getData())
    }

    fun isEnable(): Boolean {
        return getData().enable
    }

    fun setEnable(enable: Boolean) {
        set(enable, isPreM(), getBlendType())
    }

    fun getBlendType(): BlendType {
        return getData().blendType
    }

    fun setBlendType(type: BlendType) {
        set(isEnable(), isPreM(), type)
    }

    fun isPreM(): Boolean {
        return getData().isPreM
    }

    fun set(enable: Boolean, isPreM: Boolean, blendType: BlendType) {
        if (!getData().equals(enable, isPreM, blendType)) {
            val prop = BlendProperty(enable, isPreM, blendType)
            setCurrentData(prop)
            apply(prop)
        }
    }

    fun setIsPreM(isPreM: Boolean) {
        if (isPreM != isPreM()) {
            getData().isPreM = false
            apply()
        }
    }

    override fun onSave(t: BlendProperty) {

    }

    override fun onRestore(now: BlendProperty, pre: BlendProperty) {
        if (now != pre) {
            apply(now)
        }
    }

    override fun getDefData(): BlendProperty {
        return BlendProperty()
    }
}
