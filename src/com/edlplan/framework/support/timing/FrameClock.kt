package com.edlplan.framework.support.timing

import com.edlplan.framework.support.Framework

class FrameClock {

    private var startTime: Double = -1.0

    var frameTime: Double = 0.0
        private set

    var deltaTime: Double = 0.0
        private set

    private var running: Boolean = false

    fun offset(o: Double) {
        frameTime += o
    }

    fun start() {
        if (startTime == -1.0) {
            startTime = Framework.frameworkTime()
            running = true
        }
    }

    fun isRunninng(): Boolean {
        return running
    }

    fun toClockTime(frameworkTime: Double): Double {
        return if (running) {
            frameworkTime - startTime
        } else {
            frameTime
        }
    }

    fun update() {
        if (running) {
            val t = Framework.frameworkTime() - startTime
            deltaTime = t - frameTime
            frameTime = t
        }
    }

    fun run() {
        if (!running) {
            running = true
            val dt = Framework.frameworkTime() - frameTime
            startTime += dt
        }
    }

    fun pause() {
        if (running) {
            running = false
            frameTime = Framework.frameworkTime()
        }
    }
}
