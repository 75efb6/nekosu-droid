package com.edlplan.framework.support.osb

import com.edlplan.edlosbsupport.command.Target
import com.edlplan.edlosbsupport.player.CommandBooleanHandleTimeline
import com.edlplan.edlosbsupport.player.CommandColor4HandleTimeline
import com.edlplan.edlosbsupport.player.CommandFloatHandleTimeline
import com.edlplan.edlosbsupport.player.CommandHandleTimeline
import com.edlplan.edlosbsupport.player.PlayingSprite
import com.edlplan.framework.support.batch.`object`.FlippableTextureQuad
import com.edlplan.framework.utils.BooleanRef

open class EGFStoryboardSprite(protected var context: OsbContext) : PlayingSprite() {

    lateinit var textureQuad: FlippableTextureQuad
    var blendMode: BooleanRef = BooleanRef(false)

    override fun onLoad() {
        textureQuad = FlippableTextureQuad()
        textureQuad.setTextureAndSize(context.texturePool!!.get(sprite.spriteFilename))
        textureQuad.position.x.value = sprite.startX
        textureQuad.position.y.value = sprite.startY
        textureQuad.anchor = sprite.origin.value
    }

    override fun onAddedToScene() {
        context.engines!![sprite.layer]!!.add(this)
    }

    override fun onRemoveFromScene() {
        context.engines!![sprite.layer]!!.remove(this)
    }

    override fun createByTarget(target: Target): CommandHandleTimeline<*>? {
        when (target) {
            Target.X -> return CommandFloatHandleTimeline(textureQuad.position.x)
            Target.Y -> return CommandFloatHandleTimeline(textureQuad.position.y)
            Target.ScaleX -> return CommandFloatHandleTimeline(textureQuad.enableScale().scale!!.x)
            Target.ScaleY -> return CommandFloatHandleTimeline(textureQuad.enableScale().scale!!.y)
            Target.Alpha -> return CommandFloatHandleTimeline(textureQuad.alpha)
            Target.Rotation -> return CommandFloatHandleTimeline(textureQuad.enableRotation().rotation!!)
            Target.Color -> return object : CommandColor4HandleTimeline() {
                init {
                    value = textureQuad.enableColor().accentColor
                }
            }
            Target.FlipH -> return object : CommandBooleanHandleTimeline() {
                init {
                    value = textureQuad.flipH
                }
            }
            Target.FlipV -> return object : CommandBooleanHandleTimeline() {
                init {
                    value = textureQuad.flipV
                }
            }
            Target.BlendingMode -> return object : CommandBooleanHandleTimeline() {
                init {
                    value = blendMode
                }
            }
        }
        return null
    }
}
