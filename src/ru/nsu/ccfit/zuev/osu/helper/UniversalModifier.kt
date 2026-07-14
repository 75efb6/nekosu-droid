package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.SingleValueSpanEntityModifier

class UniversalModifier : SingleValueSpanEntityModifier {
    var type: ValueType

    constructor(duration: Float, from: Float, to: Float, type: ValueType) : super(duration, from, to) {
        this.type = type
    }

    constructor(modifier: UniversalModifier) : super(modifier) {
        this.type = modifier.type
    }

    override fun onSetInitialValue(pItem: IEntity, pValue: Float) {
        when (type) {
            ValueType.ALPHA -> pItem.setAlpha(pValue)
            ValueType.SCALE -> pItem.setScale(pValue)
            else -> {}
        }
    }

    override fun onSetValue(pItem: IEntity, pPercentageDone: Float, pValue: Float) {
        when (type) {
            ValueType.ALPHA -> pItem.setAlpha(pValue)
            ValueType.SCALE -> pItem.setScale(pValue)
            else -> {}
        }
    }

    override fun deepCopy(): UniversalModifier = UniversalModifier(this)

    fun init(duration: Float, from: Float, to: Float, type: ValueType) {
        reset()
        mDuration = duration
        mFromValue = from
        mValueSpan = to - from
        this.type = type
    }

    override fun onModifierFinished(pItem: IEntity) {
        super.onModifierFinished(pItem)
        this.mModifierListeners.clear()
        ModifierFactory.putModifier(this)
    }

    enum class ValueType {
        NONE, ALPHA, SCALE
    }
}
