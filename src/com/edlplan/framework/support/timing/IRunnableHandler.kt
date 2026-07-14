package com.edlplan.framework.support.timing

interface IRunnableHandler {
    fun post(r: Runnable)
    fun post(r: Runnable, delayMS: Double)
}
