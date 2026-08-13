package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.entity.modifier.IEntityModifier
import ru.nsu.ccfit.zuev.osu.helper.UniversalModifier.ValueType
import java.util.LinkedList
import java.util.Queue

object ModifierFactory {
    private val instance = ModifierFactoryInternal()

    @JvmStatic
    fun newFadeInModifier(duration: Float): IEntityModifier =
        instance.newModifier(duration, 0f, 1f, ValueType.ALPHA)

    @JvmStatic
    fun newFadeOutModifier(duration: Float): IEntityModifier =
        instance.newModifier(duration, 1f, 0f, ValueType.ALPHA)

    @JvmStatic
    fun newAlphaModifier(duration: Float, from: Float, to: Float): IEntityModifier =
        instance.newModifier(duration, from, to, ValueType.ALPHA)

    @JvmStatic
    fun newScaleModifier(duration: Float, from: Float, to: Float): IEntityModifier =
        instance.newModifier(duration, from, to, ValueType.SCALE)

    @JvmStatic
    fun newDelayModifier(duration: Float): IEntityModifier =
        instance.newModifier(duration, 0f, 0f, ValueType.NONE)

    @JvmStatic
    fun putModifier(mod: UniversalModifier) {
        instance.pool.add(mod)
    }

    @JvmStatic
    fun clear() {
        instance.pool.clear()
    }

    private class ModifierFactoryInternal {
        val pool: Queue<UniversalModifier> = LinkedList()

        fun newModifier(duration: Float, from: Float, to: Float, type: ValueType): IEntityModifier {
            if (pool.isNotEmpty()) {
                var mod: UniversalModifier? = null
                synchronized(pool) {
                    if (pool.isNotEmpty()) {
                        mod = pool.poll()
                    }
                }
                if (mod != null) {
                    mod!!.init(duration, from, to, type)
                    return mod!!
                }
            }
            return UniversalModifier(duration, from, to, type)
        }
    }
}
