package com.edlplan.andengine

import android.graphics.Bitmap
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource.InputFactory

object TextureHelper {

    private var tmpFileId = 0

    @JvmStatic
    fun createFactoryFromBitmap(bitmap: Bitmap): InputFactory? {
        tmpFileId++
        try {
            val tmp = File.createTempFile("bmp_cache$tmpFileId", ".png")
            tmp.deleteOnExit()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(tmp))
            return InputFactory { FileInputStream(tmp) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun createMemoryFactoryFromBitmap(bitmap: Bitmap): InputFactory {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return InputFactory { ByteArrayInputStream(bytes) }
    }

    @JvmStatic
    fun createRegion(bitmap: Bitmap): TextureRegion? {
        var tw = 4
        var th = 4
        val source = QualityFileBitmapSource(createFactoryFromBitmap(bitmap)!!)
        if (source.width == 0 || source.height == 0) {
            return null
        }
        while (tw < source.width) {
            tw *= 2
        }
        while (th < source.height) {
            th *= 2
        }

        var errorCount = 0
        while (!source.preload() && errorCount < 3) {
            errorCount++
        }
        if (errorCount >= 3) {
            return null
        }
        val tex = BitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)

        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        GlobalManager.getInstance().engine?.textureManager?.loadTexture(tex)
        return region
    }

    @JvmStatic
    fun create1xRegion(color: Int): TextureRegion? {
        val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        var tw = 4
        var th = 4
        val source = QualityFileBitmapSource(createMemoryFactoryFromBitmap(bmp))
        if (source.width == 0 || source.height == 0) {
            return null
        }
        while (tw < source.width) {
            tw *= 2
        }
        while (th < source.height) {
            th *= 2
        }

        var errorCount = 0
        while (!source.preload() && errorCount < 3) {
            errorCount++
        }
        if (errorCount >= 3) {
            return null
        }
        val tex = BitmapTextureAtlas(tw, th, TextureOptions.BILINEAR)

        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        GlobalManager.getInstance().engine?.textureManager?.loadTexture(tex)
        return region
    }
}
