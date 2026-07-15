package com.edlplan.framework.support.osb

import android.graphics.Color
import com.edlplan.andengine.TextureHelper
import com.edlplan.edlosbsupport.OsuStoryboard
import com.edlplan.edlosbsupport.OsuStoryboardLayer
import com.edlplan.edlosbsupport.elements.IStoryboardElement
import com.edlplan.edlosbsupport.elements.StoryboardAnimationSprite
import com.edlplan.edlosbsupport.parser.OsbFileParser
import com.edlplan.edlosbsupport.player.OsbPlayer
import com.edlplan.framework.math.Anchor
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.support.ProxySprite
import com.edlplan.framework.support.SupportSprite
import com.edlplan.framework.support.batch.BatchEngine
import com.edlplan.framework.support.batch.`object`.TextureQuad
import com.edlplan.framework.support.batch.`object`.TextureQuadBatch
import com.edlplan.framework.support.graphics.BaseCanvas
import com.edlplan.framework.support.graphics.texture.TexturePool
import com.edlplan.framework.support.util.Tracker
import com.edlplan.framework.utils.functionality.SmartIterator
import org.anddev.andengine.opengl.texture.region.TextureRegion
import java.io.File
import java.util.HashMap
import ru.nsu.ccfit.zuev.osu.helper.FileUtils

class StoryboardSprite(width: Float, height: Float) : SupportSprite(width, height) {

    var context: OsbContext = OsbContext()
    var storyboard: OsuStoryboard? = null
    var osbPlayer: OsbPlayer? = null
    var backgroundQuad: TextureQuad? = null
    var forgroundQuad: TextureQuad? = null
    var replaceBackground: Boolean = false

    var transparentBackground: Boolean = false
    var loadedOsu: String? = null
    private var time: Double = 0.0
    private var needUpdate: Boolean = false

    fun getLoadedPool(): TexturePool {
        return context.texturePool!!
    }

    fun setBrightness(brightness: Float) {
        val region: TextureRegion = TextureHelper.create1xRegion(Color.argb(255, 0, 0, 0))!!
        backgroundQuad = TextureQuad()
        backgroundQuad!!.anchor = Anchor.TopLeft
        backgroundQuad!!.setTextureAndSize(region)
        forgroundQuad = TextureQuad()
        forgroundQuad!!.anchor = Anchor.TopLeft
        forgroundQuad!!.setTextureAndSize(region)
        forgroundQuad!!.alpha.value = 1 - brightness
    }

    fun updateTime(time: Double) {
        if (Math.abs(this.time - time) > 10) {
            this.time = time
            osbPlayer?.update(time)
        }
    }

    fun isStoryboardAvailable(): Boolean {
        return storyboard != null
    }

    fun setOverlayDrawProxy(proxy: ProxySprite) {
        proxy.drawProxy = object : ProxySprite.DrawProxy {
            override fun onSupportDraw(canvas: BaseCanvas) {
                drawOverlay(canvas)
            }
        }
    }

    fun drawOverlay(canvas: BaseCanvas) {
        if (storyboard == null) {
            return
        }

        canvas.blendSetting.save()
        canvas.save()
        val scale = Math.max(640f / canvas.getWidth(), 480f / canvas.getHeight())
        val startOffset = Vec2(canvas.getWidth() / 2, canvas.getHeight() / 2)
            .minus(640 * 0.5f / scale, 480 * 0.5f / scale)

        canvas.translate(startOffset.x, startOffset.y).expendAxis(scale)

        if (context.engines != null) {
            for (engine in context.engines!!) {
                if (engine != null && engine.getLayer() == com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.Overlay) {
                    engine.draw(canvas)
                }
            }
        }

        canvas.restore()
        canvas.blendSetting.restore()
    }

    override fun onSupportDraw(canvas: BaseCanvas) {
        super.onSupportDraw(canvas)

        if (storyboard == null) {
            return
        }

        drawBackground(canvas)

        canvas.blendSetting.save()
        canvas.save()
        val scale = Math.max(640f / canvas.getWidth(), 480f / canvas.getHeight())
        val startOffset = Vec2(canvas.getWidth() / 2, canvas.getHeight() / 2)
            .minus(640 * 0.5f / scale, 480 * 0.5f / scale)

        canvas.translate(startOffset.x, startOffset.y).expendAxis(scale)

        if (context.engines != null) {
            for (engine in context.engines!!) {
                if (engine != null && engine.getLayer() != com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.Overlay) {
                    engine.draw(canvas)
                }
            }
        }

        canvas.restore()
        canvas.blendSetting.restore()

        if (forgroundQuad != null) {
            forgroundQuad!!.size.set(canvas.getWidth(), canvas.getHeight())
            TextureQuadBatch.getDefaultBatch().add(forgroundQuad!!)
            BatchEngine.flush()
        }
    }

    private fun drawBackground(canvas: BaseCanvas) {
        if (transparentBackground) {
            return
        }

        if (replaceBackground) {
            if (backgroundQuad != null) {
                backgroundQuad!!.size.set(canvas.getWidth(), canvas.getHeight())
                TextureQuadBatch.getDefaultBatch().add(backgroundQuad!!)
                BatchEngine.flush()
            }
        } else {
            if (backgroundQuad == null) {
                backgroundQuad = TextureQuad()
            }
            backgroundQuad!!.anchor = Anchor.Center
            backgroundQuad!!.setTextureAndSize(context.texturePool!!.get(storyboard!!.backgroundFile))
            backgroundQuad!!.position.set(canvas.getWidth() / 2, canvas.getHeight() / 2)
            backgroundQuad!!.enableScale().scale!!.set(
                Math.min(
                    canvas.getWidth() / backgroundQuad!!.size.x,
                    canvas.getHeight() / backgroundQuad!!.size.y))
            TextureQuadBatch.getDefaultBatch().add(backgroundQuad!!)
        }
    }

    private fun findOsb(osuFile: String): File? {
        var dir = File(osuFile)
        dir = dir.parentFile
        val fs = FileUtils.listFiles(dir, ".osb") ?: emptyArray()
        return if (fs.isNotEmpty()) {
            fs[0]
        } else {
            null
        }
    }

    private fun loadOsb(osuFile: String) {
        val file = findOsb(osuFile) ?: return

        val parser = OsbFileParser(file, null)

        Tracker.createTmpNode("ParseOsb").wrap(Runnable {
            try {
                parser.parse()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).then { obj: Any -> println(obj) }

        storyboard = parser.baseParser.storyboard
    }

    private fun loadOsu(osuFile: String) {
        val parser = OsbFileParser(File(osuFile), null)
        Tracker.createTmpNode("ParseOsu").wrap(Runnable {
            try {
                parser.parse()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).then { obj: Any -> println(obj) }

        val osustoryboard = parser.baseParser.storyboard

        if (storyboard == null) {
            var empty = true
            for (layer in osustoryboard.layers) {
                if (layer != null) {
                    empty = false
                    break
                }
            }
            if (empty) {
                return
            }
            storyboard = osustoryboard
        } else {
            storyboard!!.appendStoryboard(osustoryboard)
        }
    }

    private fun loadFromCache() {
        context.engines = arrayOfNulls<LayerRenderEngine>(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values().size)
        for (i in context.engines!!.indices) {
            context.engines!![i] = LayerRenderEngine(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values()[i])
        }

        if (storyboard == null) {
            return
        }

        osbPlayer = OsbPlayer { s ->
            if (s.javaClass == com.edlplan.edlosbsupport.elements.StoryboardSprite::class.java) {
                EGFStoryboardSprite(context)
            } else {
                EGFStoryboardAnimationSprite(context)
            }
        }

        Tracker.createTmpNode("LoadPlayer").wrap(Runnable {
            osbPlayer!!.loadStoryboard(storyboard)
        }).then { obj: Any -> println(obj) }
    }

    fun loadStoryboard(osuFile: String) {
        println("$this load storyboard from $osuFile")
        if (osuFile == loadedOsu) {
            println("load storyboard from cache")
            loadFromCache()
            return
        }
        loadedOsu = osuFile

        releaseStoryboard()

        loadedOsu = osuFile

        val osu = File(osuFile)
        val dir = osu.parentFile
        val pool = TexturePool(dir)

        context.texturePool = pool
        context.engines = arrayOfNulls<LayerRenderEngine>(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values().size)
        for (i in context.engines!!.indices) {
            context.engines!![i] = LayerRenderEngine(com.edlplan.edlosbsupport.elements.StoryboardSprite.Layer.values()[i])
        }

        loadOsb(osuFile)
        loadOsu(osuFile)

        if (storyboard == null) {
            return
        }

        replaceBackground = storyboard!!.needReplaceBackground()
        Tracker.createTmpNode("PackTextures").wrap(Runnable {
            val counted = countTextureUsedTimes(storyboard!!)
            if (!replaceBackground && storyboard!!.backgroundFile != null) {
                counted[storyboard!!.backgroundFile] =
                    if (counted[storyboard!!.backgroundFile] == null)
                        1
                    else
                        counted[storyboard!!.backgroundFile]!! + 1
            }

            var allToPack = SmartIterator.wrap(counted.keys.iterator())
                .applyFilter { s -> counted[s]!! >= 15 }
            pool.packAll(allToPack, null)

            allToPack = SmartIterator.wrap(counted.keys.iterator())
                .applyFilter { s -> counted[s]!! < 15 }
            while (allToPack.hasNext()) {
                pool.add(allToPack.next())
            }
        }).then { obj: Any -> println(obj) }


        osbPlayer = OsbPlayer { s ->
            if (s.javaClass == com.edlplan.edlosbsupport.elements.StoryboardSprite::class.java) {
                EGFStoryboardSprite(context)
            } else {
                EGFStoryboardAnimationSprite(context)
            }
        }

        Tracker.createTmpNode("LoadPlayer").wrap(Runnable {
            osbPlayer!!.loadStoryboard(storyboard)
        }).then { obj: Any -> println(obj) }

    }

    fun releaseStoryboard() {
        if (context.texturePool != null) {
            context.texturePool!!.clear()
            context.texturePool = null
        }
        if (storyboard != null) {
            storyboard!!.clear()
            storyboard = null
        }
        if (osbPlayer != null) {
            osbPlayer = null
        }
        loadedOsu = null
    }


    companion object {
        private fun countTextureUsedTimes(storyboard: OsuStoryboard): HashMap<String, Int> {
            val textures = HashMap<String, Int>()
            var tmp: Int?
            var tmps: String
            for (layer in storyboard.layers) {
                if (layer != null) {
                    for (element in layer.elements) {
                        if (element is StoryboardAnimationSprite) {
                            for (i in 0 until element.frameCount) {
                                tmps = element.buildPath(i)
                                tmp = textures[tmps]
                                if (tmp == null) {
                                    textures[tmps] = 1
                                    continue
                                }
                                textures[tmps] = tmp + 1
                            }
                        } else if (element is com.edlplan.edlosbsupport.elements.StoryboardSprite) {
                            tmps = element.spriteFilename
                            tmp = textures[tmps]
                            if (tmp == null) {
                                textures[tmps] = 1
                                continue
                            }
                            textures[tmps] = tmp + 1
                        }
                    }
                }
            }
            return textures
        }
    }
}
