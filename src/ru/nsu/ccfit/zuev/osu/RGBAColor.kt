package ru.nsu.ccfit.zuev.osu

class RGBAColor {
    private var cr: Float = 0f
    private var cg: Float = 0f
    private var cb: Float = 0f
    private var ca: Float = 1f

    constructor() {
        cr = 0f
        cg = 0f
        cb = 0f
        ca = 1f
    }

    constructor(r: Float, g: Float, b: Float, a: Float) {
        cr = r
        cg = g
        cb = b
        ca = a
    }

    fun r(): Float = cr

    fun g(): Float = cg

    fun b(): Float = cb

    fun a(): Float = ca

    fun set(r: Float, g: Float, b: Float, a: Float) {
        cr = r
        cg = g
        cb = b
        ca = a
    }
}
