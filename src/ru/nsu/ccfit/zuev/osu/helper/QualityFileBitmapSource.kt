package ru.nsu.ccfit.zuev.osu.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.StreamUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class QualityFileBitmapSource : BaseTextureAtlasSource, IBitmapTextureAtlasSource {

    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var bitmap: Bitmap? = null

    private val fileBitmapInput: InputFactory
    private var inSampleSize: Int = 1

    constructor(pFile: File) : this(pFile, 0, 0)

    constructor(pFile: InputFactory) : this(pFile, 0, 0, 1)

    constructor(pFile: File, inSampleSize: Int) : this({ FileInputStream(pFile) }, 0, 0, inSampleSize)

    constructor(pFile: File, pTexturePositionX: Int, pTexturePositionY: Int) : this({ FileInputStream(pFile) }, pTexturePositionX, pTexturePositionY, 1)

    constructor(
        pFile: InputFactory,
        pTexturePositionX: Int,
        pTexturePositionY: Int,
        inSampleSize: Int
    ) : super(pTexturePositionX, pTexturePositionY) {
        fileBitmapInput = pFile
        this.inSampleSize = inSampleSize
        val decodeOptions = BitmapFactory.Options()
        decodeOptions.inJustDecodeBounds = true
        decodeOptions.inSampleSize = inSampleSize
        var inputStream: InputStream? = null
        try {
            inputStream = openInputStream()
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            this.mWidth = decodeOptions.outWidth
            this.mHeight = decodeOptions.outHeight
        } catch (e: IOException) {
            Debug.e("Failed loading Bitmap in FileBitmapTextureAtlasSource. File: $pFile", e)
            this.mWidth = 0
            this.mHeight = 0
        } finally {
            StreamUtils.close(inputStream)
        }
    }

    internal constructor(
        pFile: InputFactory, pTexturePositionX: Int, pTexturePositionY: Int,
        pWidth: Int, pHeight: Int
    ) : super(pTexturePositionX, pTexturePositionY) {
        fileBitmapInput = pFile
        this.mWidth = pWidth
        this.mHeight = pHeight
    }

    @Throws(IOException::class)
    fun openInputStream(): InputStream = fileBitmapInput.openInput()

    fun deepCopy(): QualityFileBitmapSource {
        val source = QualityFileBitmapSource(this.fileBitmapInput, this.mTexturePositionX,
            this.mTexturePositionY, this.mWidth, this.mHeight)
        source.inSampleSize = inSampleSize
        return source
    }

    override fun getWidth(): Int = this.mWidth

    override fun getHeight(): Int = this.mHeight

    override fun getWidth(): Int = this.mWidth

    override fun getHeight(): Int = this.mHeight
        bitmap = onLoadBitmap(Bitmap.Config.ARGB_8888)
        return bitmap != null
    }

    fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
        if (bitmap != null) {
            val bmp = bitmap
            bitmap = null
            return bmp
        }
        val decodeOptions = BitmapFactory.Options()
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
        decodeOptions.inSampleSize = inSampleSize
        var inputStream: InputStream? = null
        return try {
            inputStream = openInputStream()
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } catch (e: IOException) {
            Debug.e("Failed loading Bitmap in ${this.javaClass.simpleName}. File: $fileBitmapInput", e)
            null
        } finally {
            StreamUtils.close(inputStream)
        }
    }

    override fun toString(): String = "${this.javaClass.simpleName}($fileBitmapInput)"

    fun interface InputFactory {
        @Throws(IOException::class)
        fun openInput(): InputStream
    }
}
