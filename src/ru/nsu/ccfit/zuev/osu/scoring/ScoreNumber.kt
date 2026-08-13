package ru.nsu.ccfit.zuev.osu.scoring

import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.modifier.ScaleModifier
import org.anddev.andengine.entity.modifier.SequenceEntityModifier
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

class ScoreNumber(x: Float, y: Float, text: String, scale: Float, center: Boolean) : Entity(x, y) {
    internal var height: Float = 0f

    init {
        var totalWidth = 0f
        for (i in text.indices) {
            val ch = text[i]
            val textureName: String
            val letter: Sprite

            when {
                ch in '0'..'9' -> textureName = ch.toString()
                ch == '.' || ch == ',' -> textureName = "comma"
                ch == '%' -> textureName = "percent"
                else -> textureName = "x"
            }

            letter = Sprite(
                totalWidth * scale, 0f,
                ResourceManager.getInstance().getTextureWithPrefix(OsuSkin.get().scorePrefix, textureName)
            )
            letter.setSize(letter.width * scale, letter.height * scale)

            totalWidth += letter.width * scale
            height = letter.height * scale

            attachChild(letter)
        }

        if (center) {
            totalWidth /= 2 * scale
            for (i in 0 until childCount) {
                val sp = getChild(i)
                sp.setPosition(sp.x - totalWidth, sp.y)
                sp.registerEntityModifier(
                    SequenceEntityModifier(
                        ScaleModifier(0.2f, scale, scale * 1.5f),
                        ScaleModifier(0.4f, scale * 1.5f, scale)
                    )
                )
            }
        }
    }

    fun attachToScene(scene: Scene) {
        scene.attachChild(this)
    }

    fun detachFromScene(scene: Scene) {
        scene.detachChild(this)
    }

    fun getHeight(): Float = height
}
