/*
 * @author Reco1l
 */

@file:OptIn(DelicateCoroutinesApi::class)

package com.reco1l.framework.lang

import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * Run a task on asynchronous using Kotlin Coroutines API.
 */
fun async(block: Runnable) = GlobalScope.launch {
    block.run()
}

/**
 * Run a task ignoring exceptions on asynchronous using Kotlin Coroutines API.
 */
fun asyncIgnoreExceptions(block: Runnable) = GlobalScope.launch {
    try { block.run() } catch (e: Exception) { e.printStackTrace() }
}

/**
 * Run a delayed task on asynchronous using Kotlin Coroutines API.
 */
fun delayed(time: Long, block: Runnable) = GlobalScope.launch {
    delay(time)
    block.run()
}


// Exclusive osu!droid

fun mainThread(block: Runnable) = GlobalManager.getInstance().getMainActivity()?.runOnUiThread(block)

fun updateThread(block: Runnable) = GlobalManager.getInstance().engine?.runOnUpdateThread(block)

object Execution {
    @JvmStatic
    fun async(block: Runnable) = com.reco1l.framework.lang.async(block)

    @JvmStatic
    fun asyncIgnoreExceptions(block: Runnable) = com.reco1l.framework.lang.asyncIgnoreExceptions(block)

    @JvmStatic
    fun delayed(time: Long, block: Runnable) = com.reco1l.framework.lang.delayed(time, block)

    @JvmStatic
    fun mainThread(block: Runnable) = GlobalManager.getInstance().getMainActivity()?.runOnUiThread(block)

    @JvmStatic
    fun updateThread(block: Runnable) = GlobalManager.getInstance().engine?.runOnUpdateThread(block)
}

