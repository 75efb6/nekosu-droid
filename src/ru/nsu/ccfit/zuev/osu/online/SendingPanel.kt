package ru.nsu.ccfit.zuev.osu.online

import org.anddev.andengine.entity.modifier.MoveYModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene.ITouchArea
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.util.HorizontalAlign
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils

class SendingPanel(rank: Long, score: Long, accuracy: Float) : Rectangle(
    0f, Utils.toRes(-300f), Utils.toRes(800f), Utils.toRes(300f)
) {
    private lateinit var mapText: ChangeableText
    private lateinit var rankText: ChangeableText
    private lateinit var accText: ChangeableText
    private lateinit var scoreText: ChangeableText
    private lateinit var buttonText: ChangeableText
    private lateinit var mapRect: Rectangle
    private lateinit var rankRect: Rectangle
    private lateinit var accRect: Rectangle
    private lateinit var scoreRect: Rectangle
    private var button: Sprite

    private var oldRank: Long = rank
    private var oldScore: Long = score
    private var oldAccuracy: Float = accuracy
    private var canBeDismissed = false

    init {
        val btnTex = ResourceManager.getInstance().getTexture("ranking_button")

        setColor(0f, 0f, 0f, 0.7f)

        button = object : Sprite(Utils.toRes(272f), Utils.toRes(300f), btnTex!!) {
            override fun onAreaTouched(
                pSceneTouchEvent: TouchEvent,
                pTouchAreaLocalX: Float,
                pTouchAreaLocalY: Float
            ): Boolean {
                if (canBeDismissed) {
                    this@SendingPanel.registerEntityModifier(
                        MoveYModifier(0.5f, 0f, Utils.toRes(-350f))
                    )
                    canBeDismissed = false
                    return true
                }
                return false
            }
        }
        attachChild(button)

        buttonText = ChangeableText(
            Utils.toRes(340f), Utils.toRes(305f),
            ResourceManager.getInstance().getFont("font"),
            "Sending...", HorizontalAlign.CENTER, 10
        )
        attachChild(buttonText)

        val topScoreText = Text(
            0f, 0f,
            ResourceManager.getInstance().getFont("CaptionFonrt"), "Overall Ranking"
        )
        topScoreText.setPosition(Utils.toRes(400f) - topScoreText.width / 2, Utils.toRes(60f))
        attachChild(topScoreText)

        val tableCaption = Text(
            Utils.toRes(60f), Utils.toRes(120f),
            ResourceManager.getInstance().getFont("font"),
            String.format(
                "%-12s %-12s %-14s %-14s",
                "Map rank",
                "Overall",
                "Accuracy",
                "Performance"
            )
        )
        attachChild(tableCaption)

        mapRect = Rectangle(Utils.toRes(50f), Utils.toRes(160f), Utils.toRes(140f), Utils.toRes(80f))
        mapRect.setColor(1f, 1f, 0f, 0.8f)
        attachChild(mapRect)

        rankRect = Rectangle(Utils.toRes(195f), Utils.toRes(160f), Utils.toRes(150f), Utils.toRes(80f))
        attachChild(rankRect)

        accRect = Rectangle(Utils.toRes(350f), Utils.toRes(160f), Utils.toRes(150f), Utils.toRes(80f))
        attachChild(accRect)

        scoreRect = Rectangle(Utils.toRes(505f), Utils.toRes(160f), Utils.toRes(250f), Utils.toRes(80f))
        attachChild(scoreRect)

        val font: Font = ResourceManager.getInstance().getFont("font")
        mapText = ChangeableText(0f, 0f, font, "#9999999", HorizontalAlign.CENTER, 8)
        placeText(mapRect, mapText)
        attachChild(mapText)

        rankText = ChangeableText(0f, 0f, font, "#9999999\n(+100)", HorizontalAlign.CENTER, 19)
        placeText(rankRect, rankText)
        attachChild(rankText)

        accText = ChangeableText(0f, 0f, font, "100.00%\n(+21.90%)", HorizontalAlign.CENTER, 16)
        placeText(accRect, accText)
        attachChild(accText)

        scoreText = ChangeableText(
            0f, 0f, font, "99 123 456 789pp\n(+99 999 999pp)",
            HorizontalAlign.CENTER, 100
        )
        placeText(scoreRect, scoreText)
        attachChild(scoreText)
    }

    private fun placeText(rect: Rectangle, text: ChangeableText) {
        text.setPosition(
            rect.x + rect.width / 2 - text.width / 2,
            rect.y + rect.height / 2 - text.height / 2
        )
    }

    private fun setRectColor(rect: Rectangle, difference: Float) {
        if (difference > 0)
            rect.setColor(0f, 1f, 0f, 0.5f)
        else if (difference < 0)
            rect.setColor(1f, 0f, 0f, 0.5f)
        else
            rect.setColor(0.4f, 0.4f, 0.4f, 0.8f)
    }

    private fun formatScore(score: Long): String {
        val scoreBuilder = StringBuilder()
        scoreBuilder.append(Math.abs(score))
        var i = scoreBuilder.length - 3
        while (i > 0) {
            scoreBuilder.insert(i, ' ')
            i -= 3
        }
        if (score < 0) {
            scoreBuilder.insert(0, '-')
        }
        return scoreBuilder.toString()
    }

    fun show(mapRank: Long, newScore: Long, newRank: Long, newAcc: Float) {
        canBeDismissed = true
        mapText.setText(String.format("#%d", mapRank))
        placeText(mapRect, mapText)
        if (newScore > oldScore)
            mapRect.setColor(1f, 1f, 0f, 0.8f)
        else
            setRectColor(mapRect, 0f)

        if (newRank == oldRank)
            rankText.setText(String.format("#%d", oldRank))
        else if (newRank < oldRank)
            rankText.setText(String.format("#%d\n(+%d)", newRank, oldRank - newRank))
        else
            rankText.setText(String.format("#%d\n(%d)", newRank, oldRank - newRank))
        placeText(rankRect, rankText)
        setRectColor(rankRect, (oldRank - newRank).toFloat())

        if (Math.abs(newAcc - oldAccuracy) < 0.0001f)
            accText.setText(String.format("%.2f%%", oldAccuracy * 100f))
        else if (newAcc < oldAccuracy)
            accText.setText(
                String.format(
                    "%.2f%%\n(%.2f%%)",
                    newAcc * 100f,
                    (newAcc - oldAccuracy) * 100f
                )
            )
        else
            accText.setText(
                String.format(
                    "%.2f%%\n(+%.2f%%)",
                    newAcc * 100f,
                    (newAcc - oldAccuracy) * 100f
                )
            )
        placeText(accRect, accText)
        setRectColor(accRect, newAcc - oldAccuracy)

        if (newScore == oldScore)
            scoreText.setText(String.format("%spp", formatScore(oldScore)))
        else if (newScore < oldScore)
            scoreText.setText(
                String.format(
                    "%spp\n(%spp)",
                    formatScore(newScore),
                    formatScore(newScore - oldScore)
                )
            )
        else
            scoreText.setText(
                String.format(
                    "%spp\n(+%spp)",
                    formatScore(newScore),
                    formatScore(newScore - oldScore)
                )
            )
        placeText(scoreRect, scoreText)
        setRectColor(scoreRect, (newScore - oldScore).toFloat())

        buttonText.setText(" Dismiss")

        registerEntityModifier(MoveYModifier(0.5f, Utils.toRes(-300f), 0f))
    }

    fun setFail() {
        buttonText.setText(" Failed")
    }

    fun getDismissTouchArea(): ITouchArea = button
}
