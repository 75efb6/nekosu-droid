package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import org.anddev.andengine.entity.modifier.*
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.MathUtils
import org.anddev.andengine.util.modifier.IModifier
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class Spinner : GameObject() {
    private val background: Sprite
    val center: PointF
    private val circle: Sprite
    private val approachCircle: Sprite
    private val metre: Sprite
    private val spinText: Sprite
    private val mregion: TextureRegion
    private var clearText: Sprite? = null
    private var oldMouse: PointF? = null
    private var listener: GameObjectListener? = null
    private var scene: Scene? = null
    private var fullrotations = 0
    private var rotations = 0f
    private var needRotations = 0f
    private var clear = false
    private var soundId = 0
    private var sampleSet = 0
    private var addition = 0
    private var bonusScore: ScoreNumber? = null
    private var score = 1
    private var metreY = 0f
    private var stat: StatisticV2? = null
    private var totalTime = 0f
    private val currMouse = PointF()

    init {
        ResourceManager.getInstance().checkSpinnerTextures()
        pos = PointF(Constants.MAP_WIDTH / 2.toFloat(), Constants.MAP_HEIGHT / 2.toFloat())
        center = Utils.trackToRealCoords(pos)
        background = SpritePool.getInstance().getCenteredSprite("spinner-background", center)
        val scaleX = Config.getRES_WIDTH() / background.getWidth()
        background.setScale(scaleX)
        circle = SpritePool.getInstance().getCenteredSprite("spinner-circle", center)
        mregion = ResourceManager.getInstance().getTexture("spinner-metre")!!.deepCopy()
        metre = Sprite(
            center.x - Config.getRES_WIDTH() / 2,
            Config.getRES_HEIGHT().toFloat(),
            mregion
        )
        metre.setWidth(Config.getRES_WIDTH().toFloat())
        metre.setHeight(background.getHeightScaled())
        approachCircle = SpritePool.getInstance().getCenteredSprite("spinner-approachcircle", center)
        spinText = CentredSprite(center.x, center.y * 1.5f, ResourceManager.getInstance().getTexture("spinner-spin")!!)
    }

    fun init(
        listener: GameObjectListener, scene: Scene, pretime: Float, time: Float,
        rps: Float, sound: Int, tempSound: String?, stat: StatisticV2
    ) {
        clearText = null
        fullrotations = 0
        rotations = 0f
        this.scene = scene
        needRotations = rps * time
        if (time < 0.05f) needRotations = 0.1f
        this.listener = listener
        soundId = sound
        sampleSet = 0
        addition = 0
        this.stat = stat
        totalTime = time
        startHit = true
        clear = false
        if (totalTime <= 0f) clear = true
        bonusScore = null
        score = 1
        ResourceManager.getInstance().checkSpinnerTextures()

        if (!Utils.isEmpty(tempSound)) {
            val group = tempSound!!.split(":")
            sampleSet = group[0].toInt()
            addition = group[1].toInt()
        }

        val appearModifier: IEntityModifier = SequenceEntityModifier(
            DelayModifier(pretime * 0.75f),
            FadeInModifier(pretime * 0.25f)
        )

        background.setAlpha(0f)
        background.registerEntityModifier(appearModifier.deepCopy())

        circle.setAlpha(0f)
        circle.registerEntityModifier(appearModifier.deepCopy())

        metreY = (Config.getRES_HEIGHT() - background.getHeightScaled()) / 2
        metre.setAlpha(0f)
        metre.registerEntityModifier(appearModifier.deepCopy())
        mregion.setTexturePosition(0, metre.getHeightScaled().toInt())

        approachCircle.setAlpha(0f)
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(false)
        }
        approachCircle.registerEntityModifier(
            SequenceEntityModifier(
                object : IEntityModifierListener {
                    override fun onModifierStarted(pModifier: IModifier<IEntity>, pItem: IEntity) {}
                    override fun onModifierFinished(pModifier: IModifier<IEntity>, pItem: IEntity) {
                        com.reco1l.framework.lang.Execution.updateThread { removeFromScene() }
                    }
                },
                SequenceEntityModifier(
                    DelayModifier(pretime),
                    ParallelEntityModifier(
                        AlphaModifier(time, 0.75f, 1f),
                        ScaleModifier(time, 2.0f, 0f)
                    )
                )
            )
        )

        spinText.setAlpha(0f)
        spinText.registerEntityModifier(
            SequenceEntityModifier(
                DelayModifier(pretime * 0.75f),
                FadeInModifier(pretime * 0.25f),
                DelayModifier(pretime / 2),
                FadeOutModifier(pretime * 0.25f)
            )
        )

        scene.attachChild(spinText, 0)
        scene.attachChild(approachCircle, 0)
        scene.attachChild(circle, 0)
        scene.attachChild(metre, 0)
        scene.attachChild(background, 0)

        oldMouse = null
    }

    fun removeFromScene() {
        if (clearText != null) {
            scene!!.detachChild(clearText)
            SpritePool.getInstance().putSprite("spinner-clear", clearText!!)
        }
        scene!!.detachChild(spinText)
        scene!!.detachChild(background)
        approachCircle.detachSelf()
        scene!!.detachChild(circle)
        scene!!.detachChild(metre)

        if (bonusScore != null) {
            bonusScore!!.detachFromScene(scene!!)
        }
        listener!!.removeObject(this)
        var finalScore = 0
        if (replayObjectData != null) {
            while (fullrotations + score < replayObjectData!!.accuracy / 4 + 1) {
                fullrotations++
                listener!!.onSpinnerHit(id, 1000, false, 0)
            }
            if (fullrotations >= needRotations) clear = true
        }
        var percentfill = (Math.abs(rotations.toDouble()) + fullrotations) / needRotations
        if (needRotations <= 0.1f) {
            clear = true
            percentfill = 1.0
        }
        if (percentfill > 0.9f) {
            finalScore = 50
        }
        if (percentfill > 0.95f) {
            finalScore = 100
        }
        if (clear) {
            finalScore = 300
        }
        if (replayObjectData != null) {
            when ((replayObjectData!!.accuracy % 4).toInt()) {
                0 -> finalScore = 0
                1 -> finalScore = 50
                2 -> finalScore = 100
                3 -> finalScore = 300
            }
        }
        listener!!.onSpinnerHit(id, finalScore, endsCombo, score + fullrotations - 1)
        if (finalScore > 0) {
            Utils.playHitSound(listener!!, soundId)
        }
    }

    override fun cleanupFromScene() {
        if (scene == null) return
        if (clearText != null) {
            scene!!.detachChild(clearText)
        }
        scene!!.detachChild(spinText)
        scene!!.detachChild(background)
        approachCircle.detachSelf()
        scene!!.detachChild(circle)
        scene!!.detachChild(metre)
        if (bonusScore != null) {
            bonusScore!!.detachFromScene(scene!!)
        }
        listener!!.removeObject(this)
        scene = null
    }

    override fun update(dt: Float) {
        if (circle.getAlpha() == 0f) return
        var mouse: PointF? = null

        for (i in 0 until listener!!.getCursorsCount()) {
            if (mouse == null) {
                if (autoPlay) {
                    mouse = center
                } else if (listener!!.isMouseDown(i)) {
                    mouse = listener!!.getMousePos(i)
                } else {
                    continue
                }
                currMouse.set(mouse.x - center.x, mouse.y - center.y)
            }

            if (oldMouse == null || listener!!.isMousePressed(this, i)) {
                if (oldMouse == null) {
                    oldMouse = PointF()
                }
                oldMouse!!.set(currMouse)
                return
            }
        }

        if (mouse == null) return

        circle.setRotation(MathUtils.radToDeg(Utils.direction(currMouse)))

        val len1 = Utils.length(currMouse)
        val len2 = Utils.length(oldMouse!!)
        var dfill = (currMouse.x / len1) * (oldMouse!!.y / len2) - (currMouse.y / len1) * (oldMouse!!.x / len2)

        if (Math.abs(len1) < 0.0001f || Math.abs(len2) < 0.0001f) dfill = 0f

        if (autoPlay) {
            dfill = 5 * 4 * dt
            circle.setRotation((rotations + dfill / 4f) * 360)
            if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
                val angle = (rotations + dfill / 4f) * 360
                val pX = center.x + 50 * Math.sin(angle.toDouble()).toFloat()
                val pY = center.y + 50 * Math.cos(angle.toDouble()).toFloat()
                listener!!.updateAutoBasedPos(pX, pY)
            }
        }
        rotations += dfill / 4f
        var percentfill = (Math.abs(rotations.toDouble()) + fullrotations) / needRotations

        if (percentfill > 1 || clear) {
            percentfill = 1.0
            if (!clear) {
                clearText = SpritePool.getInstance().getCenteredSprite(
                    "spinner-clear", PointF(center.x, center.y * 0.5f)
                )
                clearText!!.registerEntityModifier(
                    ParallelEntityModifier(
                        FadeInModifier(0.25f),
                        ScaleModifier(0.25f, 1.5f, 1f)
                    )
                )
                scene!!.attachChild(clearText)
                clear = true
            } else if (Math.abs(rotations.toDouble()) > 1) {
                if (bonusScore != null) {
                    scene!!.detachChild(bonusScore!!)
                }
                rotations -= 1 * Math.signum(rotations.toDouble()).toFloat()
                bonusScore = ScoreNumber(
                    center.x, center.y + 100,
                    (score * 1000).toString(), 1.1f, true
                )
                listener!!.onSpinnerHit(id, 1000, false, 0)
                score++
                scene!!.attachChild(bonusScore!!)
                ResourceManager.getInstance().getSound("spinnerbonus")?.play()
                var rate = 0.375f
                if (GameHelper.drain > 0) {
                    rate = 1 + GameHelper.drain / 4f
                }
                stat!!.changeHp(rate * 0.01f * totalTime / needRotations)
            }
        } else if (Math.abs(rotations.toDouble()) > 1) {
            rotations -= 1 * Math.signum(rotations.toDouble()).toFloat()
            if (replayObjectData == null || replayObjectData!!.accuracy / 4 > fullrotations) {
                fullrotations++
                stat!!.registerSpinnerHit()
                var rate = 0.375f
                if (GameHelper.drain > 0) {
                    rate = 1 + GameHelper.drain / 2f
                }
                stat!!.changeHp(rate * 0.01f * totalTime / needRotations)
            }
        }
        metre.setPosition(
            metre.getX(),
            metreY + metre.getHeight() * (1 - Math.abs(percentfill)).toFloat()
        )
        mregion.setTexturePosition(
            0,
            (metre.getBaseHeight() * (1 - Math.abs(percentfill))).toInt()
        )

        oldMouse!!.set(currMouse)
    }

    fun isCompleted(): Boolean = clear

    fun isActive(): Boolean = circle.getAlpha() != 0f

    fun isStopped(): Boolean = true

    fun canBeActive(): Boolean = false

    fun getSpinnerPos(): PointF = center

    fun getRadius(): Float = 0f
}
