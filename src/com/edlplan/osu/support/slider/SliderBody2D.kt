package com.edlplan.osu.support.slider

import com.edlplan.andengine.SpriteCache
import com.edlplan.andengine.TriangleBuilder
import com.edlplan.andengine.TrianglePack
import com.edlplan.framework.math.Color4
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.AbstractPath
import com.edlplan.framework.math.line.LinePath
import com.edlplan.framework.math.line.PathMeasurer
import com.edlplan.framework.utils.FloatArraySlice
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.util.modifier.ease.EaseQuadOut
import ru.nsu.ccfit.zuev.osu.RGBColor

class SliderBody2D(path: LinePath) : AbstractSliderBody(path) {

    private var sliderBodyBaseAlpha: Float = 0.7f
    private var hintAlpha: Float = 0.3f
    private var body: TrianglePack? = null
    private var border: TrianglePack? = null
    private var hint: TrianglePack? = null
    private val bodyColor = RGBColor()
    private val borderColor = RGBColor()
    private val hintColor = RGBColor()
    private var bodyWidth: Float = 0f
    private var borderWidth: Float = 0f
    private var hintWidth: Float = 0f
    private var startLength: Float = 0f
    private var endLength: Float = 0f
    private var maxPathLength: Float = 0f
    private var enableHint: Boolean = false
    private var dirty: Boolean = true

    private var cachedBodyVertices: FloatArray? = null
    private var cachedBorderVertices: FloatArray? = null
    private var cachedHintVertices: FloatArray? = null
    private var cachedBodyLength: Int = 0
    private var cachedBorderLength: Int = 0
    private var cachedHintLength: Int = 0

    init {
        maxPathLength = path.measurer.maxLength()
    }

    fun isEnableHint(): Boolean = enableHint

    fun setEnableHint(enableHint: Boolean) {
        this.enableHint = enableHint
    }

    fun getSliderBodyBaseAlpha(): Float = sliderBodyBaseAlpha

    override fun setSliderBodyBaseAlpha(sliderBodyBaseAlpha: Float) {
        this.sliderBodyBaseAlpha = sliderBodyBaseAlpha
    }

    fun setHintAlpha(hintAlpha: Float) {
        this.hintAlpha = hintAlpha
    }

    fun setHintColor(r: Float, g: Float, b: Float) {
        hintColor.set(r, g, b)
        hint?.setColor(r, g, b)
    }

    fun setHintWidth(hintWidth: Float) {
        this.hintWidth = hintWidth
    }

    fun applyFadeAdjustments(fadeInDuration: Float) {
        body?.registerEntityModifier(AlphaModifier(fadeInDuration, 0f, sliderBodyBaseAlpha))
        border?.registerEntityModifier(FadeInModifier(fadeInDuration))
        hint?.registerEntityModifier(AlphaModifier(fadeInDuration, 0f, hintAlpha))
    }

    fun applyFadeAdjustments(fadeInDuration: Float, fadeOutDuration: Float) {
        val easing = EaseQuadOut.getInstance()

        body?.registerEntityModifier(
            SequenceEntityModifier(
                AlphaModifier(fadeInDuration, 0f, sliderBodyBaseAlpha),
                AlphaModifier(fadeOutDuration, sliderBodyBaseAlpha, 0f, easing)
            )
        )

        border?.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(fadeInDuration),
                FadeOutModifier(fadeOutDuration, easing)
            )
        )

        hint?.registerEntityModifier(
            SequenceEntityModifier(
                AlphaModifier(fadeInDuration, 0f, hintAlpha),
                AlphaModifier(fadeOutDuration, hintAlpha, 0f, easing)
            )
        )
    }

    override fun onUpdate() {
        if (body == null || border == null || !dirty) return
        dirty = false

        val cache = localCache.get()

        val maxLength = path.measurer.maxLength()

        val fullPath = startLength <= 0.001f && endLength >= maxLength - 0.001f

        if (fullPath) {
            cache.drawLinePath.prepareForPath(path)

            hint?.let {
                cache.drawLinePath.buildForWidth(hintWidth, cache.triangleBuilder)
                cache.hintBuilderLength = cache.triangleBuilder.length
                cache.triangleBuilder.getVertex(it.vertices)
            }
            cache.drawLinePath.buildForWidth(bodyWidth, cache.triangleBuilder)
            cache.bodyBuilderLength = cache.triangleBuilder.length
            cache.triangleBuilder.getVertex(body!!.vertices)

            cache.drawLinePath.buildForWidth(borderWidth, cache.triangleBuilder)
            cache.borderBuilderLength = cache.triangleBuilder.length
            cache.triangleBuilder.getVertex(border!!.vertices)

            cacheBodyVertices(body!!.vertices)
            cacheBorderVertices(border!!.vertices)
            hint?.let { cacheHintVertices(it.vertices) }
            cache.cachedMaxLength = maxLength
            cache.cachedStartLength = 0f
            cache.cachedEndLength = maxLength
            return
        }

        if (startLength <= 0.001f && cachedBodyVertices != null && endLength > cache.cachedEndLength) {
            buildSnakingPrefix(cache, maxLength)
            return
        }

        if (endLength >= maxLength - 0.001f && cachedBodyVertices != null && startLength > cache.cachedStartLength) {
            buildSnakingSuffix(cache, maxLength)
            return
        }

        val sub: AbstractPath = path.cutPathView(startLength, endLength)
        cache.drawLinePath.prepareForPath(sub)

        hint?.let {
            cache.drawLinePath.buildForWidth(hintWidth, cache.triangleBuilder)
                .getVertex(it.vertices)
        }
        cache.drawLinePath.buildForWidth(bodyWidth, cache.triangleBuilder)
            .getVertex(body!!.vertices)
        cache.drawLinePath.buildForWidth(borderWidth, cache.triangleBuilder)
            .getVertex(border!!.vertices)
    }

    private fun buildSnakingPrefix(cache: BuildCache, maxLength: Float) {
        val measurer = path.measurer
        var boundarySeg = measurer.binarySearch(endLength)
        if (boundarySeg >= structureSize() - 1) {
            boundarySeg = structureSize() - 2
        }

        val segStartLen = measurer.getLengthAt(boundarySeg)
        val segEndLen = measurer.getLengthAt(boundarySeg + 1)
        val segLen = segEndLen - segStartLen
        var t = if (segLen > 0.001f) (endLength - segStartLen) / segLen else 1f
        t = Math.min(1f, Math.max(0f, t))

        val segStart = path[boundarySeg]
        val segEnd = path[boundarySeg + 1]
        val boundaryPoint = Vec2(
            segStart.x + (segEnd.x - segStart.x) * t,
            segStart.y + (segEnd.y - segStart.y) * t
        )

        cache.drawLinePath.prepareForPath(path)

        val prefixLength = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg)

        if (prefixLength <= cachedBodyLength) {
            System.arraycopy(cachedBodyVertices, 0, body!!.vertices.ary, 0, prefixLength)
            body!!.vertices.length = prefixLength
        } else {
            body!!.vertices.length = 0
        }
        cache.drawLinePath.buildBoundarySuffix(bodyWidth, cache.triangleBuilder, boundarySeg, boundaryPoint, segStart)
        cache.triangleBuilder.getVertex(body!!.vertices)

        if (prefixLength <= cachedBorderLength) {
            System.arraycopy(cachedBorderVertices, 0, border!!.vertices.ary, 0, prefixLength)
            border!!.vertices.length = prefixLength
        } else {
            border!!.vertices.length = 0
        }
        cache.drawLinePath.buildBoundarySuffix(borderWidth, cache.triangleBuilder, boundarySeg, boundaryPoint, segStart)
        cache.triangleBuilder.getVertex(border!!.vertices)

        if (hint != null && cachedHintVertices != null) {
            if (prefixLength <= cachedHintLength) {
                System.arraycopy(cachedHintVertices, 0, hint!!.vertices.ary, 0, prefixLength)
                hint!!.vertices.length = prefixLength
            } else {
                hint!!.vertices.length = 0
            }
            cache.drawLinePath.buildBoundarySuffix(hintWidth, cache.triangleBuilder, boundarySeg, boundaryPoint, segStart)
            cache.triangleBuilder.getVertex(hint!!.vertices)
        }

        cache.cachedEndLength = endLength
    }

    private fun buildSnakingSuffix(cache: BuildCache, maxLength: Float) {
        val measurer = path.measurer
        var boundarySeg = measurer.binarySearch(startLength)
        if (boundarySeg >= structureSize() - 1) {
            boundarySeg = structureSize() - 2
        }

        val segStartLen = measurer.getLengthAt(boundarySeg)
        val segEndLen = measurer.getLengthAt(boundarySeg + 1)
        val segLen = segEndLen - segStartLen
        var t = if (segLen > 0.001f) (startLength - segStartLen) / segLen else 0f
        t = Math.min(1f, Math.max(0f, t))

        val segStart = path[boundarySeg]
        val segEnd = path[boundarySeg + 1]
        val boundaryPoint = Vec2(
            segStart.x + (segEnd.x - segStart.x) * t,
            segStart.y + (segEnd.y - segStart.y) * t
        )

        cache.drawLinePath.prepareForPath(path)

        body!!.vertices.length = 0
        cache.drawLinePath.buildBoundaryPrefix(bodyWidth, cache.triangleBuilder, boundarySeg, boundaryPoint, segEnd)
        var suffixOffset = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg + 1)
        if (suffixOffset > 0 && suffixOffset <= cachedBodyLength) {
            val suffixLength = cachedBodyLength - suffixOffset
            System.arraycopy(cachedBodyVertices, suffixOffset, cache.triangleBuilder.ary, cache.triangleBuilder.length, suffixLength)
            cache.triangleBuilder.length += suffixLength
        }
        cache.triangleBuilder.getVertex(body!!.vertices)

        border!!.vertices.length = 0
        cache.drawLinePath.buildBoundaryPrefix(borderWidth, cache.triangleBuilder, boundarySeg, boundaryPoint, segEnd)
        suffixOffset = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg + 1)
        if (suffixOffset > 0 && suffixOffset <= cachedBorderLength) {
            val suffixLength = cachedBorderLength - suffixOffset
            System.arraycopy(cachedBorderVertices, suffixOffset, cache.triangleBuilder.ary, cache.triangleBuilder.length, suffixLength)
            cache.triangleBuilder.length += suffixLength
        }
        cache.triangleBuilder.getVertex(border!!.vertices)

        if (hint != null && cachedHintVertices != null) {
            hint!!.vertices.length = 0
            cache.drawLinePath.buildBoundaryPrefix(hintWidth, cache.triangleBuilder, boundarySeg, boundaryPoint, segEnd)
            suffixOffset = cache.drawLinePath.getSegmentQuadStartOffset(boundarySeg + 1)
            if (suffixOffset > 0 && suffixOffset <= cachedHintLength) {
                val suffixLength = cachedHintLength - suffixOffset
                System.arraycopy(cachedHintVertices, suffixOffset, cache.triangleBuilder.ary, cache.triangleBuilder.length, suffixLength)
                cache.triangleBuilder.length += suffixLength
            }
            cache.triangleBuilder.getVertex(hint!!.vertices)
        }

        cache.cachedStartLength = startLength
    }

    private fun structureSize(): Int = path.size()

    private fun cacheBodyVertices(vertices: FloatArraySlice) {
        if (vertices.length > MAX_CACHED_VERTICES) {
            cachedBodyVertices = null
            cachedBodyLength = 0
            return
        }
        if (cachedBodyVertices == null || cachedBodyVertices!!.size < vertices.length) {
            cachedBodyVertices = FloatArray(vertices.length)
        }
        System.arraycopy(vertices.ary, 0, cachedBodyVertices, 0, vertices.length)
        cachedBodyLength = vertices.length
    }

    private fun cacheBorderVertices(vertices: FloatArraySlice) {
        if (vertices.length > MAX_CACHED_VERTICES) {
            cachedBorderVertices = null
            cachedBorderLength = 0
            return
        }
        if (cachedBorderVertices == null || cachedBorderVertices!!.size < vertices.length) {
            cachedBorderVertices = FloatArray(vertices.length)
        }
        System.arraycopy(vertices.ary, 0, cachedBorderVertices, 0, vertices.length)
        cachedBorderLength = vertices.length
    }

    private fun cacheHintVertices(vertices: FloatArraySlice) {
        if (vertices.length > MAX_CACHED_VERTICES) {
            cachedHintVertices = null
            cachedHintLength = 0
            return
        }
        if (cachedHintVertices == null || cachedHintVertices!!.size < vertices.length) {
            cachedHintVertices = FloatArray(vertices.length)
        }
        System.arraycopy(vertices.ary, 0, cachedHintVertices, 0, vertices.length)
        cachedHintLength = vertices.length
    }

    override fun setBodyWidth(width: Float) {
        bodyWidth = width
    }

    override fun setBorderWidth(width: Float) {
        borderWidth = width
    }

    override fun setBodyColor(r: Float, g: Float, b: Float) {
        bodyColor.set(r, g, b)
        body?.setColor(r, g, b)
    }

    override fun setBorderColor(r: Float, g: Float, b: Float) {
        borderColor.set(r, g, b)
        border?.setColor(r, g, b)
    }

    override fun setStartLength(length: Float) {
        val clamped = Math.max(0f, Math.min(length, maxPathLength))
        if (Math.abs(startLength - clamped) >= SNAKE_LENGTH_THRESHOLD) {
            startLength = clamped
            dirty = true
        }
    }

    override fun setEndLength(length: Float) {
        val clamped = Math.max(0f, Math.min(length, maxPathLength))
        if (Math.abs(endLength - clamped) >= SNAKE_LENGTH_THRESHOLD) {
            endLength = clamped
            dirty = true
        }
    }

    override fun applyToScene(scene: Scene, emptyOnStart: Boolean) {
        val cache = localCache.get()
        body = SpriteCache.trianglePackCache.get()
        border = SpriteCache.trianglePackCache.get()

        body!!.clearEntityModifiers()
        border!!.clearEntityModifiers()

        if (enableHint) {
            hint = SpriteCache.trianglePackCache.get()
            hint!!.clearEntityModifiers()
            hint!!.setAlpha(0f)
            hint!!.setDepthTest(true)
            hint!!.setClearDepthOnStart(true)
            hint!!.setColor(hintColor.r(), hintColor.g(), hintColor.b())
        }

        body!!.setAlpha(0f)
        body!!.setDepthTest(true)
        body!!.setClearDepthOnStart(!enableHint)
        body!!.setColor(bodyColor.r(), bodyColor.g(), bodyColor.b())

        border!!.setAlpha(0f)
        border!!.setDepthTest(true)
        border!!.setClearDepthOnStart(false)
        border!!.setColor(borderColor.r(), borderColor.g(), borderColor.b())

        cachedBodyVertices = null
        cachedBorderVertices = null
        cachedHintVertices = null

        if (emptyOnStart) {
            hint?.let { it.vertices.length = 0 }
            body!!.vertices.length = 0
            border!!.vertices.length = 0
        } else {
            cache.drawLinePath.prepareForPath(path)
            hint?.let {
                cache.drawLinePath.buildForWidth(hintWidth, cache.triangleBuilder)
                    .getVertex(it.vertices)
            }
            cache.drawLinePath.buildForWidth(bodyWidth, cache.triangleBuilder)
                .getVertex(body!!.vertices)
            cache.drawLinePath.buildForWidth(borderWidth, cache.triangleBuilder)
                .getVertex(border!!.vertices)

            cacheBodyVertices(body!!.vertices)
            cacheBorderVertices(border!!.vertices)
            hint?.let { cacheHintVertices(it.vertices) }
        }

        if (!emptyOnStart) {
            startLength = 0f
            endLength = path.measurer.maxLength()
        }
        dirty = false

        scene.attachChild(border, 0)
        scene.attachChild(body, 0)
        hint?.let { scene.attachChild(it, 0) }
    }

    override fun removeFromScene(scene: Scene) {
        hint?.let {
            it.detachSelf()
            SpriteCache.trianglePackCache.save(it)
        }
        hint = null
        body?.let {
            it.detachSelf()
            SpriteCache.trianglePackCache.save(it)
        }
        body = null
        border?.let {
            it.detachSelf()
            SpriteCache.trianglePackCache.save(it)
        }
        border = null
    }

    private class BuildCache {
        val path = LinePath()
        val triangleBuilder = TriangleBuilder()
        val drawLinePath = DrawLinePath()
        var cachedMaxLength: Float = 0f
        var cachedStartLength: Float = 0f
        var cachedEndLength: Float = 0f
        var bodyBuilderLength: Int = 0
        var borderBuilderLength: Int = 0
        var hintBuilderLength: Int = 0
    }

    class SliderProperty {
        var color: Color4 = Color4.White.copyNew()
        var width: Float = 0f
        var pack: TrianglePack? = null
    }

    companion object {
        private val localCache = object : ThreadLocal<BuildCache>() {
            override fun initialValue(): BuildCache = BuildCache()
        }

        private const val SNAKE_LENGTH_THRESHOLD = 0.75f
        private const val MAX_CACHED_VERTICES = 512 * 1024
    }
}
