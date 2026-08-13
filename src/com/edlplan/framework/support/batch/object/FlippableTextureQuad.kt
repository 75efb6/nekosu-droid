package com.edlplan.framework.support.batch.`object`

import com.edlplan.framework.math.Anchor
import com.edlplan.framework.math.Color4
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.utils.BooleanRef
import com.edlplan.framework.utils.FloatRef
import com.edlplan.framework.utils.Vec2Ref
import org.anddev.andengine.opengl.texture.region.TextureRegion

open class FlippableTextureQuad : ATextureQuad() {

    var size: Vec2 = Vec2()

    var position: Vec2Ref = Vec2Ref()

    var anchor: Anchor = Anchor.Center

    var scale: Vec2Ref? = null

    var rotation: FloatRef? = null

    var alpha: FloatRef = FloatRef(1f)

    var accentColor: Color4? = null

    var u1: Float = 0f
    var v1: Float = 0f
    var u2: Float = 0f
    var v2: Float = 0f

    var flipH: BooleanRef = BooleanRef(false)

    var flipV: BooleanRef = BooleanRef(false)

    fun enableScale(): FlippableTextureQuad {
        if (scale == null) {
            scale = Vec2Ref()
            scale!!.x.value = 1f
            scale!!.y.value = 1f
        }
        return this
    }

    fun enableColor(): FlippableTextureQuad {
        if (accentColor == null) {
            accentColor = Color4.White.copyNew()
        }
        return this
    }

    fun syncColor(color: Color4): FlippableTextureQuad {
        accentColor = color
        return this
    }

    fun syncAlpha(ref: FloatRef): FlippableTextureQuad {
        this.alpha = ref
        return this
    }

    fun enableRotation(): FlippableTextureQuad {
        if (rotation == null) {
            rotation = FloatRef()
        }
        return this
    }

    fun syncRotation(ref: FloatRef): FlippableTextureQuad {
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

        var u1: Float
        var v1: Float
        var u2: Float
        var v2: Float

        if (flipH.value) {
            u1 = this.u2
            u2 = this.u1
        } else {
            u1 = this.u1
            u2 = this.u2
        }

        if (flipV.value) {
            v1 = this.v2
            v2 = this.v1
        } else {
            v1 = this.v1
            v2 = this.v2
        }

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
            lVar *= scale!!.x.value
            rVar *= scale!!.x.value
            tVar *= scale!!.y.value
            bVar *= scale!!.y.value
        }

        if (rotation == null) {
            lVar += position.x.value
            rVar += position.x.value
            tVar += position.y.value
            bVar += position.y.value
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
            val x = position.x.value
            val y = position.y.value
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
