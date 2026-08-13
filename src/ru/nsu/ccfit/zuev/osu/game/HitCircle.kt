package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.Shape
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.util.modifier.IModifier
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinManager
import androidx.core.util.Supplier
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import java.util.concurrent.atomic.AtomicBoolean

class HitCircle(
    pScene: Scene? = null,
    pListener: GameObjectListener? = null,
    pData: GameObjectData? = null,
    pApproach: Sprite? = null,
    pCombo: Combo? = null,
    pNumber: CircleNumber? = null,
    pStat: ru.nsu.ccfit.zuev.osu.scoring.StatisticV2? = null,
    pLeadIn: Float = 0f,
    pSpeed: Float = 0f,
    pScale: Float = 0f,
    pFadeIn: Float = 0f,
    pTime: Float = 0f,
    pApproachTime: Float = 0f
) : GameObject() {

    private var scene: Scene? = pScene
    private var listener: GameObjectListener? = pListener
    private var data: GameObjectData? = pData
    private var approach: Sprite? = pApproach
    private var combo: Combo? = pCombo
    private var number: CircleNumber? = pNumber
    private var stat: ru.nsu.ccfit.zuev.osu.scoring.StatisticV2? = pStat
    private var leadIn: Float = pLeadIn
    private var speed: Float = pSpeed
    private var scale: Float = pScale
    private var fadeIn: Float = pFadeIn
    private var time: Float = pTime
    private var approachTime: Float = pApproachTime

    private var overlay: Sprite? = null
    private var reverseArrow: AnimSprite? = null
    private var endOverlay: AnimSprite? = null
    private var bodyColor: RGBColor? = null
    private var hit: Sprite? = null
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

    private var soundId = 0
    private var sampleSet = 0
    private var addition = 0
    private var isFirstNote = false
    private var comboNumber = -1
    private var showCombo = true
    private var dataReverse = false

    init {
        if (pScene != null && pListener != null && pData != null && pApproach != null && pCombo != null && pNumber != null && pStat != null) {
            radius = (Config.getHitCircleRadius() * scale)
            val pos = data!!.getPos()
            approach!!.setPosition(
                pos.x - approach!!.getTextureRegion().getWidth().toFloat() / 2,
                pos.y - approach!!.getTextureRegion().getHeight().toFloat() / 2
            )
            hit = SpritePool.getInstance().getCenteredSprite("hitcircle", pos)
            hit!!.setAlpha(0f)
            hit!!.setScale(scale)

            if (data!!.getTime() > 0 && GameHelper.isPerfect()) {
                hit!!.setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT)
            }

            comboNumber = data!!.getComboNum()
            showCombo = data!!.isShowCombo()
            dataReverse = data!!.isReverse()
            this.pos = pos

            numberRect = OsuSkin.get().getTexture(
                when {
                    comboNumber >= 100 -> "score-999"
                    comboNumber >= 10 -> "score-99"
                    else -> "score-9"
                }
            )

            overlay = SpritePool.getInstance().getCenteredSprite("hitcircleoverlay", pos)
            overlay!!.setScale(scale)
            overlay!!.setAlpha(0f)

            var num = comboNumber + 1
            if (OsuSkin.get().isLimitComboTextLength()) {
                num %= 10
            }
            number = GameObjectPool.getInstance().getNumber(num)
            number!!.init(pos, GameHelper.scale)
            number!!.setAlpha(0f)

            if (OsuSkin.get().isComboNumbers() && !showCombo) {
                number!!.setNum(comboNumber)
                isNumbered = true
            }

            bodyColor = RGBColor(1f, 1f, 1f)
            val comboColor = OsuSkin.get().getComboColor(comboNumber % OsuSkin.get().getComboCount())
            bodyColor!!.set(
                comboColor.r(), comboColor.g(), comboColor.b()
            )
            if (bodyColor != null) {
                hit!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
                if (showCombo && combo != null && combo!!.getBlazingColor() != null) {
                    combo!!.setColor(blendColors(combo!!.getBlazingColor()!!, bodyColor ?: RGBColor(1f, 1f, 1f)))
                }
            }

            approach!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
            approach!!.setScale(scale * 2)
            approach!!.setAlpha(0f)
            if (GameHelper.isHidden()) {
                approach!!.setVisible(Config.isShowFirstApproachCircle() && isFirstNote)
            }

            hitTime = time + leadIn * speed
            this.timeFading = fadeIn * speed
            fadeInT = hitTime - this.timeFading
            preTime = hitTime - approachTime

            if (showCombo && combo != null) {
                number!!.setCombo(combo!!.getNum())
            }

            if (GameHelper.isHidden()) {
                val fadeInDuration = time * 0.4f * GameHelper.getTimeMultiplier()
                val fadeOutDuration = time * 0.3f * GameHelper.getTimeMultiplier()
                number!!.registerEntityModifiers(Supplier {
                    SequenceEntityModifier(
                        FadeInModifier(fadeInDuration),
                        FadeOutModifier(fadeOutDuration)
                    )
                })
                overlay!!.registerEntityModifier(
                    SequenceEntityModifier(
                        FadeInModifier(fadeInDuration),
                        FadeOutModifier(fadeOutDuration)
                    )
                )
                hit!!.registerEntityModifier(
                    SequenceEntityModifier(
                        FadeInModifier(fadeInDuration),
                        FadeOutModifier(fadeOutDuration)
                    )
                )
            }

            scene!!.attachChild(number, 0)
            scene!!.attachChild(overlay, 0)
            scene!!.attachChild(hit, 0)
            scene!!.attachChild(approach)

            if (dataReverse) {
                reverseArrow = SpritePool.getInstance().getAnimSprite("reversearrow", SkinManager.getFrames("reversearrow"))
                reverseArrow!!.setPosition(pos.x, pos.y)
                reverseArrow!!.setSize(
                    (reverseArrow!!.getWidth() * scale).toFloat(),
                    (reverseArrow!!.getHeight() * scale).toFloat()
                )
                if (bodyColor != null) {
                    reverseArrow!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
                }
                reverseArrow!!.setAlpha(0f)
            }

            if (data!!.isEndCombo() && comboNumber != -1) {
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
                numberText = ScoreNumber(textX, textY, comboNumber.toString(), scale, true)
                numberText!!.setAlpha(0f)
            }
        }
    }

    fun init(listener: GameObjectListener, scene: Scene, pos: PointF, time: Float, r: Float, g: Float, b: Float, scale: Float, comboNum: Int, sound: Int, tempSound: String?, isFirst: Boolean) {
        this.replayObjectData = null
        this.scale = scale
        this.pos = pos
        this.listener = listener
        this.scene = scene
        this.soundId = sound
        this.sampleSet = 0
        this.addition = 0
        this.time = time
        this.isFirstNote = isFirst
        this.comboNumber = comboNum
        this.showCombo = true
        this.dataReverse = false
        elapsed = 0f
        startHit = false
        kiai = GameHelper.isKiai
        bodyColor = RGBColor(r, g, b)

        if (!Utils.isEmpty(tempSound)) {
            val group = tempSound!!.split(":")
            this.sampleSet = group[0].toInt()
            this.addition = group[1].toInt()
        }

        radius = Utils.toRes(128) * scale / 2
        radius *= radius

        hit = SpritePool.getInstance().getCenteredSprite("hitcircle", pos)
        hit!!.setAlpha(0f)
        hit!!.setScale(scale)
        hit!!.setColor(r, g, b)

        overlay = SpritePool.getInstance().getCenteredSprite("hitcircleoverlay", pos)
        overlay!!.setScale(scale)
        overlay!!.setAlpha(0f)

        if (approach == null) {
            approach = SpritePool.getInstance().getSprite("approachcircle")
        }
        approach!!.setColor(r, g, b)
        approach!!.setScale(scale * 2)
        approach!!.setAlpha(0f)
        Utils.putSpriteAnchorCenter(pos, approach!!)
        if (GameHelper.isHidden()) {
            approach!!.setVisible(Config.isShowFirstApproachCircle() && isFirst)
        }

        var num = comboNum + 1
        if (OsuSkin.get().isLimitComboTextLength()) {
            num %= 10
        }
        number = GameObjectPool.getInstance().getNumber(num)
        number!!.init(pos, GameHelper.scale)
        number!!.setAlpha(0f)

        if (GameHelper.isHidden()) {
            val fadeInDuration = time * 0.4f * GameHelper.getTimeMultiplier()
            val fadeOutDuration = time * 0.3f * GameHelper.getTimeMultiplier()
            number!!.registerEntityModifiers(Supplier {
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            })
            overlay!!.registerEntityModifier(
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            )
            hit!!.registerEntityModifier(
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            )
        }

        scene.attachChild(number, 0)
        scene.attachChild(overlay, 0)
        scene.attachChild(hit, 0)
        scene.attachChild(approach)

        hitTime = time
        preTime = hitTime

        circle = false
        border = false
        completed = false
        timeover = false
        passed = true
        active = true
        removed.set(false)
        hasEffect = false
        hitEffect = null
        hitSound = null
        isNumbered = false
        numberText = null
        complexNum = null
        this.comboNum = null
        reverseArrow = null
        endOverlay = null
        preApproachCircle = null
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

    private fun playSound() {
        if (approach!!.getScaleX() <= scale * 1.5f) {
            listener!!.playSound("hitnormal", sampleSet, addition)
        }
    }

    private fun removeFromScene() {
        if (scene == null) return
        overlay!!.detachSelf()
        hit!!.detachSelf()
        number!!.detachSelf()
        approach!!.detachSelf()
        if (reverseArrow != null) reverseArrow!!.detachSelf()
        if (comboNum != null) comboNum!!.detachSelf()
        if (complexNum != null) complexNum!!.detachSelf()
        if (numberText != null) numberText!!.detachSelf()
        listener!!.removeObject(this)
        GameObjectPool.getInstance().putCircle(this)
        GameObjectPool.getInstance().putNumber(number!!)
        scene = null
    }

    override fun cleanupFromScene() {
        if (scene == null) return
        overlay!!.detachSelf()
        hit!!.detachSelf()
        number!!.detachSelf()
        approach!!.detachSelf()
        if (reverseArrow != null) reverseArrow!!.detachSelf()
        if (comboNum != null) comboNum!!.detachSelf()
        if (complexNum != null) complexNum!!.detachSelf()
        if (numberText != null) numberText!!.detachSelf()
        listener!!.removeObject(this)
        scene = null
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
        val pos = this.pos
        hit!!.setPosition(pos.x, pos.y)
        if (overlay != null) overlay!!.setPosition(pos.x, pos.y)
        if (reverseArrow != null) {
            reverseArrow!!.setPosition(
                pos.x - reverseArrow!!.getWidth() / 2,
                pos.y - reverseArrow!!.getHeight() / 2
            )
        }
        if (complexNum != null) {
            complexNum!!.setPosition(pos.x, pos.y)
        }
        if (comboNum != null) {
            comboNum!!.setPosition(pos.x + radius, pos.y - radius * 0.9f)
        }
        if (numberText != null) {
            numberText!!.setPosition(pos.x + radius, pos.y + radius * 0.9f)
        }
        approach!!.setPosition(
            pos.x - approach!!.getTextureRegion().getWidth().toFloat() / 2,
            pos.y - approach!!.getTextureRegion().getHeight().toFloat() / 2
        )
        number!!.setPosition(pos.x, pos.y)
    }

    fun removed(): Boolean = removed.get()

    fun setKiai(kiai: Boolean) {
        this.kiai = kiai
    }

    fun getEndTime(): Float = time + leadIn * speed

    override fun update(dt: Float) {
        if (elapsed < 0) return

        if (replayObjectData != null) {
            if (elapsed - time + dt / 2 > replayObjectData!!.accuracy / 1000f) {
                val acc = Math.abs(replayObjectData!!.accuracy / 1000f)
                if (acc <= GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                    playSound()
                }
                listener!!.registerAccuracy(replayObjectData!!.accuracy / 1000.0)
                elapsed = -1f
                listener!!.onCircleHit(id, replayObjectData!!.accuracy / 1000f, pos, endsCombo, replayObjectData!!.result, bodyColor ?: RGBColor(1f, 1f, 1f))
                removeFromScene()
                return
            }
        } else if (elapsed * 2 > time) {
            val hitOffset = checkHit()
            if (!hitOffset.isNaN()) {
                val signAcc = elapsed - time + hitOffset
                val acc = Math.abs(signAcc)
                if (acc <= GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                    playSound()
                }
                listener!!.registerAccuracy(signAcc.toDouble())
                elapsed = -1f
                startHit = true
                listener!!.onCircleHit(id, signAcc, pos, endsCombo, 0.toByte(), bodyColor ?: RGBColor(1f, 1f, 1f))
                removeFromScene()
                return
            }
        }

        if (GameHelper.isKiai) {
            val kiaiModifier = Math.max(0.0, 1 - GameHelper.globalTime / GameHelper.kiaiTickLength).toFloat() * 0.50f
            val r = Math.min(1f, bodyColor!!.r() + (1 - bodyColor!!.r()) * kiaiModifier)
            val g = Math.min(1f, bodyColor!!.g() + (1 - bodyColor!!.g()) * kiaiModifier)
            val b = Math.min(1f, bodyColor!!.b() + (1 - bodyColor!!.b()) * kiaiModifier)
            kiai = true
            hit!!.setColor(r, g, b)
        } else if (kiai) {
            hit!!.setColor(bodyColor!!.r(), bodyColor!!.g(), bodyColor!!.b())
            kiai = false
        }

        if (autoPlay && elapsed - time >= 0) {
            playSound()
            elapsed = -1f
            listener!!.onCircleHit(id, 0f, pos, endsCombo, ResultType.HIT300.id, bodyColor ?: RGBColor(1f, 1f, 1f))
            removeFromScene()
            return
        }

        elapsed += dt

        if (!GameHelper.isHidden()) {
            val duration = 0.4f * Math.min(1f, time / 0.45f)
            val percent = (elapsed / duration).coerceIn(0f, 1f)

            if (elapsed < duration) {
                hit!!.setAlpha(percent)
                overlay!!.setAlpha(percent)
                number!!.setAlpha(percent)
            }
        }

        if (elapsed < time) {
            val percentage = elapsed / time
            approach!!.setScale(scale * (1 + 2f * (1 - percentage)))
            if (!GameHelper.isHidden() || (isFirstNote && Config.isShowFirstApproachCircle())) {
                if (elapsed < time / 2) {
                    val p = elapsed * 2 / time
                    approach!!.setAlpha(p)
                } else if (!GameHelper.isHidden()) {
                    approach!!.setAlpha(1f)
                }
            }
        } else if (!autoPlay) {
            approach!!.setAlpha(0f)

            if (elapsed > time + GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                elapsed = -1f
                val forcedScore = if (replayObjectData == null) 0.toByte() else replayObjectData!!.result
                removeFromScene()
                listener!!.onCircleHit(id, 10f, pos, false, forcedScore, bodyColor ?: RGBColor(1f, 1f, 1f))
            }
        }
    }

    private fun checkHit(): Float {
        for (i in 0 until listener!!.getCursorsCount()) {
            val inPosition = Utils.squaredDistance(pos, listener!!.getMousePos(i)) <= radius
            if (GameHelper.isRelaxMod() && elapsed - time >= 0 && inPosition) {
                return 0f
            }
            if (!inPosition && !GameHelper.isAutopilotMod()) {
                continue
            }
            val isPressed = listener!!.isMousePressed(this, i)
            if (isPressed) {
                return if (inPosition && Config.isFixFrameOffset()) (listener!!.downFrameOffset(i) / 1000f).toFloat() else 0f
            }
        }
        return Float.NaN
    }

    override fun tryHit(dt: Float) {
        if (elapsed * 2 > time) {
            val hitOffset = checkHit()
            if (!hitOffset.isNaN()) {
                val signAcc = elapsed - time + hitOffset
                val acc = Math.abs(signAcc)
                if (acc <= GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                    playSound()
                }
                listener!!.registerAccuracy(signAcc.toDouble())
                elapsed = -1f
                listener!!.onCircleHit(id, signAcc, pos, endsCombo, 0.toByte(), bodyColor ?: RGBColor(1f, 1f, 1f))
                removeFromScene()
            }
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

    fun timeOver(): Boolean = timeover
    fun isCompleted(): Boolean = completed
    fun isActive(): Boolean = active
    fun setCompleted(completed: Boolean) {
        this.completed = completed
    }
    fun setActive(active: Boolean) {
        this.active = active
    }
    fun getHitCirclePos(): PointF = this.pos
    fun getTime(): Float = hitTime
    fun getData(): GameObjectData? = data
    fun isShowCombo(): Boolean = showCombo
    fun getComboNum(): Int = comboNumber
    fun setCombo(combo: Combo) {
        this.combo!!.setColor(combo.getBlazingColor()!!)
    }
}
