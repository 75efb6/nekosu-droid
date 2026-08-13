package com.edlplan.osu.support.timing.controlpoint

import com.edlplan.framework.math.FMath

class DifficultyControlPoint : ControlPoint() {
    var speedMultiplier: Double = 0.0
        set(value) {
            field = FMath.clamp(value, 0.1, 10.0)
        }
}
