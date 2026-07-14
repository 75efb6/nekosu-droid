package com.edlplan.osu.support.timing.controlpoint

class TimingControlPoint : ControlPoint() {
    var meter: Int = 0

    var beatLength: Double = 0.0
        set(value) {
            field = value
        }
}
