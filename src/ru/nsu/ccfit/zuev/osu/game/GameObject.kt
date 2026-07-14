package ru.nsu.ccfit.zuev.osu.game

import android.graphics.PointF
import ru.nsu.ccfit.zuev.osu.scoring.Replay

abstract class GameObject {

    @JvmField
    protected var endsCombo = false

    @JvmField
    protected var autoPlay = false

    @JvmField
    internal var hitTime = 0f

    @JvmField
    protected var id = -1

    @JvmField
    protected var replayObjectData: Replay.ReplayObjectData? = null

    @JvmField
    protected var startHit = false

    @JvmField
    protected var pos = PointF()

    fun getReplayData(): Replay.ReplayObjectData? = replayObjectData

    fun setReplayData(replayObjectData: Replay.ReplayObjectData?) {
        this.replayObjectData = replayObjectData
    }

    fun setEndsCombo(endsCombo: Boolean) {
        this.endsCombo = endsCombo
    }

    fun setAutoPlay() {
        autoPlay = true
    }

    abstract fun update(dt: Float)

    fun getHitTime(): Float = hitTime

    fun setHitTime(hitTime: Float) {
        this.hitTime = hitTime
    }

    fun getId(): Int = id

    fun setId(id: Int) {
        this.id = id
    }

    fun isStartHit(): Boolean = startHit

    fun tryHit(dt: Float) {}

    fun getPos(): PointF = pos

    fun cleanupFromScene() {}
}
