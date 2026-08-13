package com.edlplan.osu.support.timing

import com.edlplan.framework.utils.U
import com.edlplan.osu.support.SampleSet
import com.edlplan.osu.support.timing.controlpoint.ControlPoint

class TimingPoint : ControlPoint() {
    var meter: Int = 0

    var sampleType: Int = 0

    var sampleSet: SampleSet = SampleSet.None

    var volume: Int = 100

    var inherited: Boolean = true

    var kiaiMode: Boolean = false

    var omitFirstBarSignature: Boolean = false

    var beatLength: Double = 0.0
        set(value) {
            field = value
            speedMultiplier = if (value < 0) (100.0 / -value) else 1.0
        }

    var speedMultiplier: Double = 0.0
        private set

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(time).append(",")
        sb.append(beatLength).append(",")
        sb.append(meter).append(",")
        sb.append(sampleType).append(",")
        sb.append(sampleSet).append(",")
        sb.append(U.toVString(inherited)).append(",")
        sb.append((if (kiaiMode) 1 else 0) + (if (omitFirstBarSignature) 8 else 0))
        return sb.toString()
    }
}
