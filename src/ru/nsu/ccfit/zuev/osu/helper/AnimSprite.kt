package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.StringSkinData
import javax.microedition.khronos.opengles.GL10

open class AnimSprite : Sprite {

    enum class LoopType {
        STOP,
        LOOP,
        DISAPPEAR,
        FROZE
    }

    private val count: Int
    private val regions: Array<TextureRegion>
    private var frame = 0
    private var animTime = 0f
    private var fps: Float
    private var loopType: LoopType = LoopType.LOOP

    constructor(px: Float, py: Float, prefix: StringSkinData, name: String?, count: Int, fps: Float) : super(
        px, py, ResourceManager.getInstance().getTextureWithPrefix(prefix, (name ?: "") + if (count == 1) "" else "0")
    ) {
        val actualCount = if (count == 0) 1 else count
        this.count = actualCount
        this.fps = fps
        regions = Array(actualCount) { i ->
            ResourceManager.getInstance().getTextureWithPrefix(prefix, (name ?: "") + if (actualCount == 1) "" else i.toString())!!
        }
        if (fps == 0f) {
            loopType = LoopType.FROZE
        }
    }

    constructor(px: Float, py: Float, texname: String, count: Int, fps: Float) : super(
        px, py, ResourceManager.getInstance().getTexture(texname + "0")
    ) {
        val actualCount = if (count == 0) 1 else count
        this.count = actualCount
        this.fps = fps
        regions = Array(actualCount) { i ->
            ResourceManager.getInstance().getTexture(texname + i)!!
        }
        if (fps == 0f) {
            loopType = LoopType.FROZE
        }
    }

    constructor(px: Float, py: Float, fps: Float, vararg textures: String) : super(
        px, py, ResourceManager.getInstance().getTextureIfLoaded(textures[0])
    ) {
        this.count = textures.size
        this.fps = fps
        regions = Array(textures.size) { i ->
            ResourceManager.getInstance().getTextureIfLoaded(textures[i])!!
        }
        if (fps == 0f) {
            loopType = LoopType.FROZE
        }
    }

    fun setLoopType(loopType: LoopType) {
        this.loopType = loopType
    }

    fun getLoopType(): LoopType = loopType

    private fun updateFrame() {
        if (loopType == LoopType.FROZE || fps == 0f) return
        val frameByTime = (this.animTime * fps).toInt()
        when (loopType) {
            LoopType.LOOP -> frame = frameByTime % count
            LoopType.STOP -> frame = frameByTime.coerceAtMost(count - 1)
            LoopType.DISAPPEAR -> frame = frameByTime.coerceAtMost(count)
            else -> {}
        }
    }

    fun setFps(fps: Float) {
        frame = 0
        this.fps = fps
    }

    fun setFrame(frame: Int) {
        if (this.loopType == LoopType.FROZE || fps == 0f) {
            this.frame = frame
        } else {
            this.animTime = (frame + 0.0001f) / fps
            updateFrame()
        }
    }

    fun getFrame(): Int = frame

    fun setAnimTime(animTime: Float) {
        this.animTime = animTime
        updateFrame()
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        this.animTime += pSecondsElapsed
        updateFrame()
        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun doDraw(pGL: GL10, pCamera: Camera) {
        if (regions.isEmpty() || frame < 0 || frame >= regions.size) return
        regions[frame].onApply(pGL)
        onInitDraw(pGL)
        onApplyVertices(pGL)
        drawVertices(pGL, pCamera)
    }

    override fun setFlippedHorizontal(pFlippedHorizontal: Boolean) {
        for (reg in regions) {
            reg.setFlippedHorizontal(pFlippedHorizontal)
        }
    }

    fun getFrameWidth(): Float {
        return if (frame < regions.size && frame >= 0) {
            regions[frame].width.toFloat()
        } else if (regions.isNotEmpty()) {
            regions[0].width.toFloat()
        } else {
            40f
        }
    }

    fun setTextureRegion(index: Int, region: TextureRegion) {
        regions[index] = region
    }

    fun getTextureRegionAt(index: Int): TextureRegion = regions[index]

    fun getTextureRegionCount(): Int = regions.size
}
