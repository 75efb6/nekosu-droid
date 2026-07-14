package com.edlplan.framework.support.graphics.texture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import com.edlplan.andengine.TextureHelper
import com.edlplan.framework.math.Vec2Int
import com.edlplan.framework.support.graphics.BitmapUtil
import com.edlplan.framework.utils.interfaces.Consumer
import org.anddev.andengine.BuildConfig
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.ListIterator
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource

class TexturePool(private val dir: File) {

    var glMaxWidth: Int
    private val options: BitmapFactory.Options = BitmapFactory.Options()
    private val createdTextures: MutableSet<ITexture> = HashSet()
    private val textures: HashMap<String, TextureRegion> = HashMap()
    private var currentPack: Int = 0
    private var currentX: Int = 0
    private var currentY: Int = 0
    private var lineMaxY: Int = 0
    private val marginX: Int = 2
    private val marginY: Int = 2
    private val maxW: Int
    private val maxH: Int

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            options.inPremultiplied = true
        }
        glMaxWidth = GLHelper.GlMaxTextureWidth
        if (BuildConfig.DEBUG) println("GL_MAX_TEXTURE_SIZE = $glMaxWidth")
        if (glMaxWidth == 0) {
            throw RuntimeException("glMaxWidth not found")
        }
        glMaxWidth = Math.min(glMaxWidth, 4096)
        maxW = Math.min(400, glMaxWidth / 2)
        maxH = Math.min(400, glMaxWidth / 2)
    }

    fun clear() {
        textures.clear()
        for (texture in createdTextures) {
            GlobalManager.getInstance().engine?.textureManager?.unloadTexture(texture)
        }
        createdTextures.clear()
        currentPack = 0
        currentX = 0
        currentY = 0
        lineMaxY = 0
    }

    fun add(name: String) {
        val info = loadInfo(name)
        val bmp = loadBitmap(info)
        info.texture = TextureHelper.createRegion(bmp)
        createdTextures.add(info.texture!!.texture)
        directPut(info.name!!, info.texture!!)
        bmp.recycle()
    }

    fun packAll(collection: Iterator<String>, onPackDrawDone: Consumer<Bitmap>?) {
        clear()

        val infos = ArrayList<TextureInfo>()
        while (collection.hasNext()) {
            infos.add(loadInfo(collection.next()))
        }
        Collections.sort(infos) { p1, p2 ->
            if (p1.size.y == p2.size.y) {
                p1.size.x.toFloat().compareTo(p2.size.x.toFloat())
            } else {
                p1.size.y.toFloat().compareTo(p2.size.y.toFloat())
            }
        }

        for (t in infos) {
            testAddRaw(t)
        }

        Collections.sort(infos) { p1, p2 -> p1.pageIndex.compareTo(p2.pageIndex) }

        val iterator = infos.listIterator()

        while (iterator.hasNext()) {
            val info = iterator.next()
            if (info.pageIndex != -1) {
                iterator.previous()
                break
            }
            val bmp = loadBitmap(info)
            info.texture = TextureHelper.createRegion(bmp)
            createdTextures.add(info.texture!!.texture)
            directPut(info.name!!, info.texture!!)
            bmp.recycle()
        }

        var pack: Bitmap? = null

        if (iterator.hasNext()) {
            val width = glMaxWidth
            val height = if (currentPack == 0) lineMaxY + 10 else glMaxWidth
            pack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        if (pack == null) {
            return
        }
        val canvas = Canvas(pack)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        val toLoad = ArrayList<TextureInfo>()
        while (iterator.hasNext()) {
            toLoad.clear()
            pack.eraseColor(Color.argb(0, 0, 0, 0))
            val currentPack = iterator.next().pageIndex
            iterator.previous()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (info.pageIndex != currentPack) {
                    break
                }
                toLoad.add(info)
                val tmp = loadBitmap(info)
                canvas.drawBitmap(tmp, info.pos.x.toFloat(), info.pos.y.toFloat(), paint)
                tmp.recycle()
            }
            if (onPackDrawDone != null) {
                onPackDrawDone.consume(pack)
            }
            val source = QualityFileBitmapSource(
                TextureHelper.createFactoryFromBitmap(pack)!!)
            val tex = BitmapTextureAtlas(glMaxWidth, glMaxWidth, TextureOptions.BILINEAR)
            tex.addTextureAtlasSource(source, 0, 0)
            GlobalManager.getInstance().engine?.textureManager?.loadTexture(tex)
            createdTextures.add(tex)
            for (info in toLoad) {
                info.texture = TextureRegion(tex, info.pos.x, info.pos.y, info.size.x, info.size.y)
                info.texture!!.setTextureRegionBufferManaged(false)
            }
        }
        pack.recycle()

        for (info in infos) {
            directPut(info.name!!, info.texture!!)
        }

    }

    private fun testAddRaw(raw: TextureInfo) {
        if (raw.size.x > maxW || raw.size.y > maxH) {
            raw.single = true
            raw.pageIndex = -1
        } else {
            tryAddToPack(raw)
        }
    }

    private fun tryAddToPack(raw: TextureInfo) {
        if (currentX + raw.size.x + marginX < glMaxWidth) {
            tryAddInLine(raw)
        } else {
            toNextLine()
            tryAddToPack(raw)
        }
    }

    private fun tryAddInLine(raw: TextureInfo) {
        if (currentY + raw.size.y + marginY < glMaxWidth) {
            raw.single = false
            raw.pageIndex = currentPack
            raw.pos = Vec2Int(currentX, currentY)
            currentX += raw.size.x + marginX
            lineMaxY = Math.round(Math.max(lineMaxY.toFloat(), (currentY + raw.size.y + marginY).toFloat())).toInt()
        } else {
            toNewPack()
            tryAddToPack(raw)
        }
    }

    fun toNewPack() {
        currentPack++
        currentX = 0
        currentY = 0
        lineMaxY = 0
    }

    private fun toNextLine() {
        currentX = 0
        currentY = lineMaxY + marginY
    }

    private fun loadBitmap(info: TextureInfo): Bitmap {
        val bmp: Bitmap
        if (info.err) {
            bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            bmp.setPixel(0, 0, Color.argb(255, 255, 0, 0))
        } else {
            try {
                bmp = BitmapFactory.decodeFile(info.file, options)
            } catch (e: Exception) {
                val bmp2 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                bmp2.setPixel(0, 0, Color.argb(255, 255, 0, 0))
                return bmp2
            }
        }
        return bmp
    }

    protected fun directPut(name: String, region: TextureRegion) {
        textures[name] = region
    }

    private fun loadInfo(name: String): TextureInfo {
        val info = TextureInfo()
        try {
            info.name = name
            info.file = File(dir, name).absolutePath
            val size = BitmapUtil.parseBitmapSize(File(info.file))
            info.pos = Vec2Int(0, 0)
            info.size = size
        } catch (e: Exception) {
            e.printStackTrace()
            info.err = true
            info.pos = Vec2Int(0, 0)
            info.size = Vec2Int(1, 1)
        }
        return info
    }

    fun get(name: String): TextureRegion {
        var region: TextureRegion? = textures[name]
        if (region == null) {
            add(name)
            region = get(name)
        }
        return region
    }

    private class TextureInfo {
        var texture: TextureRegion? = null
        var name: String? = null
        var file: String? = null
        var size: Vec2Int = Vec2Int()
        var pos: Vec2Int = Vec2Int()
        var err: Boolean = false
        var single: Boolean = true
        var pageIndex: Int = -1
    }

}
