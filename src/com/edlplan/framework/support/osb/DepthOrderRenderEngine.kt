package com.edlplan.framework.support.osb

import com.edlplan.framework.support.batch.`object`.TextureQuadBatch
import com.edlplan.framework.support.graphics.BaseCanvas
import com.edlplan.framework.support.graphics.BlendType
import com.edlplan.framework.utils.advance.LinkedNode

open class DepthOrderRenderEngine {
    var first: LinkedNode<EGFStoryboardSprite>
    var end: LinkedNode<EGFStoryboardSprite>

    init {
        first = LinkedNode()
        end = LinkedNode()
        first.insertToNext(end)
    }

    fun add(sprite: EGFStoryboardSprite) {
        var s: LinkedNode<EGFStoryboardSprite>? = end.pre
        while (s != first) {
            if (s!!.value.sprite.depth < sprite.sprite.depth) {
                s.insertToNext(LinkedNode(sprite))
                return
            }
            s = s.pre
        }
        first.insertToNext(LinkedNode(sprite))
    }

    fun remove(sprite: EGFStoryboardSprite) {
        var s: LinkedNode<EGFStoryboardSprite>? = first.next
        while (s != end) {
            if (s!!.value === sprite) {
                s.removeFromList()
                break
            }
            s = s.next
        }
    }

    fun draw(canvas: BaseCanvas) {
        val batch = TextureQuadBatch.getDefaultBatch()
        var s: LinkedNode<EGFStoryboardSprite>? = first.next
        while (s != end) {
            if (s!!.value.textureQuad.alpha.value < 0.001) {
                s = s.next
                continue
            }
            canvas.blendSetting.setBlendType(if (s.value.blendMode.value) BlendType.Additive else BlendType.Normal)
            batch.add(s.value.textureQuad)
            s = s.next
        }
    }


}
