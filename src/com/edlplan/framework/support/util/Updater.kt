package com.edlplan.framework.support.util

abstract class Updater {

    private val lock = Any()

    private var updateId: Int = 0

    private var runningEvent: Event? = null

    fun update() {
        synchronized(lock) {
            updateId++
            if (runningEvent == null) {
                val event = Event()
                event.updateId = updateId
                event.runnable = createEventRunnable()
                runningEvent = event
                postEvent(event)
            }
        }
    }

    abstract fun createEventRunnable(): Runnable

    abstract fun postEvent(r: Runnable)

    inner class Event : Runnable {
        var updateId: Int = 0
        lateinit var runnable: Runnable

        override fun run() {
            runnable.run()
            synchronized(lock) {
                if (this.updateId > this@Updater.updateId) {
                    val event = Event()
                    event.updateId = this@Updater.updateId
                    event.runnable = createEventRunnable()
                    runningEvent = event
                    postEvent(event)
                } else {
                    runningEvent = null
                }
            }
        }
    }

}
