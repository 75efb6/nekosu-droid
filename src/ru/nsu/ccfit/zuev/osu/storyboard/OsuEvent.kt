package ru.nsu.ccfit.zuev.osu.storyboard

import java.util.ArrayList

class OsuEvent {
    @JvmField
    var command: Command? = null
    @JvmField
    var ease: Int = 0
    @JvmField
    var startTime: Long = 0
    @JvmField
    var endTime: Long = 0
    @JvmField
    var params: FloatArray? = null
    @JvmField
    var subEvents: ArrayList<OsuEvent>? = null //for command L and T
    @JvmField
    var triggerType: String? = null //for command T
    @JvmField
    var loopCount: Int = 0
    @JvmField
    var P: String? = null
}
