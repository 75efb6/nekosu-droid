package com.edlplan.andengine.texture

import com.edlplan.framework.math.IQuad
import com.edlplan.framework.math.Quad
import com.edlplan.framework.math.RectF
import com.edlplan.framework.math.Vec2

abstract class AbstractTexture {
    abstract fun getTextureId(): Int

    abstract fun getTexture(): GLTexture

    abstract fun getHeight(): Int

    abstract fun getWidth(): Int

    abstract fun toTexturePosition(x: Float, y: Float): Vec2

    abstract fun getRawQuad(): IQuad

    fun toTexturePosition(v: Vec2): Vec2 {
        return toTexturePosition(v.x, v.y)
    }

    fun toTextureRect(raw: RectF): RectF {
        return toTextureRect(raw.left, raw.top, raw.right, raw.bottom)
    }

    fun toTextureRect(l: Float, t: Float, r: Float, b: Float): RectF {
        val lt = toTexturePosition(l, t)
        val rb = toTexturePosition(r, b)
        return RectF.ltrb(lt.x, lt.y, rb.x, rb.y)
    }

    fun toTextureQuad(q: IQuad): Quad {
        val r = Quad()
        r.set(
            toTexturePosition(q.topLeft),
            toTexturePosition(q.topRight),
            toTexturePosition(q.bottomLeft),
            toTexturePosition(q.bottomRight))
        return r
    }
}
