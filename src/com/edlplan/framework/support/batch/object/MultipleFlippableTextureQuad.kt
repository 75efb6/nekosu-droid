package com.edlplan.framework.support.batch.`object`

import com.edlplan.framework.math.Vec2
import org.anddev.andengine.opengl.texture.region.TextureRegion

class MultipleFlippableTextureQuad : FlippableTextureQuad() {

    var textureEntries: Array<TextureEntry>? = null

    var currentEntry: TextureEntry? = null

    fun switchTexture(id: Int) {
        if (textureEntries!!.isEmpty()) {
            throw RuntimeException()
        }
        val idx = id % textureEntries!!.size
        currentEntry = textureEntries!![idx]
        texture = currentEntry!!.texture
        size.set(currentEntry!!.size)
        u1 = currentEntry!!.u1
        v1 = currentEntry!!.v1
        u2 = currentEntry!!.u2
        v2 = currentEntry!!.v2
    }

    fun initialWithTextureList(textures: List<TextureRegion>) {
        textureEntries = arrayOfNulls<TextureEntry>(textures.size) as Array<TextureEntry>
        if (textureEntries!!.isEmpty()) {
            return
        }

        for (i in textureEntries!!.indices) {
            val texture = textures[i]
            val entry = TextureEntry()
            entry.texture = texture
            entry.size = Vec2(texture.getWidth().toFloat(), texture.getHeight().toFloat())
            entry.u1 = texture.getTextureCoordinateX1()
            entry.u2 = texture.getTextureCoordinateX2()
            entry.v1 = texture.getTextureCoordinateY1()
            entry.v2 = texture.getTextureCoordinateY2()
            textureEntries!![i] = entry
        }

        switchTexture(0)
    }

    fun initialWithTextureListWithScale(textures: List<TextureRegion>, globalScale: Float) {
        textureEntries = arrayOfNulls<TextureEntry>(textures.size) as Array<TextureEntry>
        if (textureEntries!!.isEmpty()) {
            return
        }

        for (i in textureEntries!!.indices) {
            val texture = textures[i]
            val entry = TextureEntry()
            entry.texture = texture
            entry.size = Vec2(texture.getWidth() * globalScale, texture.getHeight() * globalScale)
            entry.u1 = texture.getTextureCoordinateX1()
            entry.u2 = texture.getTextureCoordinateX2()
            entry.v1 = texture.getTextureCoordinateY1()
            entry.v2 = texture.getTextureCoordinateY2()
            textureEntries!![i] = entry
        }

        switchTexture(0)
    }

    inner class TextureEntry {
        var size: Vec2 = Vec2()
        var texture: TextureRegion? = null
        var u1: Float = 0f
        var v1: Float = 0f
        var u2: Float = 0f
        var v2: Float = 0f
    }

}
