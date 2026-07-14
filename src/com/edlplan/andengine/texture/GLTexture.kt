package com.edlplan.andengine.texture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import com.edlplan.framework.math.Color4
import com.edlplan.framework.math.IQuad
import com.edlplan.framework.math.Quad
import com.edlplan.framework.math.RectF
import com.edlplan.framework.math.Vec2
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class GLTexture : AbstractTexture() {

    private var width = 0
    private var height = 0

    var realWidth = 0
        private set
    var realHeight = 0
        private set

    var glWidth = 0f
        private set
    var glHeight = 0f
        private set

    var textureId = 0
        private set

    private var recycled = false

    private var rawQuad: Quad? = null

    override fun getRawQuad(): IQuad {
        return rawQuad!!
    }

    protected fun endCreate() {
        rawQuad = RectF.xywh(0f, 0f, glWidth, glHeight).toQuad()
    }

    override fun getTexture(): GLTexture {
        return this
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun getTextureId(): Int {
        return textureId
    }

    fun bind(loc: Int) {
        bindGl(glTexIndex[loc])
    }

    private fun bindGl(i: Int) {
        GLES20.glActiveTexture(i)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    override fun toTexturePosition(x: Float, y: Float): Vec2 {
        return Vec2(glWidth * x / width, glHeight * y / height)
    }

    fun delete() {
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        recycled = true
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        if (!recycled) delete()
        super.finalize()
    }

    override fun hashCode(): Int {
        return textureId
    }

    override fun equals(obj: Any?): Boolean {
        return super.equals(obj)
    }

    companion object {
        @JvmField
        val glTexIndex = intArrayOf(
            GLES20.GL_TEXTURE0,
            GLES20.GL_TEXTURE1,
            GLES20.GL_TEXTURE2,
            GLES20.GL_TEXTURE3,
            GLES20.GL_TEXTURE4,
            GLES20.GL_TEXTURE5,
            GLES20.GL_TEXTURE6,
            GLES20.GL_TEXTURE7,
            GLES20.GL_TEXTURE8,
            GLES20.GL_TEXTURE9,
            GLES20.GL_TEXTURE10,
            GLES20.GL_TEXTURE11,
            GLES20.GL_TEXTURE12,
            GLES20.GL_TEXTURE13,
            GLES20.GL_TEXTURE14,
            GLES20.GL_TEXTURE15,
            GLES20.GL_TEXTURE16,
            GLES20.GL_TEXTURE17,
            GLES20.GL_TEXTURE18
        )

        lateinit var DEF_CREATE_OPTIONS: BitmapFactory.Options

        @JvmField
        var White: GLTexture? = null

        @JvmField
        var Alpha: GLTexture? = null

        @JvmField
        var Black: GLTexture? = null

        @JvmField
        var Red: GLTexture? = null

        @JvmField
        var Blue: GLTexture? = null

        @JvmField
        var Yellow: GLTexture? = null

        @JvmField
        var ErrorTexture: GLTexture? = null

        @JvmField
        var poor_font_mode = false

        @JvmField
        var SCALE_22 = true

        init {
            initial()
        }

        @JvmStatic
        fun createGPUTexture(w: Int, h: Int): GLTexture {
            val t = IntArray(1)
            GLES20.glGenTextures(1, t, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            val tex = GLTexture()
            tex.width = w
            tex.height = h
            tex.textureId = t[0]
            tex.glHeight = 1f
            tex.glWidth = 1f
            tex.endCreate()
            return tex
        }

        @JvmStatic
        fun createGPUAlphaTexture(w: Int, h: Int): GLTexture {
            val t = IntArray(1)
            GLES20.glGenTextures(1, t, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, w, h, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            val tex = GLTexture()
            tex.width = w
            tex.height = h
            tex.textureId = t[0]
            tex.glHeight = 1f
            tex.glWidth = 1f
            tex.endCreate()
            return tex
        }

        @JvmStatic
        fun createNotChecked(bmp: Bitmap, w: Int, h: Int): GLTexture {
            val tex = GLTexture()
            tex.textureId = createTexture()
            tex.width = w
            tex.height = h
            tex.glHeight = tex.height.toFloat() / bmp.height
            tex.glWidth = tex.width.toFloat() / bmp.width
            tex.endCreate()
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            return tex
        }

        @JvmStatic
        fun decodeStream(`in`: InputStream): GLTexture? {
            val bmp = BitmapFactory.decodeStream(`in`, null, DEF_CREATE_OPTIONS)
            return create(bmp, true)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun decodeStream(`in`: InputStream, ifClose: Boolean): GLTexture? {
            val t = decodeStream(`in`)
            if (ifClose) `in`.close()
            return t
        }

        @JvmStatic
        @Throws(FileNotFoundException::class, IOException::class)
        fun decodeFile(f: File): GLTexture? {
            return decodeStream(FileInputStream(f), true)
        }

        @JvmStatic
        fun create1pxTexture(color: Color4): GLTexture {
            val bmp = Bitmap.createBitmap(intArrayOf(color.toIntBit()), 1, 1, Bitmap.Config.ARGB_8888)
            return create(bmp, true)!!
        }

        @JvmStatic
        fun create(bmp: Bitmap?, ifDispos: Boolean): GLTexture? {
            val t = create(bmp)
            if (ifDispos) bmp?.recycle()
            return t
        }

        @JvmStatic
        fun create(bmp: Bitmap?): GLTexture? {
            if (bmp == null) return null

            var w = 1
            var h = 1
            while (w < bmp.width) w *= 2
            while (h < bmp.height) h *= 2

            w = bmp.width
            h = bmp.height
            val nb = Bitmap.createBitmap(w, h, if (poor_font_mode) Bitmap.Config.ALPHA_8 else Bitmap.Config.ARGB_8888)
            val c = Canvas(nb)
            var tex: GLTexture
            c.drawColor(0x00000000)
            val p = Paint()
            p.isAntiAlias = false
            c.drawBitmap(bmp, 0f, 0f, p)
            tex = createNotChecked(nb, bmp.width, bmp.height)
            nb.recycle()
            return tex
        }

        private fun createTexture(): Int {
            val t = IntArray(1)
            GLES20.glGenTextures(1, t, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0])
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT.toFloat())
            return t[0]
        }

        @JvmStatic
        fun createErrTexture(): GLTexture {
            val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            for (x in 0 until bmp.width) {
                for (y in 0 until bmp.height) {
                    if ((x / 32 + y / 32) % 2 == 0) {
                        bmp.setPixel(x, y, Color.argb(255, 10, 5, 5))
                    } else {
                        bmp.setPixel(x, y, Color.argb(255, 110, 40, 50))
                    }
                }
            }
            return create(bmp, true)!!
        }

        @JvmStatic
        fun initial() {
            DEF_CREATE_OPTIONS = BitmapFactory.Options()
            DEF_CREATE_OPTIONS.inPremultiplied = true
            White = create1pxTexture(Color4.White)
            Alpha = create1pxTexture(Color4.Alpha)
            Black = create1pxTexture(Color4.Black)
            Red = create1pxTexture(Color4.Red)
            Blue = create1pxTexture(Color4.Blue)
            Yellow = create1pxTexture(Color4.Yellow)
            ErrorTexture = createErrTexture()
        }
    }
}
