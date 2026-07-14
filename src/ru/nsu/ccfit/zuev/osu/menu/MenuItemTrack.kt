package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import java.lang.ref.WeakReference
import java.math.BigDecimal
import ru.nsu.ccfit.zuev.osu.BeatmapInfo
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary

class MenuItemTrack : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("menu-button-background")) {

    private val trackTitle: ChangeableText
    private val trackLeftText: ChangeableText
    private val stars: Array<Sprite>
    private val halfStar: Sprite
    private var moved = false
    private var dx = 0f
    private var dy = 0f
    private var item: WeakReference<MenuItem>? = null
    private var track: TrackInfo? = null
    private var currentMark: String? = null
    private var mark: Sprite? = null
    private var downTime = -1f

    init {
        trackTitle = ChangeableText(Utils.toRes(32).toFloat(), Utils.toRes(22).toFloat(),
            ResourceManager.getInstance().getFont("font"), "", 200)
        trackLeftText = ChangeableText(Utils.toRes(350).toFloat(), Utils.toRes(22).toFloat(),
            ResourceManager.getInstance().getFont("font"), "", 30)
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this)
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(trackTitle, trackLeftText)
        setAlpha(0.8f)
        attachChild(trackTitle)

        stars = Array(10) { i ->
            Sprite(Utils.toRes(60 + 52 * i).toFloat(), Utils.toRes(50).toFloat(),
                ResourceManager.getInstance().getTexture("star")!!).also { attachChild(it) }
        }
        val starTex = ResourceManager.getInstance().getTexture("star")!!.deepCopy()
        halfStar = Sprite(0f, 0f, starTex)
        attachChild(halfStar)
    }

    fun setItem(it: MenuItem) {
        item = WeakReference(it)
    }

    fun setTrack(track: TrackInfo, info: BeatmapInfo) {
        this.track = track
        trackTitle.setText(track.mode + " (" + track.creator + ")")
        trackLeftText.setText("\n" + info.title)

        for (s in stars) {
            s.setVisible(false)
        }
        halfStar.setVisible(false)

        val diff = minOf(track.difficulty, 10f)
        val fInt = diff.toInt()
        val b1 = BigDecimal(diff.toString())
        val b2 = BigDecimal(fInt.toString())
        val fPoint = b1.subtract(b2).toFloat()

        for (j in 0 until fInt) {
            if (j < stars.size) {
                stars[j].setVisible(true)
            }
        }

        if (fPoint > 0 && fInt != 10) {
            halfStar.setVisible(true)
            halfStar.setPosition(Utils.toRes(60 + 52 * fInt).toFloat(), Utils.toRes(50).toFloat())
            halfStar.setScale(fPoint)
        }
        updateMark()
    }

    fun updateMark() {
        val track = track ?: return
        val newmark = ScoreLibrary.getInstance().getBestMark(track.filename!!)
        if (currentMark != null && currentMark == newmark) return
        mark?.detachSelf()
        if (newmark != null) {
            mark = Sprite(Utils.toRes(25).toFloat(), Utils.toRes(55).toFloat(), ResourceManager
                .getInstance().getTexture("ranking-$newmark-small")!!)
            attachChild(mark)
        } else {
            mark = null
        }
        currentMark = newmark
    }

    fun getTrack(): TrackInfo? = track

    fun setDeselectColor() {
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this)
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(trackTitle, trackLeftText)
    }

    fun setSelectedColor() {
        OsuSkin.get().getColor("MenuItemVersionsSelectedColor", SELECTED_COLOR).apply(this)
        OsuSkin.get().getColor("MenuItemSelectedTextColor", SELECTED_TEXT_COLOR).applyAll(trackTitle, trackLeftText)
    }

    override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
        if (pSceneTouchEvent.isActionDown) {
            downTime = 0f
            moved = false
            setSelectedColor()
            dx = pTouchAreaLocalX
            dy = pTouchAreaLocalY
            item?.get()?.stopScroll(y + pTouchAreaLocalY)
            return true
        } else if (pSceneTouchEvent.isActionUp && !moved) {
            downTime = -1f
            val menuItem = item?.get() ?: return true
            if (!menuItem.isTrackSelected(this)) {
                ResourceManager.getInstance().getSound("menuclick").play()
                menuItem.deselectTrack()
            }
            menuItem.selectTrack(this, false)
            return true
        } else if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) {
            downTime = -1f
            setDeselectColor()
            moved = true
            return false
        } else {
            return !pSceneTouchEvent.isActionUp
        }
    }

    internal fun update(dt: Float) {
        if (downTime >= 0) {
            downTime += dt
        }
        if (downTime > 0.5f) {
            setSelectedColor()
            moved = true
            item?.get()?.showPropertiesMenu()
            downTime = -1f
        }
    }

    companion object {
        private val DEFAULT_COLOR = RGBColor(25 / 255f, 25 / 255f, 240 / 255f)
        private val SELECTED_COLOR = RGBColor(1f, 1f, 1f)
        private val DEFAULT_TEXT_COLOR = RGBColor(1f, 1f, 1f)
        private val SELECTED_TEXT_COLOR = RGBColor(0f, 0f, 0f)
    }
}
