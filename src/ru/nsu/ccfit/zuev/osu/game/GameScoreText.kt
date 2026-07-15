package ru.nsu.ccfit.zuev.osu.game

import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.skins.StringSkinData

class GameScoreText(
    prefix: StringSkinData,
    x: Float,
    y: Float,
    mask: String,
    scale: Float
) {
    private val letters: Array<AnimSprite>
    private val characters: Map<Char, AnimSprite?>
    private val digits: MutableList<AnimSprite> = mutableListOf()
    private val scale: Float
    private val hasX: Boolean
    val digitWidth: Float

    init {
        var scoreComma: AnimSprite? = null
        var scorePercent: AnimSprite? = null
        var scoreX: AnimSprite? = null
        digitWidth = ResourceManager.getInstance().getTextureWithPrefix(prefix, "0")!!.getWidth().toFloat()
        @Suppress("UNCHECKED_CAST")
        letters = arrayOfNulls<AnimSprite>(mask.length) as Array<AnimSprite>
        var width = 0f

        for (i in mask.indices) {
            when (mask[i]) {
                '0' -> {
                    letters[i] = AnimSprite(x + width, y, prefix, null, 10, 0f)
                    digits.add(letters[i])
                }
                '.' -> {
                    letters[i] = AnimSprite(x + width, y, prefix, "comma", 1, 0f)
                    scoreComma = letters[i]
                }
                '%' -> {
                    letters[i] = AnimSprite(x + width, y, prefix, "percent", 1, 0f)
                    scorePercent = letters[i]
                }
                else -> {
                    letters[i] = AnimSprite(x + width, y, prefix, "x", 1, 0f)
                    scoreX = letters[i]
                }
            }
            letters[i].setSize(letters[i].getWidth() * scale, letters[i].getHeight() * scale)
            width += letters[i].getWidth()
        }
        this.scale = scale
        this.hasX = mask.last() == 'x'
        this.characters = mapOf('.' to scoreComma, '%' to scorePercent, 'x' to scoreX)
    }

    fun changeText(text: String) {
        var j = 0
        var totalWidth = 0f
        val digitsSize = digits.size

        for (i in text.indices) {
            if (j >= digitsSize) break
            val digit = digits[j]
            val ch = text[i]

            when {
                ch in '0'..'9' -> {
                    digit.setVisible(true)
                    digit.setFrame(ch - '0')
                    digit.setWidth(digit.getFrameWidth() * scale)
                    digit.setPosition(digits[0].getX() + totalWidth, digit.getY())
                    totalWidth += digit.getWidth()
                    j++
                }
                ch == '*' -> {
                    digit.setVisible(false)
                    j++
                }
                else -> {
                    val sprite = characters[ch]
                    if (sprite != null) {
                        sprite.setPosition(digits[0].getX() + totalWidth, sprite.getY())
                        totalWidth += sprite.getWidth()
                    }
                }
            }
        }
        if (hasX) {
            letters[letters.size - 1].setPosition(
                digits[0].getX() + totalWidth,
                letters[letters.size - 1].getY()
            )
        }
    }

    fun attachToScene(scene: Scene) {
        for (sp in letters) {
            scene.attachChild(sp, 0)
        }
    }

    fun detachFromScene() {
        for (sp in letters) {
            sp.detachSelf()
        }
    }

    fun setPosition(x: Float, y: Float) {
        var width = 0f
        for (sp in letters) {
            sp.setPosition(x + width, y)
            width += sp.getWidth()
        }
    }
}
