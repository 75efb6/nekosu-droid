package ru.nsu.ccfit.zuev.osu.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.source.BaseTextureAtlasSource
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.StreamUtils
import ru.nsu.ccfit.zuev.osu.Config
import java.io.IOException
import java.io.InputStream

class QualityAssetBitmapSource : BaseTextureAtlasSource, IBitmapTextureAtlasSource {

    private val mWidth: Int
    private val mHeight: Int
    private val mAssetPath: String
    private val mContext: Context
    private var bitmap: Bitmap? = null

    constructor(pContext: Context, pAssetPath: String) : this(pContext, pAssetPath, 0, 0)

    constructor(
        pContext: Context,
        pAssetPath: String,
        pTexturePositionX: Int,
        pTexturePositionY: Int
    ) : super(pTexturePositionX, pTexturePositionY) {
        this.mContext = pContext
        this.mAssetPath = pAssetPath
        val decodeOptions = BitmapFactory.Options()
        decodeOptions.inJustDecodeBounds = true
        decodeOptions.inSampleSize = Config.getTextureQuality()
        var inputStream: InputStream? = null
        try {
            inputStream = pContext.assets.open(pAssetPath)
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } catch (e: IOException) {
            // Debug.e("Failed loading Bitmap in AssetBitmapTextureAtlasSource. AssetPath: $pAssetPath", e)
        } finally {
            StreamUtils.close(inputStream)
        }
        this.mWidth = decodeOptions.outWidth
        this.mHeight = decodeOptions.outHeight
    }

    internal constructor(
        pContext: Context, pAssetPath: String,
        pTexturePositionX: Int, pTexturePositionY: Int,
        pWidth: Int, pHeight: Int
    ) : super(pTexturePositionX, pTexturePositionY) {
        this.mContext = pContext
        this.mAssetPath = pAssetPath
        this.mWidth = pWidth
        this.mHeight = pHeight
    }

    fun deepCopy(): QualityAssetBitmapSource {
        return QualityAssetBitmapSource(this.mContext, this.mAssetPath,
            this.mTexturePositionX, this.mTexturePositionY, this.mWidth,
            this.mHeight)
    }

    override fun getWidth(): Int = this.mWidth

    override fun getHeight(): Int = this.mHeight

    fun preload(): Boolean {
        bitmap = onLoadBitmap(Bitmap.Config.ARGB_8888)
        return bitmap != null
    }

    fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
        if (bitmap != null) {
            val bmp = bitmap
            bitmap = null
            return bmp
        }
        var inputStream: InputStream? = null
        return try {
            val decodeOptions = BitmapFactory.Options()
            decodeOptions.inPreferredConfig = pBitmapConfig
            decodeOptions.inSampleSize = Config.getTextureQuality()
            inputStream = this.mContext.assets.open(this.mAssetPath)
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } catch (e: IOException) {
            Debug.e("Failed loading Bitmap in ${this.javaClass.simpleName}. AssetPath: $mAssetPath", e)
            null
        } finally {
            StreamUtils.close(inputStream)
        }
    }

    override fun toString(): String = "${this.javaClass.simpleName}($mAssetPath)"
}
