package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin

class BreakAnimator(
    listener: GameObjectListener,
    private val scene: Scene,
    private val stat: StatisticV2,
    private val showMark: Boolean,
    bgSprite: Rectangle?
) : GameObject() {

    private val arrows = Array(4) { Sprite(0f, 0f, ResourceManager.getInstance().getTexture("play-warningarrow")!!.deepCopy()) }
    private var length = 0f
    private var time = 0f
    private var passfail: Sprite? = null
    private var ending: String? = null
    private var mark: Sprite? = null
    private var isbreak = false
    private var over = false
    private var dimRectangle: Rectangle? = null

    init {
        this.dimRectangle = bgSprite
        listener.addPassiveObject(this)

        for (i in 0 until 4) {
            arrows[i].registerEntityModifier(
                LoopEntityModifier(
                    SequenceEntityModifier(
                        FadeInModifier(0.05f),
                        DelayModifier(0.1f),
                        FadeOutModifier(0.1f)
                    )
                )
            )
            if (i > 1) {
                arrows[i].setFlippedHorizontal(true)
            }
        }
        arrows[0].setPosition(Utils.toRes(64).toFloat(), Utils.toRes(72).toFloat())
        arrows[1].setPosition(Utils.toRes(64).toFloat(), Config.getRES_HEIGHT() - arrows[1].getHeight())
        arrows[2].setPosition(Config.getRES_WIDTH() - arrows[1].getWidth() - Utils.toRes(64).toFloat(), Utils.toRes(72).toFloat())
        arrows[3].setPosition(
            Config.getRES_WIDTH() - arrows[1].getWidth() - Utils.toRes(64).toFloat(),
            Config.getRES_HEIGHT() - arrows[1].getHeight()
        )
    }

    fun isBreak(): Boolean = isbreak

    fun isOver(): Boolean {
        val isover = over
        over = false
        return isover
    }

    fun init(length: Float) {
        if (this.length > 0 && time < this.length) return
        isbreak = true
        over = false
        this.length = length
        time = 0f
        ending = if (stat.getHp() > 0.5f) "pass" else "fail"
        val center = PointF(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        passfail = SpritePool.getInstance().getCenteredSprite("section-$ending", center)
        scene.attachChild(passfail, 0)
        passfail!!.setVisible(false)

        for (i in 0 until 4) {
            arrows[i].setVisible(false)
            arrows[i].setIgnoreUpdate(true)
            scene.attachChild(arrows[i], 0)
        }
        if (showMark) {
            val zeroRect = ResourceManager.getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), "0")!!
            mark = Sprite(
                Config.getRES_WIDTH() - zeroRect.getWidth() * 11f,
                Utils.toRes(5).toFloat(),
                ResourceManager.getInstance().getTexture("ranking-${stat.getMark()}-small")
            )
            mark!!.setScale(1.2f)
            scene.attachChild(mark, 0)
        }

        System.gc()
    }

    private fun setBgFade(percent: Float) {
        if (dimRectangle != null && !Config.isNoChangeDimInBreaks()) {
            dimRectangle!!.setAlpha((1 - Config.getBackgroundBrightness()) * (1 - percent))
        }
    }

    private fun resumeBgFade() {
        if (dimRectangle != null && !Config.isNoChangeDimInBreaks()) {
            dimRectangle!!.setAlpha(1 - Config.getBackgroundBrightness())
        }
    }

    override fun update(dt: Float) {
        if (length == 0f || time >= length) return
        time += dt

        if (length > 3 && time > (length - 1) / 2 && time - dt < (length - 1) / 2) {
            passfail!!.setVisible(true)
            passfail!!.registerEntityModifier(
                SequenceEntityModifier(
                    DelayModifier(0.25f), FadeOutModifier(0.025f),
                    DelayModifier(0.025f), FadeInModifier(0.025f),
                    DelayModifier(0.6725f), FadeOutModifier(0.3f)
                )
            )

            val sound = ResourceManager.getInstance().getCustomSound("section$ending", 1)
            sound?.play()
        }
        if (length - time <= 1 && length - time + dt > 1) {
            for (sp in arrows) {
                sp.setVisible(true)
                sp.setIgnoreUpdate(false)
            }
            if (Multiplayer.isMultiplayer) RoomScene.chat.dismiss()
        }
        if (length > 1) {
            when {
                time < 0.5f -> setBgFade(time * 2)
                length - time < 0.5f -> setBgFade((length - time) * 2)
                time >= 0.5f && time - dt < 0.5f -> setBgFade(1f)
            }
        }

        if (time >= length) {
            isbreak = false
            over = true
            resumeBgFade()
            mark?.detachSelf()
            for (sp in arrows) {
                sp.detachSelf()
            }
            passfail!!.detachSelf()
            SpritePool.getInstance().putSprite("section-$ending", passfail!!)
        }
    }
}
