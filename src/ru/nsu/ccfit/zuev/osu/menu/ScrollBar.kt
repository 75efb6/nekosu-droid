package ru.nsu.ccfit.zuev.osu.menu

import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Utils

class ScrollBar(scene: Scene) {
    private val barRectangle: Rectangle
    private var visible = false

    init {
        visible = false
        barRectangle = Rectangle(
            (Config.getRES_WIDTH() - Utils.toRes(20)).toFloat(), 0f, Utils.toRes(20).toFloat(), Utils.toRes(50).toFloat()
        )
        barRectangle.setAlpha(0.8f)
        barRectangle.setColor(1f, 1f, 1f)
        scene.attachChild(barRectangle)
        barRectangle.setVisible(false)
    }

    fun setPosition(vy: Float, maxy: Float) {
        if (!visible) return
        barRectangle.setPosition(
            barRectangle.x,
            (Config.getRES_HEIGHT() - barRectangle.height) * vy / maxy
        )
    }

    fun setVisible(vis: Boolean) {
        barRectangle.setVisible(vis)
        if (vis && !visible) {
            val parent = barRectangle.parent
            parent.detachChild(barRectangle)
            parent.attachChild(barRectangle)
        }
        visible = vis
    }
}
