package com.reco1l.legacy.ui

import android.animation.Animator
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osuplus.R

class StyledKeybindDialog : BaseFragment() {

    override val layoutID = R.layout.dialog_keybind

    private var dialogTitle = ""
    private var slotIndex = 0
    private var onConfirm: OnKeyConfirmed? = null
    private var onCancel: Runnable? = null

    private var capturedKeyCode = -1
    private var statusText: TextView? = null

    init {
        isDismissOnBackgroundClick = false
        isDismissOnBackPress = true
    }

    override fun onLoadView() {
        findViewById<TextView>(R.id.keybind_title)!!.text = dialogTitle
        statusText = findViewById(R.id.keybind_status)

        findViewById<View>(R.id.keybind_confirm)!!.setOnClickListener {
            if (capturedKeyCode >= 0) {
                dismiss()
                onConfirm?.onKeyConfirmed(capturedKeyCode)
            }
        }

        playOnLoadAnim()
    }

    fun onKeyPress(keyCode: Int, keyName: String): Boolean {
        if (!isCreated) return false
        capturedKeyCode = keyCode
        statusText?.text = keyName
        statusText?.setTextColor(resources.getColor(R.color.textPrimary))
        return true
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.body)!!
        body.alpha = 0f
        body.translationY = -200f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    override fun dismiss() {
        val body = findViewById<View>(R.id.body) ?: return super.dismiss()
        body.animate().cancel()
        body.animate()
            .translationYBy(-200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) = super@StyledKeybindDialog.dismiss()
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    override fun callDismissOnBackPress() {
        onCancel?.run()
        dismiss()
    }

    fun interface OnKeyConfirmed {
        fun onKeyConfirmed(keyCode: Int)
    }

    companion object {

        private val keyNames = mapOf(
            KeyEvent.KEYCODE_Z to "Z",
            KeyEvent.KEYCODE_X to "X",
            KeyEvent.KEYCODE_C to "C",
            KeyEvent.KEYCODE_V to "V",
            KeyEvent.KEYCODE_B to "B",
            KeyEvent.KEYCODE_N to "N",
            KeyEvent.KEYCODE_M to "M",
            KeyEvent.KEYCODE_A to "A",
            KeyEvent.KEYCODE_S to "S",
            KeyEvent.KEYCODE_D to "D",
            KeyEvent.KEYCODE_F to "F",
            KeyEvent.KEYCODE_G to "G",
            KeyEvent.KEYCODE_H to "H",
            KeyEvent.KEYCODE_J to "J",
            KeyEvent.KEYCODE_K to "K",
            KeyEvent.KEYCODE_L to "L",
            KeyEvent.KEYCODE_Q to "Q",
            KeyEvent.KEYCODE_W to "W",
            KeyEvent.KEYCODE_E to "E",
            KeyEvent.KEYCODE_R to "R",
            KeyEvent.KEYCODE_T to "T",
            KeyEvent.KEYCODE_Y to "Y",
            KeyEvent.KEYCODE_P to "P",
            KeyEvent.KEYCODE_SPACE to "Space",
            KeyEvent.KEYCODE_DEL to "Backspace",
            KeyEvent.KEYCODE_ENTER to "Enter",
            KeyEvent.KEYCODE_TAB to "Tab",
            KeyEvent.KEYCODE_SHIFT_LEFT to "L-Shift",
            KeyEvent.KEYCODE_SHIFT_RIGHT to "R-Shift",
            KeyEvent.KEYCODE_CTRL_LEFT to "L-Ctrl",
            KeyEvent.KEYCODE_CTRL_RIGHT to "R-Ctrl",
            KeyEvent.KEYCODE_ALT_LEFT to "L-Alt",
            KeyEvent.KEYCODE_ALT_RIGHT to "R-Alt",
            KeyEvent.KEYCODE_DPAD_UP to "Up",
            KeyEvent.KEYCODE_DPAD_DOWN to "Down",
            KeyEvent.KEYCODE_DPAD_LEFT to "Left",
            KeyEvent.KEYCODE_DPAD_RIGHT to "Right",
            KeyEvent.KEYCODE_0 to "0",
            KeyEvent.KEYCODE_1 to "1",
            KeyEvent.KEYCODE_2 to "2",
            KeyEvent.KEYCODE_3 to "3",
            KeyEvent.KEYCODE_4 to "4",
            KeyEvent.KEYCODE_5 to "5",
            KeyEvent.KEYCODE_6 to "6",
            KeyEvent.KEYCODE_7 to "7",
            KeyEvent.KEYCODE_8 to "8",
            KeyEvent.KEYCODE_9 to "9"
        )

        @JvmStatic
        fun getKeyName(keyCode: Int): String {
            return keyNames[keyCode] ?: "Key $keyCode"
        }

        @JvmStatic
        fun show(
            context: Context,
            title: String,
            slotIndex: Int,
            onConfirm: OnKeyConfirmed,
            onCancel: Runnable = Runnable {},
        ) {
            val dialog = StyledKeybindDialog()
            dialog.dialogTitle = title
            dialog.slotIndex = slotIndex
            dialog.onConfirm = onConfirm
            dialog.onCancel = onCancel
            dialog.show()
        }
    }
}
