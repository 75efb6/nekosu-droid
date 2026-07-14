package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import androidx.core.util.Supplier
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.modifier.IEntityModifier
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

class CircleNumber : Entity(0f, 0f) {

    private var num: Int = 0

    constructor(number: Int) : super() {
        num = number
        val snum = Math.abs(number).toString()

        for (i in snum.indices) {
            val tex = ResourceManager.getInstance().getTextureWithPrefix(
                OsuSkin.get().getHitCirclePrefix(), snum[i].toString()
            )
            attachChild(Sprite(0f, 0f, tex))
        }
    }

    constructor(region: TextureRegion) : super() {
        num = 0
        attachChild(Sprite(0f, 0f, region))
    }

    fun init(pos: PointF, scale: Float) {
        var s = scale * OsuSkin.get().getComboTextScale()
        val overlap = OsuSkin.get().getHitCircleOverlap()
        var maxWidthScaled = 0f
        var maxHeight = 0f

        for (i in 0 until getChildCount()) {
            val sprite = getChild(i) as Sprite
            sprite.setScale(s)
            sprite.setPosition(maxWidthScaled, 0f)
            maxWidthScaled += sprite.getWidthScaled() - overlap
            maxHeight = maxOf(maxHeight, sprite.getHeight())
        }

        val maxWidth = getLastChild().getX() + (getLastChild() as Sprite).getWidth()
        setPosition(pos.x - maxWidth / 2f, pos.y - maxHeight / 2f)
    }

    fun getNum(): Int = num

    fun setNum(num: Int) {
        this.num = num
    }

    fun setCombo(num: Int) {
        this.num = num
    }

    fun setColor(color: RGBColor) {
        for (i in 0 until getChildCount()) {
            getChild(i).setColor(color.r(), color.g(), color.b())
        }
    }

    fun attachToScene(scene: Scene) {
        scene.attachChild(this)
    }

    fun detachFromScene() {
        this.detachSelf()
    }

    fun startFading() {
        // no-op placeholder
    }

    override fun setAlpha(pAlpha: Float) {
        val count = getChildCount()
        if (count > 0) {
            for (i in 0 until count) {
                getChild(i).setAlpha(pAlpha)
            }
        }
        super.setAlpha(pAlpha)
    }

    override fun getAlpha(): Float {
        return if (getFirstChild() != null) getFirstChild().getAlpha() else super.getAlpha()
    }

    fun registerEntityModifiers(modifier: Supplier<IEntityModifier>) {
        for (i in 0 until getChildCount()) {
            getChild(i).registerEntityModifier(modifier.get())
        }
    }
}
