package com.edlplan.framework.support.batch.`object`

import com.edlplan.framework.math.Anchor
import com.edlplan.framework.math.Color4
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.utils.FloatRef
import org.anddev.andengine.opengl.texture.region.TextureRegion

class TextureQuad : ATextureQuad() {

    companion object {
        const val TopLeft = 0
        const val TopRight = 1
        const val BottomLeft = 2
        const val BottomRight = 3
    }

    var size: Vec2 = Vec2()

    var position: Vec2 = Vec2()

    var anchor: Anchor = Anchor.Center

    var scale: Vec2? = null

    var rotation: FloatRef? = null

    var alpha: FloatRef = FloatRef(1f)

    var accentColor: Color4? = null

    var u1: Float = 0f
    var v1: Float = 0f
    var u2: Float = 0f
    var v2: Float = 0f

    fun syncPosition(position: Vec2): TextureQuad {
        this.position = position
        return this
    }

    fun enableScale(): TextureQuad {
        if (scale == null) {
            scale = Vec2(1f, 1f)
        }
        return this
    }

    fun syncScale(vec2: Vec2): TextureQuad {
        scale = vec2
        return this
    }

    fun enableColor(): TextureQuad {
        if (accentColor == null) {
            accentColor = Color4.White.copyNew()
        }
        return this
    }

    fun syncColor(color: Color4): TextureQuad {
        accentColor = color
        return this
    }

    fun syncAlpha(ref: FloatRef): TextureQuad {
        this.alpha = ref
        return this
    }

    fun enableRotation(): TextureQuad {
        if (rotation == null) {
            rotation = FloatRef()
        }
        return this
    }

    fun syncRotation(ref: FloatRef): TextureQuad {
        this.rotation = ref
        return this
    }

    fun setTextureAndSize(texture: TextureRegion) {
        this.texture = texture
        size.set(texture.getWidth().toFloat(), texture.getHeight().toFloat())
        u1 = texture.getTextureCoordinateX1()
        u2 = texture.getTextureCoordinateX2()
        v1 = texture.getTextureCoordinateY1()
        v2 = texture.getTextureCoordinateY2()
    }

    fun setBaseWidth(width: Float) {
        size.set(width, width * (size.y / size.x))
    }

    fun setBaseHeight(height: Float) {
        size.set(height * (size.x / size.y), height)
    }

    override fun write(ary: FloatArray, offset: Int) {
        var offset = offset
        val l = -size.x * anchor.x()
        val r = size.x + l
        val t = -size.y * anchor.y()
        val b = size.y + t
        val cr: Float
        val cg: Float
        val cb: Float
        val ca: Float
        if (accentColor == null) {
            cr = alpha.value
            cg = alpha.value
            cb = alpha.value
            ca = alpha.value
        } else {
            val a = alpha.value
            cr = a * accentColor!!.r
            cg = a * accentColor!!.g
            cb = a * accentColor!!.b
            ca = a * accentColor!!.a
        }

        var lVar = l
        var rVar = r
        var tVar = t
        var bVar = b
        if (scale != null) {
            lVar *= scale!!.x
            rVar *= scale!!.x
            tVar *= scale!!.y
            bVar *= scale!!.y
        }

        if (rotation == null) {
            lVar += position.x
            rVar += position.x
            tVar += position.y
            bVar += position.y
            ary[offset++] = lVar
            ary[offset++] = tVar
            ary[offset++] = u1
            ary[offset++] = v1
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca

            ary[offset++] = rVar
            ary[offset++] = tVar
            ary[offset++] = u2
            ary[offset++] = v1
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca

            ary[offset++] = lVar
            ary[offset++] = bVar
            ary[offset++] = u1
            ary[offset++] = v2
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca

            ary[offset++] = rVar
            ary[offset++] = bVar
            ary[offset++] = u2
            ary[offset++] = v2
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca
        } else {
            val s = Math.sin(rotation!!.value.toDouble()).toFloat()
            val c = Math.cos(rotation!!.value.toDouble()).toFloat()
            val x = position.x
            val y = position.y
            ary[offset++] = lVar * c - tVar * s + x
            ary[offset++] = lVar * s + tVar * c + y
            ary[offset++] = u1
            ary[offset++] = v1
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca

            ary[offset++] = rVar * c - tVar * s + x
            ary[offset++] = rVar * s + tVar * c + y
            ary[offset++] = u2
            ary[offset++] = v1
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca

            ary[offset++] = lVar * c - bVar * s + x
            ary[offset++] = lVar * s + bVar * c + y
            ary[offset++] = u1
            ary[offset++] = v2
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca

            ary[offset++] = rVar * c - bVar * s + x
            ary[offset++] = rVar * s + bVar * c + y
            ary[offset++] = u2
            ary[offset++] = v2
            ary[offset++] = cr
            ary[offset++] = cg
            ary[offset++] = cb
            ary[offset++] = ca
        }
    }
}
