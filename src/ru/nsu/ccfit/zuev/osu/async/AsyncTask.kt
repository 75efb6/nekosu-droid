package ru.nsu.ccfit.zuev.osu.async

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

abstract class AsyncTask {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var isCompleted = false

    private val mOnComplete = Runnable {
        onComplete()
        isCompleted = true
    }

    open fun run() {}
    open fun onComplete() {}
    open fun onCancel(wasForced: Boolean) {}

    fun execute() {
        executor.execute {
            isCompleted = false
            val t = Thread.currentThread()
            t.name = "async::${t.name}"
            run()
            handler.post(mOnComplete)
            executor.shutdown()
        }
    }

    fun cancel(force: Boolean) {
        if (force) {
            executor.shutdownNow()
        } else {
            executor.shutdown()
        }
        handler.removeCallbacks(mOnComplete)
        onCancel(force)
    }

    fun isCompleted(): Boolean = isCompleted

    fun isTerminated(): Boolean = executor.isTerminated

    fun isShutdown(): Boolean = executor.isShutdown
}
