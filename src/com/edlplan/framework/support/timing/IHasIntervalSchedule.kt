package com.edlplan.framework.support.timing

import com.edlplan.framework.timing.IntervalSchedule
import com.edlplan.framework.timing.TimeUpdateable
import com.edlplan.framework.utils.annotation.NotThreadSafe

interface IHasIntervalSchedule {
    fun getIntervalSchedule(): IntervalSchedule

    @NotThreadSafe
    fun addIntervalTask(start: Double, end: Double, updateable: TimeUpdateable) {
        getIntervalSchedule().addTask(start, end, false, updateable)
    }

    @NotThreadSafe
    fun addAnimTask(start: Double, duration: Double, anim: TimeUpdateable) {
        getIntervalSchedule().addAnimTask(start, duration, anim)
    }

    @NotThreadSafe
    fun addTask(time: Double, runnable: Runnable) {
        getIntervalSchedule().addTask(time, time, true, TimeUpdateable { runnable.run() })
    }
}
