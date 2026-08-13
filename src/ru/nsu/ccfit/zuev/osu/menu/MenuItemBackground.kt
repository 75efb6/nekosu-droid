package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.Utils

class MenuItemBackground : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("menu-button-background")) {

    private val title: ChangeableText
    private val author: ChangeableText
    private val defColor: RGBColor = OsuSkin.get().getColor("MenuItemDefaultColor", DEFAULT_COLOR)
    private val onTouchColor: RGBColor = OsuSkin.get().getColor("MenuItemOnTouchColor", ON_TOUCH_COLOR)
    private var moved = false
    private var dx = 0f
    private var dy = 0f
    private var item: MenuItem? = null

    init {
        setAlpha(0.8f)
        title = ChangeableText(Utils.toRes(32).toFloat(), Utils.toRes(25).toFloat(),
            ResourceManager.getInstance().getFont("font"), "", 255)
        author = ChangeableText(0f, 0f, ResourceManager.getInstance()
            .getFont("font"), "", 100)
        author.setPosition(Utils.toRes(150).toFloat(), Utils.toRes(60).toFloat())
        defColor.apply(this)
        attachChild(title)
        attachChild(author)
    }

    override fun reset() {
        defColor.apply(this)
    }

    fun setItem(it: MenuItem) {
        item = it
    }

    fun setTitle(newTitle: String) {
        title.setText(newTitle)
    }

    fun setAuthor(newAuthor: String) {
        author.setText(newAuthor)
    }

    override fun onAreaTouched(pSceneTouchEvent: TouchEvent, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
        if (!isVisible) return false
        if (pSceneTouchEvent.isActionDown) {
            moved = false
            onTouchColor.apply(this)
            item?.stopScroll(y + pTouchAreaLocalY)
            dx = pTouchAreaLocalX
            dy = pTouchAreaLocalY
            return true
        } else if (pSceneTouchEvent.isActionUp && !moved) {
            ResourceManager.getInstance().getSound("menuclick").play()
            defColor.apply(this)
            item?.select(true, true)
            return true
        } else if (pSceneTouchEvent.isActionOutside || pSceneTouchEvent.isActionMove && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 50) {
            defColor.apply(this)
            moved = true
            return false
        }
        return false
    }

    companion object {
        private val DEFAULT_COLOR = RGBColor(240 / 255f, 150 / 255f, 0 / 255f)
        private val ON_TOUCH_COLOR = RGBColor(1f, 1f, 1f)
    }
}
