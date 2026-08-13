package com.rian.difficultycalculator.beatmap.timings

class TimingControlPoint : ControlPoint {
    @JvmField
    val msPerBeat: Double

    @JvmField
    val timeSignature: Int

    constructor(time: Double, msPerBeat: Double, timeSignature: Int) : super(time) {
        this.msPerBeat = msPerBeat
        this.timeSignature = timeSignature
    }

    private constructor(source: TimingControlPoint) : this(source.time, source.msPerBeat, source.timeSignature)

    fun getBPM(): Double {
        return 60000 / msPerBeat
    }

    override fun isRedundant(existing: ControlPoint): Boolean {
        return false
    }

    override fun deepClone(): TimingControlPoint {
        return TimingControlPoint(this)
    }
}
