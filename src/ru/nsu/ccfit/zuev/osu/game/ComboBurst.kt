package ru.nsu.ccfit.zuev.osu.game

import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.DelayModifier
import org.anddev.andengine.entity.modifier.FadeInModifier
import org.anddev.andengine.entity.modifier.FadeOutModifier
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.entity.modifier.MoveXModifier
import org.anddev.andengine.entity.modifier.ParallelEntityModifier
import org.anddev.andengine.entity.modifier.SequenceEntityModifier
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.ease.EaseSineOut
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

class ComboBurst(private val rightX: Float, private val bottomY: Float) {

    private val comboBursts: MutableList<Sprite> = mutableListOf()
    private val comboBurstVocals: MutableList<BassSoundProvider> = mutableListOf()

    private var nextKeyComboNum = 0
    private var fromX = 0f
    private var nextShowId = 0
    private var nextSoundId = 0

    init {
        breakCombo()

        val globalTex = ResourceManager.getInstance().getTexture("comboburst")
        if (globalTex != null) {
            val sprite = Sprite(0f, 0f, globalTex)
            sprite.setAlpha(0f)
            sprite.setIgnoreUpdate(true)
            comboBursts.add(sprite)
        }
        val sound = ResourceManager.getInstance().getSound("comboburst")
        if (sound != null) {
            comboBurstVocals.add(sound)
        }
        for (i in 0 until 10) {
            val tex = ResourceManager.getInstance().getTexture("comboburst-$i")
            if (tex != null) {
                val sprite = Sprite(0f, 0f, tex)
                sprite.setAlpha(0f)
                sprite.setIgnoreUpdate(true)
                comboBursts.add(sprite)
            }
            val s = ResourceManager.getInstance().getSound("comboburst-$i")
            if (s != null) {
                comboBurstVocals.add(s)
            }
        }
    }

    fun checkAndShow(currentCombo: Int) {
        if (Config.isComboburst() && currentCombo >= nextKeyComboNum) {
            if (comboBurstVocals.isNotEmpty()) {
                comboBurstVocals[nextSoundId].play(0.8f)
            }
            if (comboBursts.isNotEmpty()) {
                val sprite = comboBursts[nextShowId]
                val toX: Float = if (fromX > 0) {
                    fromX - sprite.getWidth()
                } else {
                    fromX = -sprite.getWidth()
                    0f
                }
                sprite.setIgnoreUpdate(false)
                sprite.setPosition(fromX, bottomY - sprite.getHeight())
                sprite.registerEntityModifier(
                    SequenceEntityModifier(
                        object : IEntityModifier.IEntityModifierListener {
                            override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {}
                            override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                pItem.setAlpha(0f)
                                pItem.setIgnoreUpdate(true)
                            }
                        },
                        ParallelEntityModifier(
                            MoveXModifier(0.5f, fromX, toX, EaseSineOut.getInstance()),
                            FadeInModifier(0.5f)
                        ),
                        DelayModifier(1.0f),
                        ParallelEntityModifier(
                            MoveXModifier(0.5f, toX, fromX, EaseSineOut.getInstance()),
                            FadeOutModifier(0.5f)
                        )
                    )
                )
            }

            if (comboBursts.isNotEmpty()) {
                nextShowId = (nextShowId + 1) % comboBursts.size
            }
            if (comboBurstVocals.isNotEmpty()) {
                nextSoundId = (nextSoundId + 1) % comboBurstVocals.size
            }
            when (nextKeyComboNum) {
                30 -> {
                    nextKeyComboNum = 60
                    fromX = rightX
                }
                60 -> {
                    nextKeyComboNum = 100
                    fromX = -1f
                }
                else -> {
                    nextKeyComboNum += 100
                    val mod = nextKeyComboNum / 100
                    fromX = if (mod % 2 == 0) rightX else -1f
                }
            }
        }
    }

    fun breakCombo() {
        fromX = 0f
        nextKeyComboNum = 30
    }

    fun attachAll(scene: Scene) {
        for (sprite in comboBursts) {
            scene.attachChild(sprite)
        }
    }

    fun detachAll() {
        for (sprite in comboBursts) {
            sprite.detachSelf()
        }
    }
}
