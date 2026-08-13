package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.entity.modifier.ParallelEntityModifier
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.util.modifier.IModifier
import com.reco1l.framework.lang.Execution
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite

class GameEffect(private val texname: String) : GameObject(), IEntityModifier.IEntityModifierListener {

    @JvmField
    var hit: Sprite

    init {
        if (isAnimationEffect(texname) && ResourceManager.getInstance().isTextureLoaded("$texname-0")) {
            val loadedScoreBarTextures = ArrayList<String>()
            for (i in 0 until 60) {
                if (ResourceManager.getInstance().isTextureLoaded("$texname-$i"))
                    loadedScoreBarTextures.add("$texname-$i")
                else break
            }
            val animHit = AnimSprite(0f, 0f, 60f, *loadedScoreBarTextures.toTypedArray())
            animHit.setLoopType(AnimSprite.LoopType.STOP)
            hit = animHit
        } else {
            hit = Sprite(0f, 0f, ResourceManager.getInstance().getTexture(texname))
        }
    }

    fun setColor(color: RGBColor) {
        hit.setColor(color.r(), color.g(), color.b())
    }

    fun init(scene: Scene, pos: PointF, scale: Float, vararg entityModifiers: IEntityModifier) {
        if (hit is AnimSprite) {
            (hit as AnimSprite).setAnimTime(0f)
        }
        hit.setPosition(
            pos.x - hit.getTextureRegion().getWidth() / 2f,
            pos.y - hit.getTextureRegion().getHeight() / 2f
        )
        hit.registerEntityModifier(ParallelEntityModifier(this, *entityModifiers))
        hit.setScale(scale)
        hit.setAlpha(1f)
        hit.detachSelf()
        hit.setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT)
        scene.attachChild(hit)
    }

    fun setBlendFunction(sourceBlend: Int, destBlend: Int) {
        hit.setBlendFunction(sourceBlend, destBlend)
    }

    fun getTexname(): String = texname

    override fun update(dt: Float) {}

    override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {}

    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
        Execution.updateThread {
            hit.detachSelf()
            hit.clearEntityModifiers()
            GameObjectPool.getInstance().putEffect(this)
        }
    }

    companion object {
        private val animationEffects = HashSet(
            listOf(
                "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
            )
        )

        private fun isAnimationEffect(textureName: String): Boolean = animationEffects.contains(textureName)
    }
}
