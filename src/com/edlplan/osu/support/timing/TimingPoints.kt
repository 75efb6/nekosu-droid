package com.edlplan.osu.support.timing

import com.edlplan.framework.utils.U
import com.edlplan.osu.support.SampleSet

class TimingPoints {
    private val timings = ArrayList<TimingPoint>()

    fun addTimingPoint(t: TimingPoint) {
        timings.add(t)
    }

    fun getTimingPointList(): ArrayList<TimingPoint> = timings

    override fun toString(): String {
        val sb = StringBuilder()
        for (t in timings) {
            sb.append(t.toString()).append(U.NEXT_LINE)
        }
        return sb.toString()
    }

    companion object {
        @JvmStatic
        fun parse(strings: List<String>): TimingPoints {
            val timingPoints = TimingPoints()
            for (ll in strings) {
                val l = ll.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val t = TimingPoint()
                t.time = l[0].trim { it <= ' ' }.toDouble()
                t.beatLength = l[1].trim { it <= ' ' }.toDouble()
                if (t.beatLength.isNaN()) continue
                t.meter = if (l.size > 2) l[2].toInt() else 4
                t.sampleType = if (l.size > 3) l[3].toInt() else 1
                t.sampleSet = if (l.size > 4) SampleSet.parse(l[4]) ?: SampleSet.None else SampleSet.None
                t.volume = if (l.size > 5) l[5].toInt() else 100
                t.inherited = t.beatLength < 0
                val eff = if (l.size > 7) l[7].toInt() else 0
                t.kiaiMode = eff and 1 > 0
                t.omitFirstBarSignature = eff and 8 > 0
                timingPoints.addTimingPoint(t)
            }
            return timingPoints
        }
    }
}
