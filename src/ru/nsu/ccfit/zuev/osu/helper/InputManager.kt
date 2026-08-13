package ru.nsu.ccfit.zuev.osu.helper

import android.content.Context
import android.view.inputmethod.InputMethodManager

class InputManager private constructor() {
    private var inputStarted = false
    private var changed = true
    private var builder: StringBuilder? = null
    private var maxlength = 0

    fun startInput(start: String, maxlength: Int) {
        this.maxlength = maxlength
        builder = StringBuilder(start)
        changed = true
        inputStarted = true
        toggleKeyboard()
    }

    fun toggleKeyboard() {
        val mgr = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mgr.toggleSoftInput(0, 0)
    }

    fun append(c: Char) {
        if (!inputStarted) return
        if (builder!!.length >= maxlength) return
        changed = true
        builder!!.append(c)
    }

    fun pop() {
        if (!inputStarted || builder!!.isEmpty()) return
        changed = true
        builder!!.deleteCharAt(builder!!.length - 1)
    }

    fun getText(): String {
        if (!inputStarted) return ""
        changed = false
        return builder.toString()
    }

    fun setText(text: String) {
        builder = StringBuilder(text)
        changed = true
    }

    fun isChanged(): Boolean = changed
    fun isStarted(): Boolean = inputStarted

    fun finish(): String {
        inputStarted = false
        val str = builder.toString()
        builder = null
        return str
    }

    companion object {
        private var context: Context? = null
        private val instance = InputManager()

        @JvmStatic
        fun setContext(context: Context) {
            InputManager.context = context.applicationContext
        }

        @JvmStatic
        fun getInstance(): InputManager = instance
    }
}
