package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.modifier.FadeInModifier
import org.anddev.andengine.entity.modifier.LoopEntityModifier
import org.anddev.andengine.entity.modifier.RotationModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.util.concurrent.atomic.AtomicBoolean

class Spinner(
    pData: GameObjectData? = null,
    pListener: GameObjectListener? = null,
    pScene: Scene? = null,
    pStat: ru.nsu.ccfit.zuev.osu.scoring.StatisticV2? = null,
    pSpeed: Float = 0f,
    pLeadIn: Float = 0f,
    pFadeIn: Float = 0f,
    pTime: Float = 0f,
    pApproachTime: Float = 0f
) : GameObject() {
    private var data: GameObjectData? = pData
    private var listener: GameObjectListener? = pListener
    private var scene: Scene? = pScene
    private var stat: ru.nsu.ccfit.zuev.osu.scoring.StatisticV2? = pStat
    private var speed: Float = pSpeed
    private var fadeIn: Float = pFadeIn
    private var approachTime: Float = pApproachTime

    private var rotateRect: Sprite? = null
    private var completed = false
    private var removed = false
    private var active = false
    private var currentRotation = 0f
    private var maxRotation = 0f
    private var neededRotation = 0f
    private var spinnerTime = 0f
    private var elapsed = 0f
    private var number: CircleNumber? = null
    private var numberText: ScoreNumber? = null
    private var spinnerBgSprite: Sprite? = null
    private var spinnerTrack: Sprite? = null
    private var spinnerRemove: Sprite? = null

    private var approachCircle: Sprite? = null
    private var approachTop: Sprite? = null
    private var approachBot: Sprite? = null

    private var bonusScore = 0
    private var scoreGained = 0
    private var totalTime = 0f
    private val removed2 = AtomicBoolean(false)

    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    init {
        if (pData != null && pScene != null) {
            radius = Utils.toRes((Config.getHitCircleRadius() * 1.5f).toInt()).toFloat()
            centerX = Config.getRES_WIDTH() / 2f
            centerY = Config.getRES_HEIGHT() / 2f
            val hitTime = pTime + pLeadIn * pSpeed
            spinnerTime = data!!.getTime() - pTime
            totalTime = spinnerTime
            elapsed = hitTime + GameHelper.getDifficulty()!!.getTimePre()

            neededRotation = 8 + spinnerTime * 3
            neededRotation = Math.max(neededRotation.toDouble(), 3.0).toFloat()

            spinnerBgSprite = SpritePool.getInstance().getCenteredSprite("spinnerbg", PointF(centerX, centerY))
            spinnerBgSprite!!.setColor(0.75f, 0.75f, 0.75f)
            spinnerBgSprite!!.setAlpha(0f)

            spinnerTrack = SpritePool.getInstance().getCenteredSprite("spinnertrack", PointF(centerX, centerY))
            spinnerTrack!!.setAlpha(0f)

            spinnerRemove = SpritePool.getInstance().getCenteredSprite("spinner-ol", PointF(centerX, centerY))
            spinnerRemove!!.setColor(0.75f, 0.75f, 0.75f)
            spinnerRemove!!.setAlpha(0f)

            spinnerTrack!!.registerEntityModifier(
                LoopEntityModifier(RotationModifier(360f, 360f, 0f))
            )

            approachCircle = SpritePool.getInstance().getCenteredSprite(
                "spinner-approachcircle",
                PointF(centerX, centerY - radius)
            )
            approachCircle!!.setColor(0.75f, 0.75f, 0.75f)
            approachCircle!!.setAlpha(0f)
            approachCircle!!.setScale(0f)

            approachTop = SpritePool.getInstance().getCenteredSprite("spinner-top", PointF(centerX, centerY))
            approachTop!!.setAlpha(0f)

            approachBot = SpritePool.getInstance().getCenteredSprite("spinner-bottom", PointF(centerX, centerY))
            approachBot!!.setAlpha(0f)
            approachBot!!.setScale(0.01f)
        }
    }

    fun updateRotation(delta: Float) {
        if (!active) return
        currentRotation += delta * 100f
    }

    override fun update(dt: Float) {
        if (removed2.get()) return
        if (dt != 0f) elapsed += dt

        if (!active && elapsed > 0) {
            active = true
            listener!!.onSpinnerStart(this)
            spinnerBgSprite!!.registerEntityModifier(FadeInModifier(0.3f))
            scene!!.attachChild(spinnerBgSprite)
            spinnerTrack!!.registerEntityModifier(FadeInModifier(0.3f))
            scene!!.attachChild(spinnerTrack)
            spinnerRemove!!.registerEntityModifier(FadeInModifier(0.3f))
            scene!!.attachChild(spinnerRemove)
            approachCircle!!.registerEntityModifier(FadeInModifier(0.3f))
            scene!!.attachChild(approachCircle)
            approachTop!!.registerEntityModifier(FadeInModifier(0.3f))
            scene!!.attachChild(approachTop)
            approachBot!!.registerEntityModifier(FadeInModifier(0.3f))
            scene!!.attachChild(approachBot)
        }

        if (active && elapsed > 0) {
            spinnerRemove!!.setRotation(currentRotation)
            spinnerBgSprite!!.setRotation(currentRotation / 3)
            spinnerTrack!!.setRotation(-currentRotation)
            approachCircle!!.setRotation(currentRotation)
            approachCircle!!.setScale(1 - elapsed / totalTime)

            if (currentRotation > maxRotation) {
                maxRotation = currentRotation
                if (maxRotation >= neededRotation) {
                    listener!!.onSpinnerEnd(this, true, bonusScore.toFloat())
                } else {
                    listener!!.onSpinnerEnd(this, false, bonusScore.toFloat())
                }
            }

            if (dt != 0f && maxRotation >= neededRotation && currentRotation == maxRotation) {
                bonusScore++
                if (bonusScore % 10 == 0) scoreGained++
                listener!!.onSpinnerEnd(this, true, bonusScore.toFloat())
            }
        }

        if (elapsed >= totalTime) {
            completed = true
            active = false

            spinnerBgSprite!!.detachSelf()
            spinnerTrack!!.detachSelf()
            spinnerRemove!!.detachSelf()
            approachCircle!!.detachSelf()
            approachTop!!.detachSelf()
            approachBot!!.detachSelf()

            SpritePool.getInstance().putSprite("spinnerbg", spinnerBgSprite!!)
            SpritePool.getInstance().putSprite("spinnertrack", spinnerTrack!!)
            SpritePool.getInstance().putSprite("spinner-ol", spinnerRemove!!)
            SpritePool.getInstance().putSprite("spinner-approachcircle", approachCircle!!)
            SpritePool.getInstance().putSprite("spinner-top", approachTop!!)
            SpritePool.getInstance().putSprite("spinner-bottom", approachBot!!)

            if (currentRotation >= neededRotation) {
                listener!!.onSpinnerEnd(this, true, bonusScore.toFloat())
            } else {
                listener!!.onSpinnerEnd(this, false, 0f)
            }
            listener!!.removePassiveObject(this)
            removed2.set(true)
            removed = true
        }
    }

    fun isCompleted(): Boolean = completed
    fun isActive(): Boolean = active
    fun isStopped(): Boolean = true
    fun canBeActive(): Boolean = false
    fun getPos(): PointF = PointF(centerX, centerY)
    fun getRadius(): Float = radius
}
