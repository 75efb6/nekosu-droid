package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import com.edlplan.framework.math.Vec2
import com.edlplan.framework.math.line.LinePath
import com.edlplan.osu.support.slider.SliderBody2D
import com.reco1l.framework.lang.Execution
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.util.MathUtils
import org.anddev.andengine.util.modifier.IModifier
import org.anddev.andengine.util.modifier.ease.EaseQuadIn
import org.anddev.andengine.util.modifier.ease.EaseQuadOut
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper
import ru.nsu.ccfit.zuev.osu.helper.ModifierListener
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinManager
import java.util.Arrays
import java.util.BitSet

class Slider() : GameObject() {

    private val startCircle: Sprite
    private val endCircle: Sprite
    private val startOverlay: Sprite
    private val endOverlay: Sprite
    private val approachCircle: Sprite
    private val startArrow: Sprite
    private val endArrow: Sprite
    private val ticks = ArrayList<Sprite>()
    private var startPosition = PointF()
    private var endPosition = PointF()
    private var scene: Scene? = null
    private var listener: GameObjectListener? = null
    private var timing: TimingPoint? = null
    private var number: CircleNumber? = null
    private var path: GameHelper.SliderPath? = null
    private var passedTime = 0.0
    private var preTime = 0.0
    private var tickTime = 0.0
    private var maxTime = 0.0
    private var scale = 0f
    private var repeatCount = 0
    private var reverse = false
    private var soundId = IntArray(3)
    private var sampleSet = IntArray(3)
    private var addition = IntArray(3)

    private var soundIdIndex = 0
    private var ticksGot = 0
    private var ticksTotal = 0
    private var currentTick = 0
    private var tickInterval = 0.0

    private var ball: AnimSprite? = null
    private var followCircle: Sprite? = null

    private var tmpPoint = PointF()
    private var ballAngle = 0f

    private var kiai = false
    private var color = RGBColor()
    private val circleColor = RGBColor()

    private var firstHitAccuracy = 0
    private val tickSet = BitSet()
    private var tickIndex = 0

    private var lengthCache = FloatArray(0)

    private var superPath: LinePath? = null
    private var preStageFinish = false

    private var abstractSliderBody: SliderBody2D? = null

    private var mIsOver = false
    private var mIsAnimating = false
    private var mWasInRadius = false

    private val mFollowEnterListener = object : ModifierListener() {
        override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
            mIsAnimating = false
        }
    }
    private val mFollowLeaveListener = object : ModifierListener() {
        override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
            mIsAnimating = false
            if (mIsOver) {
                Execution.updateThread { pItem.detachSelf() }
            }
        }
    }

    init {
        startCircle = SpritePool.getInstance().getSprite("sliderstartcircle")
        endCircle = SpritePool.getInstance().getSprite("sliderendcircle")
        startOverlay = SpritePool.getInstance().getSprite("sliderstartcircleoverlay")
        endOverlay = SpritePool.getInstance().getSprite("sliderendcircleoverlay")
        approachCircle = SpritePool.getInstance().getSprite("approachcircle")
        startArrow = SpritePool.getInstance().getSprite("reversearrow")
        endArrow = SpritePool.getInstance().getSprite("reversearrow")
    }

    fun init(
        listener: GameObjectListener, scene: Scene,
        pos: PointF, offset: Float, time: Float, r: Float, g: Float,
        b: Float, scale: Float, num: Int, sound: Int, repeats: Int,
        length: Float, data: String, timing: TimingPoint?,
        customSound: String?, tempSound: String?, isFirstNote: Boolean, realTime: Double,
        sliderPath: GameHelper.SliderPath?
    ) {
        this.listener = listener
        this.scene = scene
        this.timing = timing
        this.scale = scale
        this.pos = pos
        passedTime = -time.toDouble()
        preTime = time.toDouble()
        path = sliderPath
            ?: GameHelper.calculatePath(
                Utils.realToTrackCoords(pos),
                data.split("[|]".toRegex()).toTypedArray(),
                Math.max(0f, length), offset
            )

        val lenCount = path!!.length.size
        if (lengthCache.size < lenCount) {
            lengthCache = FloatArray(lenCount)
        }
        for (i in 0 until lenCount) {
            lengthCache[i] = path!!.length[i]
        }

        var num2 = num + 1
        if (OsuSkin.get().isLimitComboTextLength()) {
            num2 %= 10
        }
        number = GameObjectPool.getInstance().getNumber(num2)
        number!!.init(pos, scale)

        val timingPoint = GameHelper.controlPoints.getTimingPointAt(realTime)
        val speedMultiplier = GameHelper.controlPoints.getDifficultyPointAt(realTime).speedMultiplier

        val scoringDistance = GameHelper.speed.toDouble() * speedMultiplier
        val velocity = scoringDistance / timingPoint!!.beatLength
        var spanDuration = length.toDouble() / velocity
        if (spanDuration <= 0) {
            spanDuration = 0.0
        }

        mIsOver = false
        mIsAnimating = false
        mWasInRadius = false

        maxTime = (spanDuration / 1000f).toDouble()
        ball = null
        followCircle = null
        repeatCount = repeats
        reverse = false
        startHit = false
        ticksGot = 0
        ticksTotal = 1
        tickTime = 0.0
        currentTick = 0
        tickIndex = 0
        firstHitAccuracy = 0
        tickSet.clear()
        kiai = GameHelper.isKiai
        preStageFinish = false
        color.set(r, g, b)
        if (!OsuSkin.get().isSliderFollowComboColor()) {
            val bodyColor = OsuSkin.get().getSliderBodyColor()
            color.set(bodyColor.r(), bodyColor.g(), bodyColor.b())
        }
        circleColor.set(r, g, b)

        if (soundId.size < repeats + 1) {
            soundId = IntArray(repeats + 1)
            sampleSet = IntArray(repeats + 1)
            addition = IntArray(repeats + 1)
        }

        Arrays.fill(soundId, sound)
        Arrays.fill(sampleSet, 0)
        Arrays.fill(addition, 0)

        if (customSound != null) {
            val pars = customSound.split("[|]".toRegex()).toTypedArray()
            for (i in soundId.indices) {
                if (i < pars.size) {
                    soundId[i] = pars[i].toInt()
                }
            }
        }

        if (!Utils.isEmpty(tempSound)) {
            val pars = tempSound!!.split("[|]".toRegex()).toTypedArray()
            for (i in pars.indices) {
                val group = pars[i].split(":".toRegex()).toTypedArray()
                if (i < sampleSet.size) {
                    sampleSet[i] = group[0].toInt()
                }
                if (i < addition.size) {
                    addition[i] = group[1].toInt()
                }
            }
        }
        soundIdIndex = 1

        startCircle.setScale(scale)
        startCircle.setColor(r, g, b)
        startCircle.setAlpha(0f)
        startPosition = pos
        Utils.putSpriteAnchorCenter(pos, startCircle)

        startOverlay.setScale(scale)
        startOverlay.setAlpha(0f)
        Utils.putSpriteAnchorCenter(pos, startOverlay)

        approachCircle.setColor(r, g, b)
        approachCircle.setScale(scale * 2)
        approachCircle.setAlpha(0f)
        Utils.putSpriteAnchorCenter(pos, approachCircle)
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(Config.isShowFirstApproachCircle() && isFirstNote)
        }

        var endPos = pos
        if (!path!!.points.isEmpty()) {
            endPos = path!!.points[path!!.points.size - 1]
        }
        endCircle.setScale(scale)
        endCircle.setColor(r, g, b)
        endCircle.setAlpha(0f)
        endPosition = endPos
        Utils.putSpriteAnchorCenter(if (Config.isSnakingInSliders()) pos else endPos, endCircle)

        endOverlay.setScale(scale)
        endOverlay.setAlpha(0f)
        Utils.putSpriteAnchorCenter(if (Config.isSnakingInSliders()) pos else endPos, endOverlay)

        scene.attachChild(startOverlay, 0)
        if (repeatCount > 2 && path!!.points.size >= 2) {
            startArrow.setAlpha(0f)
            startArrow.setScale(scale)
            startArrow.setRotation(
                MathUtils.radToDeg(
                    Utils.direction(
                        path!!.points[0], path!!.points[1]
                    )
                )
            )
            Utils.putSpriteAnchorCenter(pos, startArrow)
            scene.attachChild(startArrow, 0)
        }

        val fadeInDuration: Float

        if (GameHelper.isHidden()) {
            fadeInDuration = time * 0.4f * GameHelper.getTimeMultiplier()
            val fadeOutDuration = time * 0.3f * GameHelper.getTimeMultiplier()

            number!!.registerEntityModifiers {
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            }

            startCircle.registerEntityModifier(
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            )

            startOverlay.registerEntityModifier(
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            )

            endCircle.registerEntityModifier(
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeInDuration)
                )
            )

            endOverlay.registerEntityModifier(
                SequenceEntityModifier(
                    FadeInModifier(fadeInDuration),
                    FadeOutModifier(fadeOutDuration)
                )
            )
        } else {
            fadeInDuration = 0.4f * Math.min(
                1f,
                time / (GameHelper.ar2ms(10.0).toFloat() / 1000f)
            ) * GameHelper.getTimeMultiplier()

            number!!.registerEntityModifiers { FadeInModifier(fadeInDuration) }
            startCircle.registerEntityModifier(FadeInModifier(fadeInDuration))
            startOverlay.registerEntityModifier(FadeInModifier(fadeInDuration))
            endCircle.registerEntityModifier(FadeInModifier(fadeInDuration))
            endOverlay.registerEntityModifier(FadeInModifier(fadeInDuration))
        }
        scene.attachChild(number, 0)
        scene.attachChild(startCircle, 0)
        scene.attachChild(approachCircle)
        scene.attachChild(endOverlay, 0)
        if (repeatCount > 1) {
            endArrow.setAlpha(0f)
            endArrow.setScale(scale)
            if (path!!.points.size >= 2) {
                val lastIndex = path!!.points.size - 1
                endArrow.setRotation(
                    MathUtils.radToDeg(
                        Utils.direction(
                            path!!.points[lastIndex], path!!.points[lastIndex - 1]
                        )
                    )
                )
            }
            Utils.putSpriteAnchorCenter(if (Config.isSnakingInSliders()) pos else endPos, endArrow)
            scene.attachChild(endArrow, 0)
        }
        scene.attachChild(endCircle, 0)

        tickInterval = timing!!.getBeatLength() * speedMultiplier
        var tickCount = (maxTime * GameHelper.tickRate / tickInterval).toInt()
        if (tickInterval.isNaN() || tickInterval < GameHelper.tickRate / 1000.0) {
            tickCount = 0
        }
        if ((maxTime * GameHelper.tickRate / tickInterval) - (maxTime * GameHelper.tickRate / tickInterval).toInt() < 0.001f) {
            tickCount--
        }
        ticks.clear()
        for (i in 1..tickCount) {
            val tick = SpritePool.getInstance().getCenteredSprite(
                "sliderscorepoint",
                getPercentPosition(
                    (i * tickInterval / (maxTime * GameHelper.tickRate)).toFloat(), null
                )
            )
            tick.setScale(scale)
            tick.setAlpha(0f)
            ticks.add(tick)
            scene.attachChild(tick, 0)
        }

        if (!path!!.points.isEmpty()) {
            superPath = LinePath()

            val sourceSize = path!!.points.size
            val distThreshold: Float = when {
                sourceSize > 10000 -> 32f
                sourceSize > 2000 -> 12f
                sourceSize > 2 -> 6f
                else -> 32f
            }
            val distThresholdSq = distThreshold * distThreshold

            var lastAdded: Vec2? = null
            for (p in path!!.points) {
                val v = Vec2(p.x, p.y)
                if (lastAdded == null || Vec2.lengthSquared(lastAdded, v) >= distThresholdSq) {
                    superPath!!.add(v)
                    lastAdded = v
                }
            }

            val lastPoint = Vec2(
                path!!.points[sourceSize - 1].x,
                path!!.points[sourceSize - 1].y
            )
            if (lastAdded == null || Vec2.lengthSquared(lastAdded, lastPoint) > 0.01f) {
                superPath!!.add(lastPoint)
            }
            superPath!!.measure()
            superPath!!.bufferLength(path!!.length[path!!.length.size - 1])
            superPath = superPath!!.fitToLinePath()
            superPath!!.measure()

            val bodyWidth = (OsuSkin.get().getSliderBodyWidth() - OsuSkin.get().getSliderBorderWidth()) * scale
            abstractSliderBody = SliderBody2D(superPath!!)
            abstractSliderBody!!.setBodyWidth(bodyWidth)
            abstractSliderBody!!.setBorderWidth(OsuSkin.get().getSliderBodyWidth() * scale)
            abstractSliderBody!!.setSliderBodyBaseAlpha(OsuSkin.get().getSliderBodyBaseAlpha())

            if (OsuSkin.get().isSliderHintEnable() && length > OsuSkin.get().getSliderHintShowMinLength()) {
                abstractSliderBody!!.setEnableHint(true)
                abstractSliderBody!!.setHintAlpha(OsuSkin.get().getSliderHintAlpha())
                abstractSliderBody!!.setHintWidth(
                    Math.min(
                        OsuSkin.get().getSliderHintWidth() * scale,
                        bodyWidth
                    )
                )
                val hintColor = OsuSkin.get().getSliderHintColor()
                if (hintColor != null) {
                    abstractSliderBody!!.setHintColor(hintColor.r(), hintColor.g(), hintColor.b())
                } else {
                    abstractSliderBody!!.setHintColor(color.r(), color.g(), color.b())
                }
            }

            abstractSliderBody!!.applyToScene(scene, Config.isSnakingInSliders())
            abstractSliderBody!!.setBodyColor(color.r(), color.g(), color.b())
            val scolor = GameHelper.sliderColor
            abstractSliderBody!!.setBorderColor(scolor.r(), scolor.g(), scolor.b())
        }

        applyBodyFadeAdjustments(fadeInDuration)
    }

    private fun getPercentPosition(percentage: Float, angle: Float?): PointF {
        if (path!!.points.isEmpty()) {
            tmpPoint.set(startPosition)
            return tmpPoint
        }

        if (percentage >= 1) {
            tmpPoint.set(endPosition)
            return tmpPoint
        } else if (percentage <= 0) {
            if (angle != null && path!!.points.size >= 2) {
                ballAngle = MathUtils.radToDeg(
                    Utils.direction(
                        path!!.points[1], startPosition
                    )
                )
            }
            tmpPoint.set(startPosition)
            return tmpPoint
        }

        val lenSize = path!!.length.size
        if (lenSize == 1) {
            tmpPoint.x = startPosition.x * percentage + path!!.points[1].x * (1 - percentage)
            tmpPoint.y = startPosition.y * percentage + path!!.points[1].y * (1 - percentage)
            return tmpPoint
        }
        var left = 0
        var right = lenSize
        var index = right / 2
        val realLength = percentage * lengthCache[lenSize - 1]
        while (left < right) {
            if (index < lenSize - 1 && lengthCache[index + 1] < realLength) {
                left = index
            } else if (lengthCache[index] >= realLength) {
                right = index
            } else {
                break
            }
            index = (right + left) / 2
        }

        var addlength = realLength - lengthCache[index]
        addlength /= lengthCache[index] - lengthCache[index + 1]
        tmpPoint.x = path!!.points[index].x * addlength + path!!.points[index + 1].x * (1 - addlength)
        tmpPoint.y = path!!.points[index].y * addlength + path!!.points[index + 1].y * (1 - addlength)
        if (angle != null) {
            ballAngle = MathUtils.radToDeg(
                Utils.direction(
                    path!!.points[index], path!!.points[index + 1]
                )
            )
        }
        return tmpPoint
    }

    private fun removeFromScene() {
        if (scene == null) return

        abstractSliderBody?.removeFromScene(scene!!)

        if (ball != null) {
            ball!!.clearEntityModifiers()
            ball!!.registerEntityModifier(FadeOutModifier(0.1f * GameHelper.getTimeMultiplier(), object : ModifierListener() {
                override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                    Execution.updateThread {
                        pItem.detachSelf()
                        SpritePool.getInstance().putAnimSprite("sliderb", pItem as AnimSprite)
                    }
                }
            }))
        }

        if (followCircle != null) {
            if (!Config.isComplexAnimations()) {
                followCircle!!.detachSelf()
            }
            SpritePool.getInstance().putSprite("sliderfollowcircle", followCircle!!)
        }
        startCircle.detachSelf()
        endCircle.detachSelf()
        startOverlay.detachSelf()
        endOverlay.detachSelf()
        approachCircle.detachSelf()
        startArrow.detachSelf()
        endArrow.detachSelf()
        for (i in ticks.indices) {
            val sp = ticks[i]
            sp.detachSelf()
            SpritePool.getInstance().putSprite("sliderscorepoint", sp)
        }
        listener?.removeObject(this)
        number?.detachSelf()
        path?.let { GameHelper.putPath(it) }
        GameObjectPool.getInstance().putSlider(this)
        number?.let { GameObjectPool.getInstance().putNumber(it) }
        scene = null
    }

    override fun cleanupFromScene() {
        if (scene == null) return
        abstractSliderBody?.removeFromScene(scene!!)
        if (ball != null) {
            ball!!.clearEntityModifiers()
            ball!!.detachSelf()
        }
        followCircle?.detachSelf()
        startCircle.detachSelf()
        endCircle.detachSelf()
        startOverlay.detachSelf()
        endOverlay.detachSelf()
        approachCircle.detachSelf()
        startArrow.detachSelf()
        endArrow.detachSelf()
        for (i in ticks.indices) {
            ticks[i].detachSelf()
        }
        number?.detachSelf()
        listener?.removeObject(this)
        scene = null
    }

    private fun over() {
        repeatCount--
        if (mWasInRadius && replayObjectData == null ||
            replayObjectData != null && replayObjectData!!.tickSet?.get(tickIndex) == true
        ) {
            if (soundIdIndex < soundId.size)
                Utils.playHitSound(
                    listener!!, soundId[soundIdIndex],
                    sampleSet[soundIdIndex], addition[soundIdIndex]
                )
            ticksGot++
            tickSet.set(tickIndex++, true)
            if (repeatCount > 0) {
                listener!!.onSliderHit(
                    id, 30, null,
                    if (reverse) startPosition else endPosition,
                    false, color, GameObjectListener.SLIDER_REPEAT
                )
            }
        } else {
            tickSet.set(tickIndex++, false)
            if (repeatCount > 0) {
                listener!!.onSliderHit(
                    id, -1, null,
                    if (reverse) startPosition else endPosition,
                    false, color, GameObjectListener.SLIDER_REPEAT
                )
            }
        }
        soundIdIndex++
        ticksTotal++
        if (repeatCount > 0) {
            reverse = !reverse
            passedTime -= maxTime
            tickTime = passedTime
            if (ball != null) {
                ball!!.setFlippedHorizontal(reverse)
            }
            for (sp in ticks) {
                sp.setAlpha(1f)
            }
            currentTick = if (reverse) ticks.size - 1 else 0
            if (reverse && repeatCount <= 2) {
                endArrow.setAlpha(0f)
            }
            if (reverse && repeatCount > 1) {
                startArrow.setAlpha(1f)
            }
            if (!reverse && repeatCount <= 2) {
                startArrow.setAlpha(0f)
            }
            (listener as GameScene).onSliderReverse(
                if (!reverse) startPosition else endPosition,
                if (reverse) endArrow.getRotation() else startArrow.getRotation(),
                color
            )
            if (passedTime >= maxTime) {
                over()
            }
            return
        }
        mIsOver = true

        var firstHitScore = 0
        if (GameHelper.isScoreV2()) {
            val diffHelper = GameHelper.difficultyHelper
            val od = GameHelper.overallDifficulty

            if (Math.abs(firstHitAccuracy) <= diffHelper.hitWindowFor300(od) * 1000) {
                firstHitScore = 300
            } else if (Math.abs(firstHitAccuracy) <= diffHelper.hitWindowFor100(od) * 1000) {
                firstHitScore = 100
            }
        }
        var score = 0
        if (ticksGot > 0) {
            score = 50
        }
        if (ticksGot >= ticksTotal / 2 && (!GameHelper.isScoreV2() || firstHitScore >= 100)) {
            score = 100
        }
        if (ticksGot >= ticksTotal && (!GameHelper.isScoreV2() || firstHitScore == 300)) {
            score = 300
        }
        if (reverse) {
            listener!!.onSliderHit(
                id, score,
                endPosition, startPosition, endsCombo, color, GameObjectListener.SLIDER_END
            )
        } else {
            listener!!.onSliderHit(
                id, score, startPosition,
                endPosition, endsCombo, color, GameObjectListener.SLIDER_END
            )
        }
        if (!startHit) {
            firstHitAccuracy =
                (GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty) * 1000 + 13).toInt()
        }
        listener!!.onSliderEnd(id, firstHitAccuracy, tickSet)

        if (Config.isComplexAnimations() && followCircle != null) {
            mIsAnimating = true

            followCircle!!.clearEntityModifiers()
            followCircle!!.registerEntityModifier(
                ParallelEntityModifier(object : ModifierListener() {
                    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                        mIsAnimating = false
                    }
                },
                    ScaleModifier(
                        0.2f * GameHelper.getTimeMultiplier(),
                        followCircle!!.getScaleX(), followCircle!!.getScaleX() * 0.8f,
                        EaseQuadOut.getInstance()
                    ),
                    AlphaModifier(
                        0.2f * GameHelper.getTimeMultiplier(),
                        followCircle!!.getAlpha(), 0f, object : ModifierListener() {
                            override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                                Execution.updateThread { pItem.detachSelf() }
                            }
                        }, EaseQuadIn.getInstance()
                    )
                )
            )
        }

        removeFromScene()
    }

    private fun isHit(): Boolean {
        val radius = Utils.sqr(64 * scale)
        for (i in 0 until listener!!.getCursorsCount()) {
            val inPosition = Utils.squaredDistance(startPosition, listener!!.getMousePos(i)) <= radius
            if (GameHelper.isRelaxMod() && passedTime >= 0 && inPosition) {
                return true
            }
            if (!inPosition && !GameHelper.isAutopilotMod()) {
                continue
            }
            if (listener!!.isMousePressed(this, i)) {
                return true
            }
        }
        return false
    }

    override fun update(dt: Float) {
        if (scene == null) return
        passedTime += dt

        if (!startHit) {
            if (passedTime > GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                startHit = true
                listener!!.onSliderHit(id, -1, null, startPosition, false, color, GameObjectListener.SLIDER_START)
                firstHitAccuracy = (passedTime * 1000).toInt()
            } else if (autoPlay && passedTime >= 0) {
                startHit = true
                Utils.playHitSound(listener!!, soundId[0], sampleSet[0], addition[0])
                ticksGot++
                listener!!.onSliderHit(id, 30, null, startPosition, false, color, GameObjectListener.SLIDER_START)
            } else if (replayObjectData != null &&
                Math.abs(replayObjectData!!.accuracy / 1000f) <= GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty) &&
                passedTime + dt / 2 > replayObjectData!!.accuracy / 1000f
            ) {
                startHit = true
                Utils.playHitSound(listener!!, soundId[0], sampleSet[0], addition[0])
                ticksGot++
                listener!!.onSliderHit(id, 30, null, startPosition, false, color, GameObjectListener.SLIDER_START)
            } else if (isHit() && -passedTime < GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                listener!!.registerAccuracy(passedTime)
                startHit = true
                Utils.playHitSound(listener!!, soundId[0], sampleSet[0], addition[0])
                ticksGot++
                firstHitAccuracy = (passedTime * 1000).toInt()
                listener!!.onSliderHit(id, 30, null, startPosition, false, color, GameObjectListener.SLIDER_START)
            }
        }

        if (GameHelper.isKiai) {
            val kiaiModifier =
                Math.max(0.0, 1.0 - GameHelper.globalTime / GameHelper.kiaiTickLength).toFloat() * 0.50f
            val r = Math.min(1f, circleColor.r() + (1 - circleColor.r()) * kiaiModifier)
            val g = Math.min(1f, circleColor.g() + (1 - circleColor.g()) * kiaiModifier)
            val b = Math.min(1f, circleColor.b() + (1 - circleColor.b()) * kiaiModifier)
            kiai = true
            startCircle.setColor(r, g, b)
            endCircle.setColor(r, g, b)
        } else if (kiai) {
            startCircle.setColor(circleColor.r(), circleColor.g(), circleColor.b())
            endCircle.setColor(circleColor.r(), circleColor.g(), circleColor.b())
            kiai = false
        }

        if (passedTime < 0) {
            val percentage = (1 + passedTime / preTime).toFloat()
            approachCircle.setScale(scale * (1 + 2f * (1 - percentage)))
            if (startHit) {
                approachCircle.setAlpha(0f)
            }
            if (percentage <= 0.5f) {
                val pct = Math.min(1f, percentage * 2)
                if (!startHit) {
                    approachCircle.setAlpha(pct)
                }

                val tickCount = ticks.size
                for (i in 0 until tickCount) {
                    if (pct > (i + 1).toFloat() / tickCount) {
                        ticks[i].setAlpha(1f)
                    }
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(pct)
                }

                if (Config.isSnakingInSliders()) {
                    if (superPath != null && abstractSliderBody != null) {
                        val l = superPath!!.measurer.maxLength() * pct

                        abstractSliderBody!!.setEndLength(l)
                        abstractSliderBody!!.onUpdate()
                    }

                    tmpPoint = getPercentPosition(pct, null)

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle)
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay)
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow)
                }
            } else if (percentage - dt / preTime <= 0.5f) {
                approachCircle.setAlpha(1f)
                for (i in ticks.indices) {
                    ticks[i].setAlpha(1f)
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(1f)
                }
                if (Config.isSnakingInSliders()) {
                    if (!preStageFinish && superPath != null && abstractSliderBody != null) {
                        abstractSliderBody!!.setEndLength(superPath!!.measurer.maxLength())
                        abstractSliderBody!!.onUpdate()
                        preStageFinish = true
                    }

                    tmpPoint = endPosition

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle)
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay)
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow)
                }
            }
            return
        } else {
            startCircle.setAlpha(0f)
            startOverlay.setAlpha(0f)

            if (Config.isSnakingInSliders() && !preStageFinish
                && superPath != null && abstractSliderBody != null
            ) {
                abstractSliderBody!!.setEndLength(superPath!!.measurer.maxLength())
                abstractSliderBody!!.onUpdate()
                preStageFinish = true
            }
        }

        if (maxTime <= 0) {
            over()
            return
        }

        if (ball == null) {
            number?.detachSelf()
            approachCircle.setAlpha(0f)

            ball = SpritePool.getInstance().getAnimSprite(
                "sliderb",
                SkinManager.getFrames("sliderb")
            )
            ball!!.setFps((0.1f * GameHelper.speed * scale / timing!!.getBeatLength()).toFloat())
            ball!!.setScale(scale)
            ball!!.setFlippedHorizontal(false)

            ball!!.registerEntityModifier(FadeInModifier(0.1f * GameHelper.getTimeMultiplier()))

            followCircle = SpritePool.getInstance().getSprite("sliderfollowcircle")
            followCircle!!.setAlpha(0f)
            if (!Config.isComplexAnimations()) {
                followCircle!!.setScale(scale)
            }

            scene!!.attachChild(ball)
            scene!!.attachChild(followCircle)
        }
        val percentage = (passedTime / maxTime).toFloat()
        val ballpos = getPercentPosition(if (reverse) 1 - percentage else percentage, ballAngle)

        val radius = 128 * scale
        var inRadius = false
        for (i in 0 until listener!!.getCursorsCount()) {
            val isPressed = listener!!.isMouseDown(i)

            if (autoPlay || (isPressed && Utils.squaredDistance(listener!!.getMousePos(i), ballpos) <= radius * radius)) {
                inRadius = true
                break
            }
            if (GameHelper.isAutopilotMod() && isPressed)
                inRadius = true
        }
        listener!!.onTrackingSliders(inRadius)
        tickTime += dt

        if (Config.isComplexAnimations()) {
            val remainTime = (maxTime * GameHelper.getTimeMultiplier() * repeatCount - passedTime).toFloat()

            if (inRadius && !mWasInRadius) {
                mWasInRadius = true
                mIsAnimating = true

                val initialScale = if (followCircle!!.getAlpha() == 0f) scale * 0.5f else followCircle!!.getScaleX()

                followCircle!!.clearEntityModifiers()
                followCircle!!.registerEntityModifier(
                    ParallelEntityModifier(mFollowEnterListener,
                        ScaleModifier(
                            Math.min(remainTime, 0.18f * GameHelper.getTimeMultiplier()),
                            initialScale, scale, EaseQuadOut.getInstance()
                        ),
                        AlphaModifier(
                            Math.min(remainTime, 0.06f * GameHelper.getTimeMultiplier()),
                            followCircle!!.getAlpha(), 1f
                        )
                    )
                )
            } else if (!inRadius && mWasInRadius) {
                mWasInRadius = false
                mIsAnimating = true

                followCircle!!.clearEntityModifiers()
                followCircle!!.registerEntityModifier(
                    ParallelEntityModifier(mFollowLeaveListener,
                        ScaleModifier(0.1f * GameHelper.getTimeMultiplier(), followCircle!!.getScaleX(), scale * 2f),
                        AlphaModifier(0.1f * GameHelper.getTimeMultiplier(), followCircle!!.getAlpha(), 0f)
                    )
                )
            }
        } else {
            mWasInRadius = inRadius
            followCircle!!.setAlpha(if (inRadius) 1f else 0f)
        }

        while (ticks.isNotEmpty() && percentage < 1 - 0.02f / maxTime &&
            tickTime * GameHelper.tickRate > tickInterval
        ) {
            tickTime -= tickInterval / GameHelper.tickRate
            if (followCircle!!.getAlpha() > 0 && replayObjectData == null ||
                replayObjectData != null && replayObjectData!!.tickSet?.get(tickIndex) == true
            ) {
                Utils.playHitSound(listener!!, 16)
                listener!!.onSliderHit(id, 10, null, ballpos, false, color, GameObjectListener.SLIDER_TICK)

                if (Config.isComplexAnimations() && !mIsAnimating) {
                    followCircle!!.clearEntityModifiers()
                    followCircle!!.registerEntityModifier(
                        ScaleModifier(
                            (Math.min(tickInterval / GameHelper.tickRate, 0.2) * GameHelper.getTimeMultiplier()).toFloat(),
                            scale * 1.1f, scale, EaseQuadOut.getInstance()
                        )
                    )
                }

                ticksGot++
                tickSet.set(tickIndex++, true)
            } else {
                listener!!.onSliderHit(id, -1, null, ballpos, false, color, GameObjectListener.SLIDER_TICK)
                tickSet.set(tickIndex++, false)
            }
            ticks[currentTick].setAlpha(0f)
            if (reverse && currentTick > 0) {
                currentTick--
            } else if (!reverse && currentTick < ticks.size - 1) {
                currentTick++
            }
            ticksTotal++
        }
        followCircle!!.setPosition(
            ballpos.x - followCircle!!.getWidth() / 2,
            ballpos.y - followCircle!!.getHeight() / 2
        )
        ball!!.setPosition(ballpos.x - ball!!.getWidth() / 2, ballpos.y - ball!!.getHeight() / 2)
        ball!!.setRotation(ballAngle)

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            listener!!.updateAutoBasedPos(ballpos.x, ballpos.y)
        }

        if (percentage >= 1) {
            over()
        }
    }

    private fun applyBodyFadeAdjustments(fadeInDuration: Float) {
        if (abstractSliderBody == null) return

        if (GameHelper.isHidden()) {
            val realFadeInDuration = fadeInDuration / GameHelper.getTimeMultiplier()
            val fadeOutDuration =
                ((maxTime * repeatCount + preTime - realFadeInDuration) * GameHelper.getTimeMultiplier()).toFloat()

            abstractSliderBody!!.applyFadeAdjustments(fadeInDuration, fadeOutDuration)
        } else {
            abstractSliderBody!!.applyFadeAdjustments(fadeInDuration)
        }
    }

    override fun tryHit(dt: Float) {
        if (!startHit) {
            if (isHit() && -passedTime < GameHelper.difficultyHelper.hitWindowFor50(GameHelper.overallDifficulty)) {
                listener!!.registerAccuracy(passedTime)
                startHit = true
                Utils.playHitSound(listener!!, soundId[0], sampleSet[0], addition[0])
                ticksGot++
                firstHitAccuracy = (passedTime * 1000).toInt()
                listener!!.onSliderHit(id, 30, null, startPosition, false, color, GameObjectListener.SLIDER_START)
            }
            if (passedTime < 0) {
                if (startHit) {
                    approachCircle.setAlpha(0f)
                }
            }
        }
    }
}
