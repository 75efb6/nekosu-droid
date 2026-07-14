package com.edlplan.framework.support.batch.`object`

import com.edlplan.framework.support.batch.AbstractBatch
import com.edlplan.framework.support.batch.BatchEngine
import com.edlplan.framework.support.graphics.GLWrapped
import com.edlplan.framework.support.util.BufferUtil
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.util.GLHelper
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

class TextureQuadBatch private constructor(size: Int) : AbstractBatch<ATextureQuad>() {

    private val maxArySize: Int
    private val buffer: FloatBuffer
    private val indicesBuffer: ShortBuffer
    private val ary: FloatArray
    private var offset: Int = 0
    private var bindTexture: ITexture? = null

    init {
        if (size > Short.MAX_VALUE / 4 - 10) {
            throw IllegalArgumentException("过大的QuadBatch")
        }
        maxArySize = size * SIZE_PER_QUAD
        ary = FloatArray(maxArySize)
        buffer = BufferUtil.createFloatBuffer(maxArySize)
        indicesBuffer = BufferUtil.createShortBuffer(size * 6)
        val list = ShortArray(size * 6)
        val l = list.size
        var j: Short = 0
        var i = 0
        while (i < l) {
            list[i] = j++
            list[i + 1] = j
            list[i + 3] = j++
            list[i + 2] = j
            list[i + 5] = j++
            list[i + 4] = j++
            i += 6
        }
        indicesBuffer.put(list)
        indicesBuffer.position(0)
    }

    override fun onBind() {

    }

    override fun onUnbind() {

    }

    override fun add(textureQuad: ATextureQuad) {
        if (textureQuad.texture == null) {
            return
        }

        if (!isBind()) {
            bind()
        }

        if (textureQuad.texture!!.getTexture() !== bindTexture) {
            flush()
            bindTexture = textureQuad.texture!!.getTexture()
        }

        textureQuad.write(ary, offset)
        offset += SIZE_PER_QUAD

        if (offset == maxArySize) {
            flush()
        }
    }

    override fun clearData() {
        offset = 0
        buffer.position(0)
        buffer.limit(maxArySize)
    }

    override fun applyToGL(): Boolean {
        if (offset != 0) {
            GLWrapped.blend.setIsPreM(bindTexture!!.getTextureOptions().mPreMultipyAlpha)

            val pGL: GL10 = BatchEngine.pGL!!
            bindTexture!!.bind(pGL)
            GLHelper.enableTextures(pGL)
            GLHelper.enableTexCoordArray(pGL)
            pGL.glEnableClientState(GL10.GL_COLOR_ARRAY)
            pGL.glShadeModel(GL10.GL_SMOOTH)
            GLHelper.enableVertexArray(pGL)
            GLHelper.disableCulling(pGL)

            buffer.position(0)
            buffer.put(ary, 0, offset)
            buffer.position(0).limit(offset)

            pGL.glVertexPointer(2, GL10.GL_FLOAT, STEP, buffer)
            buffer.position(OFFSET_COORD)
            pGL.glTexCoordPointer(2, GL10.GL_FLOAT, STEP, buffer)
            buffer.position(OFFSET_COLOR)
            pGL.glColorPointer(4, GL10.GL_FLOAT, STEP, buffer)

            pGL.glDrawElements(
                GL10.GL_TRIANGLES,
                offset / SIZE_PER_QUAD * 6,
                GL10.GL_UNSIGNED_SHORT,
                indicesBuffer
            )

            pGL.glDisableClientState(GL10.GL_COLOR_ARRAY)

            return true
        } else {
            return false
        }
    }

    companion object {
        private const val SIZE_PER_QUAD = 4 * 8
        private const val STEP = (2 + 2 + 4) * 4
        private const val OFFSET_COORD = 2
        private const val OFFSET_COLOR = OFFSET_COORD + 2

        private var defaultBatch: TextureQuadBatch? = null

        @JvmStatic
        fun getDefaultBatch(): TextureQuadBatch {
            if (defaultBatch == null) {
                defaultBatch = TextureQuadBatch(1023)
            }
            return defaultBatch!!
        }
    }

}
