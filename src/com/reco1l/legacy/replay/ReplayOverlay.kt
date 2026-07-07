package com.reco1l.legacy.replay

object ReplayOverlay {
    const val MIN_SPEED = 0.25f
    const val MAX_SPEED = 3.0f
    const val SPEED_FINE_STEP = 0.01f
    const val SPEED_COARSE_STEP = 0.05f
    const val SEEK_STEP_MS = 5000

    var isVisible = false
        private set

    var seeking = false

    var currentSpeed = 1.0f
        private set

    var currentSeekPositionMs = 0
        private set

    var totalLengthMs = Integer.MAX_VALUE
        private set

    var listener: Listener? = null

    @JvmStatic
    fun show() {
        isVisible = true
        listener?.onShow()
    }

    @JvmStatic
    fun hide() {
        isVisible = false
        listener?.onHide()
    }

    @JvmStatic
    fun toggle() {
        if (isVisible) hide() else show()
    }

    @JvmStatic
    fun updatePosition(positionMs: Int) {
        currentSeekPositionMs = positionMs
        listener?.onPositionUpdate(positionMs)
    }

    @JvmStatic
    fun updateTotalLength(lengthMs: Int) {
        totalLengthMs = lengthMs
    }

    @JvmStatic
    fun updateSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        listener?.onSpeedUpdate(currentSpeed)
    }

    @JvmStatic
    fun updateVisibility(visible: Boolean) {
        isVisible = visible
    }

    interface Listener {
        fun onShow() {}
        fun onHide() {}
        fun onPositionUpdate(positionMs: Int) {}
        fun onSpeedUpdate(speed: Float) {}
    }
}
