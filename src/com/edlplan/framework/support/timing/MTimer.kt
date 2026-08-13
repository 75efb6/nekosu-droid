package com.edlplan.framework.support.timing

import com.edlplan.framework.support.Framework

class MTimer {
    var hasInitial: Boolean = false

    var startTime: Double = 0.0

    var nowTime: Double = 0.0

    var deltaTime: Double = 0.0

    var runnedTime: Double = 0.0

    constructor() {
        hasInitial = false
    }

    fun initial() {
        initial(Framework.relativePreciseTimeMillion())
    }

    fun initial(s: Double) {
        hasInitial = true
        startTime = s
        nowTime = s
        deltaTime = 0.0
        runnedTime = 0.0
    }

    fun nowTime(): Double {
        return nowTime
    }

    fun refresh(_deltaTime: Double) {
        deltaTime = _deltaTime
        nowTime += _deltaTime
        runnedTime += _deltaTime
    }

    fun refresh() {
        refresh(Framework.relativePreciseTimeMillion() - nowTime)
    }
}
