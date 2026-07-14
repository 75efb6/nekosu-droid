package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.util.modifier.IModifier
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.util.concurrent.atomic.AtomicBoolean

class Slider(
    pData: GameObjectData? = null,
    pListener: GameObjectListener? = null,
    pScene: Scene? = null,
    pStat: ru.nsu.ccfit.zuev.osu.scoring.StatisticV2? = null,
    pSpeed: Float = 0f,
    pScale: Float = 0f,
    pLeadIn: Float = 0f,
    pFadeIn: Float = 0f,
    pTime: Float = 0f,
    pApproachTime: Float = 0f,
    pCombo: Combo? = null,
    pNumber: CircleNumber? = null,
    pSliderBody: Sprite? = null
) : GameObject() {

    private var data: GameObjectData? = pData
    private var listener: GameObjectListener? = pListener
    private var scene: Scene? = pScene
    private var stat: ru.nsu.ccfit.zuev.osu.scoring.StatisticV2? = pStat
    private var speed: Float = pSpeed
    private var scale: Float = pScale
    private var leadIn: Float = pLeadIn
    private var fadeIn: Float = pFadeIn
    private var time: Float = pTime
    private var approachTime: Float = pApproachTime
    private var combo: Combo? = pCombo
    private var number: CircleNumber? = pNumber

    private var overlay: Sprite? = null
    private var reverseArrow: AnimSprite? = null
    private var endOverlay: AnimSprite? = null
    private var bodyColor: RGBColor? = null
    private var hit: Sprite? = null
    private lateinit var approach: Sprite
    private var circle = false
    private var border = false
    private var completed = false
    private var timeover = false
    private var kiai = false
    private var hasNumber = true
    private var isNumbered = false
    private var active = true
    private var preTime = 0f
    private var passed = true
    private var elapsed = 0f
    private var sliderBreak = false
    private var reverseHit = false
    private var sliderFinished = false
    private var ballFinished = false
    private var startSet = false
    private var maxScore = 0

    private var numberText: ScoreNumber? = null
    private var complexNum: CentredSprite? = null
    private var comboNum: CentredSprite? = null

    private var numSprites = arrayOfNulls<Sprite>(2)
    private var preApproachCircle: Sprite? = null

    private var radius = 0f
    private var approachScale = 1f
    private var fadeInT = 0f
    private var timeFading = 0f

    private var numberRect: org.anddev.andengine.opengl.texture.region.TextureRegion? = null

    private val removed = AtomicBoolean(false)
    private var hasEffect = false
    private var hitEffect: GameEffect? = null
    private var hitSound: GameEffect? = null

    private var pathFlow = 0f
    private var reverseDone = false
    private var reverseNum = 0
    private var sliderLength = 0f
    private var sliderBodyList: ArrayList<PointF>? = null
    private var ball: Sprite? = null
    private var tickSprite: Sprite? = null
    private var tickSet = false
    private var tickTimer = 0f
    private var tickCount = 0
    private var firstHit = false
    private var followCircle: Sprite? = null
    private var reverseFollowCircle: Sprite? = null

    init {
        if (pData != null && pListener != null && pScene != null && pStat != null && pCombo != null && pNumber != null) {
            radius = (Config.getHitCircleRadius() * scale)
            val pos = Utils.trackToRealCoords(data!!.getPos())
            hit = SpritePool.getInstance().getCenteredSprite("hitcircle", pos)
            hit!!.setAlpha(0.95f)
            hit!!.setScale(scale)
            approach = SpritePool.getInstance().getCenteredSprite("approachcircle", pos)

            if (data!!.getTime() > 0 && GameHelper.isPerfect()) {
                hit!!.setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT)
            }

            overlay = SpritePool.getInstance().getCenteredSprite("hitcircleoverlay", pos)
            overlay!!.setScale(scale)

            numberRect = OsuSkin.get().getTexture(
                when {
                    data!!.getComboNum() >= 100 -> "score-999"
                    data!!.getComboNum() >= 10 -> "score-99"
                    else -> "score-9"
                }
            )

            number = CircleNumber(numberRect!!)
            number!!.setScale(scale)
            if (OsuSkin.get().isComboNumbers() && !data!!.isShowCombo()) {
                number!!.setNum(data!!.getComboNum())
                isNumbered = true
            }
            number!!.setPosition(pos.x, pos.y)

            bodyColor = RGBColor(1f, 1f, 1f)
            val comboColor = OsuSkin.get().getComboColor(data!!.getComboNum() % OsuSkin.get().getComboCount())
            bodyColor!!.set(comboColor.r(), comboColor.g(), comboColor.b())
            if (bodyColor != null) {
                hit!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
            }

            hitTime = time + leadIn * speed
            elapsed = hitTime + GameHelper.getDifficulty()!!.getTimePre()
            this.timeFading = fadeIn * speed
            fadeInT = elapsed - this.timeFading
            preTime = hitTime - approachTime

            if (data!!.isShowCombo() && combo != null) {
                number!!.setCombo(combo!!.getNum())
            }

            val pos2 = Utils.trackToRealCoords(data!!.getPos())
            approach.setPosition(
                pos2.x - approach.getTextureRegion().getWidth().toFloat() / 2,
                pos2.y - approach.getTextureRegion().getHeight().toFloat() / 2
            )

            if (data!!.isReverse()) {
                reverseArrow = SpritePool.getInstance().getAnimSprite("reversearrow", SkinManager.getFrames("reversearrow"))
                reverseArrow!!.setPosition(pos.x, pos.y)
                reverseArrow!!.setSize(
                    (reverseArrow!!.getWidth() * scale).toFloat(),
                    (reverseArrow!!.getHeight() * scale).toFloat()
                )
                if (bodyColor != null) reverseArrow!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
                reverseArrow!!.setAlpha(0f)
            }

            sliderLength = data!!.getSliderLength()
            sliderBodyList = data!!.getSliderBody(sliderLength)

            if (data!!.isShowCombo() && combo != null) {
                val textX = pos.x + radius
                val textY = pos.y - radius * 0.9f
                comboNum = CentredSprite(textX, textY, OsuSkin.get().getTexture("combo-prefix")!!)
                if (comboNum != null) {
                    comboNum!!.setScale(scale)
                    if (bodyColor != null) comboNum!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
                    comboNum!!.setAlpha(0f)
                }
            }
            if (isNumbered) {
                val textX = pos.x + radius
                val textY = pos.y + radius * 0.9f
                numberText = ScoreNumber(textX, textY, data!!.getComboNum().toString(), scale, true)
                numberText!!.setAlpha(0f)
            }
        }
    }

    private fun blendColors(c1: RGBColor, c2: RGBColor): RGBColor {
        val avg = RGBColor((c1.r() + c2.r()) / 2, (c1.g() + c2.g()) / 2, (c1.b() + c2.b()) / 2)
        if (c1.r() == 0.75f && c1.g() == 0.75f && c1.b() == 0.75f) return c2
        if (c2.r() == 0.75f && c2.g() == 0.75f && c2.b() == 0.75f) return c1
        if (avg.r() > 0.75f) {
            avg.set(0.75f, 0.75f, 0.75f)
        } else if (avg.r() < 0.4f) {
            avg.set(0.4f, 0.4f, 0.4f)
        }
        return avg
    }

    fun initColor(combo: RGBColor, num: Int) {
        bodyColor = RGBColor(1f, 1f, 1f)
        bodyColor!!.set(combo.r(), combo.g(), combo.b())
        hit!!.setColor(combo.r(), combo.g(), combo.b())
        if (overlay != null) overlay!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
        if (complexNum != null) complexNum!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
        if (comboNum != null) comboNum!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
        if (reverseArrow != null) reverseArrow!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
        if (number != null) number!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
        if (numberText != null) numberText!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
    }

    fun initPos() {
        val pos = Utils.trackToRealCoords(data!!.getPos())
        hit!!.setPosition(pos.x, pos.y)
        if (overlay != null) overlay!!.setPosition(pos.x, pos.y)
        if (reverseArrow != null) {
            reverseArrow!!.setPosition(
                pos.x - reverseArrow!!.getWidth() / 2,
                pos.y - reverseArrow!!.getHeight() / 2
            )
        }
        if (complexNum != null) complexNum!!.setPosition(pos.x, pos.y)
        if (comboNum != null) comboNum!!.setPosition(pos.x + radius, pos.y - radius * 0.9f)
        if (numberText != null) numberText!!.setPosition(pos.x + radius, pos.y + radius * 0.9f)
        approach.setPosition(
            pos.x - approach.getTextureRegion().getWidth().toFloat() / 2,
            pos.y - approach.getTextureRegion().getHeight().toFloat() / 2
        )
        number!!.setPosition(pos.x, pos.y)
    }

    fun removed(): Boolean = removed.get()
    fun setKiai(kiai: Boolean) { this.kiai = kiai }
    fun getEndTime(): Float = time + leadIn * speed

    override fun update(dt: Float) {
        if (removed.get()) return

        if (circle) {
            approach.setAlpha(approach.getAlpha() + dt / approachTime * 2)
            approach.setScale(approachScale - dt * 3 * (1 - scale))
        }
        if (!circle) {
            approach.setScale(approachScale)
            approach.setAlpha(1f)
            approach.setVisible(true)
            circle = true
        }
        approachScale = hit!!.getScaleX()

        if (dt != 0f) elapsed += dt
        if (!passed) {
            if (elapsed >= hitTime) {
                listener!!.passed(this, false)
                passed = true
            }
        }

        if (!border && elapsed >= fadeInT) {
            border = true
            scene!!.attachChild(approach)
            scene!!.attachChild(hit)
            scene!!.attachChild(overlay, 0)
            if (reverseArrow != null) scene!!.attachChild(reverseArrow, 0)
            if (data!!.isShowCombo()) {
                if (complexNum != null) scene!!.attachChild(complexNum, 0)
                if (comboNum != null) scene!!.attachChild(comboNum, 0)
            }
            if (isNumbered) scene!!.attachChild(numberText, 0)
            number!!.attachToScene(scene!!)
            number!!.startFading()
        }

        if (elapsed <= fadeInT) {
            var alpha = 0f
            if (this.timeFading != 0f) alpha = (elapsed - (hitTime - approachTime - this.timeFading)) / this.timeFading
            if (alpha < 0) alpha = 0f
            hit!!.setAlpha(alpha)
            overlay!!.setAlpha(alpha)
            if (reverseArrow != null) reverseArrow!!.setAlpha(alpha)
            if (complexNum != null) complexNum!!.setAlpha(alpha)
            if (comboNum != null) comboNum!!.setAlpha(alpha)
            if (numberText != null) numberText!!.setAlpha(alpha)
        } else if (hit!!.getAlpha() < 1) {
            var alpha = hit!!.getAlpha() + dt / GameHelper.getDifficulty()!!.getFadeInTime() * 3
            if (alpha > 1) alpha = 1f
            hit!!.setAlpha(0.95f)
            if (complexNum != null) complexNum!!.setAlpha(1f)
            if (comboNum != null) comboNum!!.setAlpha(1f)
            if (numberText != null) numberText!!.setAlpha(1f)
            overlay!!.setAlpha(1f)
        }

        if (!timeover && elapsed >= hitTime) {
            timeover = true
            if (!active) return
            if (!startHit) {
                startHit = true
                firstHit = true
            }
            hit!!.setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT)
            var numRatio = 1f
            if (data!!.getTime() < 0) {
                numRatio = 0f
                listener!!.objectClicked(this, PointF(0f, 0f), 0)
            } else {
                listener!!.objectClicked(this, PointF(data!!.getPos().x, data!!.getPos().y), 0)
            }
            number!!.setNum(0)
            number!!.detachFromScene()
            number = CircleNumber(numberRect!!)
            number!!.setScale(numRatio * scale)
            number!!.setNum(0)
            number!!.setPosition(hit!!.getX() + radius, hit!!.getY() + radius)
            number!!.detachFromScene()

            hit!!.registerEntityModifier(
                SequenceEntityModifier(
                    ScaleModifier(0.2f, scale, scale * 1.5f),
                    FadeOutModifier(0.2f)
                )
            )
            overlay!!.registerEntityModifier(FadeOutModifier(0.2f))
            if (reverseArrow != null) reverseArrow!!.registerEntityModifier(FadeOutModifier(0.2f))

            listener!!.onSliderHit(id, 10, PointF(data!!.getPos().x, data!!.getPos().y), PointF(data!!.getPos().x, data!!.getPos().y), false, Combo.hitColor(data!!.getComboNum()), 0)
        }

        if (elapsed > hitTime + GameHelper.getDifficulty()!!.getDifficultyRange(GameHelper.drain, 300f, 250f, 200f)) {
            listener!!.onSliderHit(id, 0, PointF(data!!.getPos().x, data!!.getPos().y), PointF(data!!.getPos().x, data!!.getPos().y), false, Combo.hitColor(data!!.getComboNum()), 0)
        }

        if (data!!.isEndCombo()) {
            listener!!.removePassiveObject(this)
            completed = true
        }
    }

    fun setModSpeed(mSpeed: Float) {
        if (mSpeed < 1) return
    }

    fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {}
    fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
        pItem.setAlpha(0f)
        pItem.setVisible(false)
        pItem.setIgnoreUpdate(true)
        if (pItem == hit) {
            removed.set(true)
            listener!!.passed(this, true)
            clearEffects()
        }
    }

    fun clearEffects() {
        if (hasEffect) {
            hitEffect?.hit?.detachSelf()
            hitSound?.hit?.detachSelf()
            hitEffect = null
            hitSound = null
            hasEffect = false
        }
    }

    fun tryHit(time: Float, coords: PointF): Boolean {
        if (!active) return false
        if (hitTime > elapsed) return false
        if (coords.x == 0f || coords.y == 0f) return false

        val maxRadius = Config.getHitCircleRadius() * 1.35f * 2
        val dist = Utils.distance(coords, data!!.getPos())
        val relDist = (maxRadius - dist) / maxRadius
        if (relDist > 1) return true
        return false
    }

    fun timeOver(): Boolean = timeover
    fun isCompleted(): Boolean = completed
    fun isActive(): Boolean = active
    fun setCompleted(completed: Boolean) { this.completed = completed }
    fun setActive(active: Boolean) { this.active = active }
    fun getSliderPos(): PointF = data!!.getPos()
    fun getTime(): Float = hitTime
    fun getData(): GameObjectData = data!!
    fun isShowCombo(): Boolean = data!!.isShowCombo()
    fun getComboNum(): Int = data!!.getComboNum()
    fun setCombo(combo: Combo) { this.combo!!.setColor(combo.getBlazingColor()!!) }
}
