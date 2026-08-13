package ru.nsu.ccfit.zuev.osu.online

import com.edlplan.ui.fragment.ConfirmDialogFragment
import com.edlplan.ui.fragment.WebViewFragment
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.util.Debug
import org.anddev.andengine.util.HorizontalAlign
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osuplus.R

class OnlinePanel : Entity() {
    private val bannerLayer = Entity()
    private val onlineLayer = Entity()
    private val messageLayer = Entity()
    private val frontLayer = Entity()

    @JvmField
    var rect: Rectangle? = null

    private lateinit var rankText: ChangeableText
    private lateinit var nameText: ChangeableText
    private lateinit var scoreText: ChangeableText
    private lateinit var accText: ChangeableText
    private lateinit var messageText: ChangeableText
    private lateinit var submessageText: ChangeableText
    private var avatar: Sprite? = null
    private var banner: Sprite? = null

    init {
        attachChild(bannerLayer)

        rect = object : Rectangle(0f, 0f, Utils.toRes(410f), Utils.toRes(110f)) {
            var moved = false
            var dx = 0f
            var dy = 0f

            override fun onAreaTouched(
                pSceneTouchEvent: TouchEvent,
                pTouchAreaLocalX: Float,
                pTouchAreaLocalY: Float
            ): Boolean {
                if (pSceneTouchEvent.isActionDown) {
                    this.setColor(0.3f, 0.3f, 0.3f, 0.9f)
                    moved = false
                    dx = pTouchAreaLocalX
                    dy = pTouchAreaLocalY
                    return true
                }
                if (pSceneTouchEvent.isActionUp) {
                    this.setColor(0.2f, 0.2f, 0.2f, 0.5f)
                    if (!moved) {
                        if (OnlineManager.getInstance().isStayOnline) {
                            ConfirmDialogFragment()
                                .setMessage(R.string.dialog_visit_profile_page)
                                .showForResult { isAccepted ->
                                    GlobalManager.getInstance().getMainActivity()?.runOnUiThread {
                                        WebViewFragment().setURL(
                                            WebViewFragment.PROFILE_URL + OnlineManager.getInstance().userId
                                        ).show()
                                    }
                                }
                        }
                    }
                    return true
                }
                if (pSceneTouchEvent.isActionOutside
                    || pSceneTouchEvent.isActionMove
                    && (MathUtils.distance(
                        dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY
                    ) > 50)
                ) {
                    moved = true
                    this.setColor(0.2f, 0.2f, 0.2f, 0.5f)
                }
                return false
            }
        }
        rect!!.setColor(0.2f, 0.2f, 0.2f, 0.5f)
        attachChild(rect)

        val overlay = Rectangle(0f, 0f, Utils.toRes(410f), Utils.toRes(110f))
        overlay.setColor(0f, 0f, 0f, 0.4f)
        bannerLayer.attachChild(overlay)

        val avatarFooter = Rectangle(0f, 0f, Utils.toRes(110f), Utils.toRes(110f))
        avatarFooter.setColor(0.2f, 0.2f, 0.2f, 0.8f)
        attachChild(avatarFooter)

        rankText = ChangeableText(
            0f, 0f,
            ResourceManager.getInstance().getFont("CaptionFont"), "#1",
            HorizontalAlign.RIGHT, 12
        )
        rankText.setColor(0.6f, 0.6f, 0.6f, 0.9f)
        rankText.setScaleCenterX(0f)
        rankText.setScale(1.7f)
        rankText.setPosition(
            Utils.toRes(390 + 10f) - rankText.widthScaled,
            Utils.toRes(55f)
        )
        onlineLayer.attachChild(rankText)

        nameText = ChangeableText(
            Utils.toRes(120f), Utils.toRes(5f),
            ResourceManager.getInstance().getFont("CaptionFont"), "Guest", 16
        )
        onlineLayer.attachChild(nameText)

        scoreText = ChangeableText(
            Utils.toRes(120f), Utils.toRes(50f),
            ResourceManager.getInstance().getFont("smallFont"), "Score: 0",
            HorizontalAlign.LEFT, 22
        )
        scoreText.setColor(0.85f, 0.85f, 0.9f)
        onlineLayer.attachChild(scoreText)

        accText = ChangeableText(
            Utils.toRes(120f), Utils.toRes(75f),
            ResourceManager.getInstance().getFont("smallFont"), "Accuracy: 0.00%",
            HorizontalAlign.LEFT, 17
        )
        accText.setColor(0.85f, 0.85f, 0.9f)
        onlineLayer.attachChild(accText)

        messageText = ChangeableText(
            Utils.toRes(110f), Utils.toRes(5f),
            ResourceManager.getInstance().getFont("CaptionFont"), "Logging in...", 16
        )
        messageLayer.attachChild(messageText)

        submessageText = ChangeableText(
            Utils.toRes(110f), Utils.toRes(60f),
            ResourceManager.getInstance().getFont("smallFont"), "Connecting to server...", 40
        )
        messageLayer.attachChild(submessageText)

        attachChild(messageLayer)
        attachChild(frontLayer)
    }

    fun setBanner() {
        val bannerUrl = OnlineManager.getInstance().profileBannerURL
        val textureName =
            if (OnlineScoring.getInstance().isBannerLoaded() && bannerUrl.isNotEmpty()) bannerUrl else null
        setBanner(textureName)
    }

    fun setBanner(texname: String?) {
        banner?.detachSelf()
        banner = null

        if (texname.isNullOrEmpty()) return
        val tex: TextureRegion = ResourceManager.getInstance().getBannerTextureIfLoaded(texname) ?: run {
            Debug.i("Banner not loaded yet")
            return
        }

        banner = Sprite(0f, 0f, Utils.toRes(410f), Utils.toRes(110f), tex)
        banner!!.alpha = 0.8f

        bannerLayer.attachChild(banner)
    }

    fun setMessage(message: String, submessage: String) {
        messageText.setText(message)
        submessageText.setText(submessage)

        messageLayer.detachSelf()
        onlineLayer.detachSelf()
        attachChild(messageLayer)
    }

    fun setInfo() {
        val online = OnlineManager.getInstance()

        nameText.setText(online.username)

        scoreText.setText(String.format("Performance: %,dpp", online.score))

        accText.setText(String.format("Accuracy: %.2f%%", online.accuracy * 100f))

        val rank = Math.toIntExact(online.rank)

        rankText.setScale(1f)
        rankText.setText(if (rank == 0) "#-" else String.format("#%d", rank))
        rankText.setPosition(
            Utils.toRes(400f) - rankText.width * 1.7f,
            Utils.toRes(55f)
        )
        rankText.setScaleCenterX(0f)
        rankText.setScale(1.7f)

        messageLayer.detachSelf()
        onlineLayer.detachSelf()
        attachChild(onlineLayer)
    }

    fun setAvatar() {
        val avatarUrl = OnlineManager.getInstance().avatarURL
        val textureName =
            if (OnlineScoring.getInstance().isAvatarLoaded() && avatarUrl.isNotEmpty()) avatarUrl else null
        setAvatar(textureName)
    }

    fun setAvatar(texname: String?) {
        avatar?.detachSelf()
        avatar = null
        if (texname == null) return
        val tex: TextureRegion = ResourceManager.getInstance().getAvatarTextureIfLoaded(texname) ?: return

        Debug.i("Avatar is set!")
        avatar = Sprite(0f, 0f, Utils.toRes(110f), Utils.toRes(110f), tex)
        frontLayer.attachChild(avatar)
    }
}
