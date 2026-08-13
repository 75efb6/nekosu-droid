package ru.nsu.ccfit.zuev.osu.game

import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.skins.OsuSkin

class GameScoreTextShadow(
    x: Float,
    y: Float,
    mask: String,
    scale: Float,
    private val comboText: GameScoreText
) : GameObject() {

    private val letters: Array<AnimSprite>
    private val digits: MutableList<AnimSprite> = mutableListOf()
    private var hasX = false
    private var text = "0****"

    init {
        @Suppress("UNCHECKED_CAST")
        letters = arrayOfNulls<AnimSprite>(mask.length) as Array<AnimSprite>
        var width = 0f
        val prefix = OsuSkin.get().getComboPrefix()

        for (i in mask.indices) {
            when (mask[i]) {
                '0' -> {
                    letters[i] = AnimSprite(x + width, y, prefix, null, 10, 0f)
                    digits.add(letters[i])
                }
                '.' -> letters[i] = AnimSprite(x + width, y, prefix, "comma", 1, 0f)
                '%' -> letters[i] = AnimSprite(x + width, y, prefix, "percent", 1, 0f)
                else -> {
                    letters[i] = AnimSprite(x + width, y, prefix, "x", 1, 0f)
                    hasX = true
                }
            }
            letters[i].setSize(letters[i].getWidth() * scale, letters[i].getHeight() * scale)
            width += letters[i].getWidth()
            letters[i].setAlpha(0f)
            if (i == 0) {
                // root entity
            } else {
                letters[0].attachChild(letters[i])
            }
        }
    }

    fun changeText(text: String) {
        if (text == this.text) return
        var j = 0
        var digitsWidth = 0f
        val textLength = text.length
        val digitsSize = digits.size

        for (i in 0 until textLength) {
            if (j >= digitsSize) break
            val digit = digits[j]
            val ch = text[i]

            when {
                ch in '0'..'9' -> {
                    digit.setVisible(true)
                    digit.setFrame(ch - '0')
                    digitsWidth += digit.getWidth()
                    j++
                }
                ch == '*' -> {
                    digit.setVisible(false)
                    j++
                }
            }
        }
        if (hasX) {
            letters[letters.size - 1].setPosition(
                digits[0].getX() + digitsWidth,
                letters[letters.size - 1].getY()
            )
        }
        comboText.changeText(this.text)
        this.text = text

        letters[0].setAlpha(0.6f)
    }

    fun attachToScene(scene: Scene) {
        scene.attachChild(letters[0], 0)
    }

    fun detachFromScene() {
        letters[0].detachSelf()
    }

    override fun update(dt: Float) {
        if (letters[0].getAlpha() > 0) {
            var alpha = letters[0].getAlpha() - dt * 2.5f
            if (alpha < 0) alpha = 0f

            letters[0].setScale(1.5f - Math.abs(0.6f - alpha))
            letters[0].setPosition(20f, Config.getRES_HEIGHT() - letters[0].getHeightScaled() - 20)
            for (sp in letters) {
                sp.setAlpha(alpha)
            }

            if (alpha == 0f) {
                comboText.changeText(text)
            }
        } else {
            comboText.changeText(text)
        }
    }

    fun registerEntityModifier(modifier: IEntityModifier) {
        letters[0].registerEntityModifier(modifier)
    }
}
