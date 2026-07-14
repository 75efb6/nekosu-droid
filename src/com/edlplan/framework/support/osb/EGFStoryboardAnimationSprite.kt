package com.edlplan.framework.support.osb

import com.edlplan.edlosbsupport.elements.StoryboardAnimationSprite
import com.edlplan.framework.support.batch.`object`.MultipleFlippableTextureQuad
import org.anddev.andengine.opengl.texture.region.TextureRegion
import java.util.ArrayList

class EGFStoryboardAnimationSprite(context: OsbContext) : EGFStoryboardSprite(context) {

    override fun update(time: Double) {
        super.update(time)
        val sprite = this.sprite as StoryboardAnimationSprite
        var idx = (Math.max(0.0, time - sprite.startTime()) / sprite.frameDelay).toInt()
        if (idx >= sprite.frameCount) {
            when (sprite.loopType) {
                StoryboardAnimationSprite.LoopType.LoopOnce -> idx = sprite.frameCount - 1
                StoryboardAnimationSprite.LoopType.LoopForever -> idx %= sprite.frameCount
            }
        }
        (textureQuad as MultipleFlippableTextureQuad).switchTexture(idx)
    }

    override fun onLoad() {
        val sprite = this.sprite as StoryboardAnimationSprite
        val textureQuad = MultipleFlippableTextureQuad()

        val paths: MutableList<TextureRegion> = ArrayList(sprite.frameCount)
        for (i in 0 until sprite.frameCount) {
            paths.add(context.texturePool!!.get(sprite.buildPath(i)))
        }
        textureQuad.initialWithTextureList(paths)
        textureQuad.switchTexture(0)
        textureQuad.position.x.value = sprite.startX
        textureQuad.position.y.value = sprite.startY
        textureQuad.anchor = sprite.origin.value
        this.textureQuad = textureQuad
    }

}
