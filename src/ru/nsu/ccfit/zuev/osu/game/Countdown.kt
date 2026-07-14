package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.modifier.DelayModifier
import org.anddev.andengine.entity.modifier.FadeInModifier
import org.anddev.andengine.entity.modifier.FadeOutModifier
import org.anddev.andengine.entity.modifier.ParallelEntityModifier
import org.anddev.andengine.entity.modifier.RotationModifier
import org.anddev.andengine.entity.modifier.ScaleModifier
import org.anddev.andengine.entity.modifier.SequenceEntityModifier
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite

class Countdown(
    private val listener: GameObjectListener,
    private var scene: Scene?,
    private val speed: Float,
    offset: Float,
    time: Float
) : GameObject() {

    private val ready: Sprite
    private val count1: Sprite
    private val count2: Sprite
    private val count3: Sprite
    private val go: Sprite
    private var timepassed: Float

    init {
        timepassed = -time + COUNTDOWN_LENGTH * speed
        val center = Utils.trackToRealCoords(PointF(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f))

        ready = CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("ready")!!)
        ready.registerEntityModifier(
            SequenceEntityModifier(
                ParallelEntityModifier(
                    FadeInModifier(COUNTDOWN_LENGTH * speed / 9),
                    RotationModifier(COUNTDOWN_LENGTH * speed / 9, -90f, 0f)
                ),
                DelayModifier(COUNTDOWN_LENGTH * speed / 9),
                ParallelEntityModifier(
                    FadeOutModifier(COUNTDOWN_LENGTH * speed / 9),
                    ScaleModifier(COUNTDOWN_LENGTH * speed / 9, 1f, 1.5f)
                )
            )
        )
        ready.setRotation(-90f)
        ready.setVisible(false)
        ready.setIgnoreUpdate(true)

        count3 = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("count3"))
        count3.setPosition(0f, center.y - count3.getHeight() / 2)
        count3.setVisible(false)
        count3.setIgnoreUpdate(true)
        count3.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                DelayModifier(COUNTDOWN_LENGTH * speed * 8 / 18),
                FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)
            )
        )

        count2 = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("count2"))
        count2.setPosition(Config.getRES_WIDTH() - count2.getWidth(), center.y - count2.getHeight() / 2)
        count2.setVisible(false)
        count2.setIgnoreUpdate(true)
        count2.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                DelayModifier(COUNTDOWN_LENGTH * speed * 5 / 18),
                FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)
            )
        )

        count1 = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("count1"))
        count1.setPosition(center.x - count1.getWidth() / 2, center.y - count1.getHeight() / 2)
        count1.setVisible(false)
        count1.setIgnoreUpdate(true)
        count1.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                DelayModifier(COUNTDOWN_LENGTH * speed * 2 / 18),
                FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)
            )
        )

        go = CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("go")!!)
        go.registerEntityModifier(
            SequenceEntityModifier(
                ParallelEntityModifier(
                    FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                    RotationModifier(COUNTDOWN_LENGTH * speed / 18, -180f, 0f)
                ),
                DelayModifier(COUNTDOWN_LENGTH * speed / 18),
                FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)
            )
        )
        go.setRotation(-180f)
        go.setVisible(false)
        go.setIgnoreUpdate(true)

        scene!!.attachChild(ready, 0)
        scene!!.attachChild(go, 0)
        scene!!.attachChild(count1, 0)
        scene!!.attachChild(count2, 0)
        scene!!.attachChild(count3, 0)
    }

    private fun playIfNotNull(resname: String) {
        val sound = ResourceManager.getInstance().getCustomSound(resname, 1)
        sound?.play()
    }

    override fun update(dt: Float) {
        if (scene == null) return
        timepassed += dt

        if (timepassed >= 0 && timepassed - dt < 0) {
            playIfNotNull("readys")
            ready.setVisible(true)
            ready.setIgnoreUpdate(false)
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 2 / 6 && timepassed - dt < COUNTDOWN_LENGTH * speed * 2 / 6) {
            playIfNotNull("count3s")
            count3.setVisible(true)
            count3.setIgnoreUpdate(false)
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 3 / 6 && timepassed - dt < COUNTDOWN_LENGTH * speed * 3 / 6) {
            playIfNotNull("count2s")
            count2.setVisible(true)
            count2.setIgnoreUpdate(false)
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 4 / 6 && timepassed - dt < COUNTDOWN_LENGTH * speed * 4 / 6) {
            playIfNotNull("count1s")
            count1.setVisible(true)
            count1.setIgnoreUpdate(false)
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 5 / 6 && timepassed - dt < COUNTDOWN_LENGTH * speed * 5 / 6) {
            playIfNotNull("gos")
            go.setVisible(true)
            go.setIgnoreUpdate(false)
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed && timepassed - dt < COUNTDOWN_LENGTH * speed) {
            this.scene = null
            listener.removePassiveObject(this)
            ready.detachSelf()
            go.detachSelf()
            count1.detachSelf()
            count2.detachSelf()
            count3.detachSelf()
        }
    }

    companion object {
        @JvmField
        val COUNTDOWN_LENGTH = 3f
    }
}
