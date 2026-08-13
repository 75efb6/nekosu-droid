package ru.nsu.ccfit.zuev.osu.game.cursor.main

import android.graphics.PointF

class Cursor {
    @JvmField
    var mousePos = PointF(0f, 0f)

    @JvmField
    var trackPos = PointF(0f, 0f)

    @JvmField
    var mouseDown = false

    @JvmField
    var mouseOldDown = false

    @JvmField
    var mousePressed = false

    @JvmField
    var mouseDownOffsetMS = 0.0
}
