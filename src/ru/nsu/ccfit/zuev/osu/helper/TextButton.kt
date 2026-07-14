package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.ResourceManager

open class TextButton @JvmOverloads constructor(
    font: Font,
    text: String,
    scale: Float = 1.0f
) : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("button")) {

    private val buttonText: ChangeableText

    init {
        buttonText = ChangeableText(0f, 0f, font, text, 50)
        buttonText.setScale(scale)
        setColor(201 / 255f, 31 / 255f, 55 / 255f)
        this.width = buttonText.widthScaled + 80
        this.height = buttonText.heightScaled + 20
        val textX = (this.width - buttonText.width) / 2
        val textY = (this.height - buttonText.height) / 2
        buttonText.setPosition(textX, textY)
        alpha = 0.7f
        attachChild(buttonText)
    }

    override fun setWidth(pWidth: Float) {
        this.mWidth = pWidth
        val textX = (this.width - buttonText.width) / 2
        val textY = (this.height - buttonText.height) / 2
        buttonText.setPosition(textX, textY)
        this.updateVertexBuffer()
    }

    override fun setHeight(pHeight: Float) {
        this.mHeight = pHeight
        val textX = (this.width - buttonText.width) / 2
        val textY = (this.height - buttonText.height) / 2
        buttonText.setPosition(textX, textY)
        this.updateVertexBuffer()
    }

    fun setTextColor(pRed: Float, pGreen: Float, pBlue: Float) {
        buttonText.setColor(pRed, pGreen, pBlue)
    }

    fun setText(text: String) {
        buttonText.text = text
        val textX = (this.width - buttonText.width) / 2
        val textY = (this.height - buttonText.height) / 2
        buttonText.setPosition(textX, textY)
    }
}
