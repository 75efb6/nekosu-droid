package ru.nsu.ccfit.zuev.osu.game

import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.RGBColor

class Combo(private val num: Int) {

    private var color: RGBColor = RGBColor(1f, 1f, 1f)

    fun getNum(): Int = num

    fun getColor(): RGBColor = color

    fun setColor(color: RGBColor) {
        this.color = color
    }

    fun getBlazingColor(): RGBColor = color

    companion object {
        @JvmStatic
        fun hitColor(comboNum: Int): RGBColor {
            val colors = Config.getComboColors()
            if (colors.isEmpty()) return RGBColor(1f, 1f, 1f)
            return colors[comboNum % colors.size] ?: RGBColor(1f, 1f, 1f)
        }
    }
}
