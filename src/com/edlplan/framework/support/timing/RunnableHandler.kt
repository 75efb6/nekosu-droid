package com.edlplan.framework.support.timing

import com.edlplan.framework.utils.SafeList

class RunnableHandler : Loopable(), IRunnableHandler {

    private val bufferedRunnables: SafeList<DelayedRunnable> = SafeList()

    override fun post(r: Runnable, delayMS: Double) {
        bufferedRunnables.add(DelayedRunnable(r, delayMS))
    }

    override fun post(r: Runnable) {
        post(r, 0.0)
    }

    fun stop() {
        flag = Loopable.Flag.Stop
    }

    fun block() {
        flag = Loopable.Flag.Skip
    }

    override fun onRemove() {
        bufferedRunnables.clear()
        flag = Loopable.Flag.Stop
    }

    override fun onLoop(deltaTime: Double) {
        bufferedRunnables.startIterate()
        val iter: MutableIterator<DelayedRunnable> = bufferedRunnables.iterator()
        var tmp: DelayedRunnable
        while (iter.hasNext()) {
            tmp = iter.next()
            tmp.delay -= deltaTime
            if (tmp.delay <= 0) {
                tmp.r.run()
                iter.remove()
            }
        }
        bufferedRunnables.endIterate()
    }

    private inner class DelayedRunnable(var r: Runnable, var delay: Double)

}
