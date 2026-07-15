package ru.nsu.ccfit.zuev.osu.game

import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class ScoreBar(listener: GameObjectListener, scene: Scene, private val stat: StatisticV2) : GameObject() {

    private val bg: Sprite
    private val colour: Sprite
    private val ki: AnimSprite
    private val width: Float
    private var lasthp = 0f

    init {
        bg = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("scorebar-bg"))
        bg.setScaleCenter(0f, 0f)

        if (ResourceManager.getInstance().isTextureLoaded("scorebar-colour-0")) {
            val loadedScoreBarTextures = ArrayList<String>()
            for (i in 0 until 60) {
                if (ResourceManager.getInstance().isTextureLoaded("scorebar-colour-$i"))
                    loadedScoreBarTextures.add("scorebar-colour-$i")
            }
            colour = AnimSprite(
                Utils.toRes(5).toFloat(), Utils.toRes(16).toFloat(), loadedScoreBarTextures.size.toFloat(),
                *loadedScoreBarTextures.toTypedArray()
            )
        } else {
            colour = Sprite(
                Utils.toRes(5).toFloat(), Utils.toRes(16).toFloat(),
                ResourceManager.getInstance().getTexture("scorebar-colour")!!
            )
        }
        width = colour.getWidth()

        ki = if (ResourceManager.getInstance().isTextureLoaded("scorebar-kidanger")) {
            AnimSprite(0f, 0f, 0f, "scorebar-ki", "scorebar-kidanger", "scorebar-kidanger2")
        } else {
            AnimSprite(0f, 0f, 0f, "scorebar-ki")
        }
        ki.setPosition(
            Utils.toRes(5).toFloat() + colour.getWidth() - ki.getWidth() / 2,
            Utils.toRes(16).toFloat() + colour.getHeight() / 2 - Utils.toRes(58).toFloat()
        )

        scene.attachChild(ki, 0)
        scene.attachChild(colour, 0)
        scene.attachChild(bg, 0)
    }

    fun setVisible(visible: Boolean) {
        bg.setVisible(visible)
        colour.setVisible(visible)
        ki.setVisible(visible)
    }

    override fun update(dt: Float) {
        var hp = stat.getHp()
        if (Math.abs(hp - lasthp) > speed * dt) {
            hp = speed * dt * Math.signum(hp - lasthp) + lasthp
        }

        colour.setWidth(width * hp)

        ki.setPosition(
            5f + colour.getWidth() - ki.getWidth() / 2,
            16f + colour.getHeight() / 2 - ki.getHeight() / 2
        )
        ki.setFrame(
            when {
                hp > 0.49f -> 0
                hp > 0.24f -> 1
                else -> 2
            }
        )
        lasthp = hp
    }

    fun flush() {
        lasthp = stat.getHp()
        update(0f)
    }

    companion object {
        private var speed = 0.75f
    }
}
