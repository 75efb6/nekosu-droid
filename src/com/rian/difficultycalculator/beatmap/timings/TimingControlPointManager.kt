package com.rian.difficultycalculator.beatmap.timings

class TimingControlPointManager : ControlPointManager<TimingControlPoint> {
    constructor() : super(TimingControlPoint(0.0, 1000.0, 4))

    private constructor(source: TimingControlPointManager) : super(source.defaultControlPoint) {
        for (point in source.controlPoints) {
            controlPoints.add(point.deepClone())
        }
    }

    override fun controlPointAt(time: Double): TimingControlPoint {
        return binarySearchWithFallback(time, if (controlPoints.size > 0) controlPoints[0] else defaultControlPoint)
    }

    override fun deepClone(): TimingControlPointManager {
        return TimingControlPointManager(this)
    }
}
