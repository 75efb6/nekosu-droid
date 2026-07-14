package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.util.modifier.IModifier

open class ModifierListener : IEntityModifier.IEntityModifierListener {
    override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {}
    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {}
}
