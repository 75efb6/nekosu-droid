package com.edlplan.framework.support.timing

abstract class Loopable {

    var flag: Flag = Flag.Run
    var looper: ILooper<*>? = null

    open fun onRemove() {

    }

    abstract fun onLoop(deltaTime: Double)

    enum class Flag {
        Run, Skip, Stop
    }
}
