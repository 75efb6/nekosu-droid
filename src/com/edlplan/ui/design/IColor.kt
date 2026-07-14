package com.edlplan.ui.design

import com.edlplan.framework.math.Color4

interface IColor {
    fun getColor(): Color4

    fun isContextNeeded(): Boolean = false
}
