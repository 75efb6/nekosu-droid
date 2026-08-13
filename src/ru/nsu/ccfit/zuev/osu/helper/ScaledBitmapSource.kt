package ru.nsu.ccfit.zuev.osu.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.StreamUtils
import ru.nsu.ccfit.zuev.osu.Config
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class ScaledBitmapSource : BaseTextureAtlasSource, IBitmapTextureAtlasSource {

    private val mFile: File
    private var mWidth: Int
    private var mHeight: Int
    private var bitmap: Bitmap? = null

    constructor(pFile: File) : this(pFile, 0, 0)

    constructor(pFile: File, pTexturePositionX: Int, pTexturePositionY: Int) : super(pTexturePositionX, pTexturePositionY) {
        this.mFile = pFile
        val decodeOptions = BitmapFactory.Options()
        decodeOptions.inJustDecodeBounds = true
        decodeOptions.inSampleSize = Config.getBackgroundQuality()
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(pFile)
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
        pFile: File, pTexturePositionX: Int, pTexturePositionY: Int,
        pWidth: Int, pHeight: Int
    ) : super(pTexturePositionX, pTexturePositionY) {
        this.mFile = pFile
        this.mWidth = pWidth
        this.mHeight = pHeight
    }

    fun clone(): ScaledBitmapSource {
        return ScaledBitmapSource(this.mFile, this.mTexturePositionX,
            this.mTexturePositionY, this.mWidth, this.mHeight)
    }

    override fun getWidth(): Int = this.mWidth

    override fun getHeight(): Int = this.mHeight

    fun preload(): Boolean {
        bitmap = onLoadBitmap(Bitmap.Config.ARGB_8888)
        return bitmap != null
    }

    override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
        if (bitmap != null) {
            val bmp = bitmap
            bitmap = null
            return bmp
        }
        val decodeOptions = BitmapFactory.Options()
        decodeOptions.inPreferredConfig = pBitmapConfig
        decodeOptions.inSampleSize = Config.getBackgroundQuality()
        var inputStream: InputStream? = null
        return try {
            inputStream = FileInputStream(this.mFile)
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } catch (e: IOException) {
            Debug.e("Failed loading Bitmap in ${this.javaClass.simpleName}. File: $this.mFile", e)
            null
        } finally {
            StreamUtils.close(inputStream)
        }
    }

    override fun toString(): String = "${this.javaClass.simpleName}($mFile)"

    override fun deepCopy(): ScaledBitmapSource = ScaledBitmapSource(mFile, mTexturePositionX, mTexturePositionY)
}
