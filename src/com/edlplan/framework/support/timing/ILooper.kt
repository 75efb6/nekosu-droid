package com.edlplan.framework.support.timing

interface ILooper<T : Loopable> {
    fun loop(deltaTime: Double)
    fun prepare()
    fun addLoopable(l: T)
}
