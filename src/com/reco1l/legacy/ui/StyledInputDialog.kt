package com.reco1l.legacy.ui

import android.animation.Animator
import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.preference.EditTextPreference
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osuplus.R

class StyledInputDialog : BaseFragment() {

    override val layoutID = R.layout.dialog_styled_input

    private var dialogTitle = ""
    private var currentValue = ""
    private var inputType = InputType.TYPE_CLASS_TEXT
    private var onConfirm: ((String) -> Unit)? = null

    init {
        isDismissOnBackgroundClick = true
    }

    override fun onLoadView() {
        findViewById<TextView>(R.id.input_title)!!.text = dialogTitle

        val input = findViewById<EditText>(R.id.input_field)!!
        input.inputType = inputType
        if (currentValue.isNotEmpty()) {
            input.setText(currentValue)
            input.setSelection(currentValue.length)
        }

        fun confirm() {
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(input.windowToken, 0)
            dismiss()
            onConfirm?.invoke(input.text.toString())
        }

        input.setOnEditorActionListener { _, _, _ -> confirm(); true }
        findViewById<View>(R.id.input_confirm)!!.setOnClickListener { confirm() }

        input.post {
            input.requestFocus()
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        playOnLoadAnim()
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
                override fun onAnimationEnd(animation: Animator) = super@StyledInputDialog.dismiss()
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    fun interface Callback {
        fun onConfirm(value: String)
    }

    companion object {

        @JvmStatic
        fun show(
            context: Context,
            title: String,
            currentValue: String,
            inputType: Int,
            onConfirm: Callback,
        ) {
            show(context, title, currentValue, inputType) { onConfirm.onConfirm(it) }
        }

        @JvmStatic
        fun show(
            context: Context,
            preference: EditTextPreference,
            inputType: Int = InputType.TYPE_CLASS_TEXT,
        ) {
            show(
                context = context,
                title = preference.title?.toString() ?: "",
                currentValue = preference.text ?: "",
                inputType = inputType,
            ) { newValue ->
                if (preference.callChangeListener(newValue)) {
                    preference.text = newValue
                }
            }
        }

        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun show(
            context: Context,
            title: String,
            currentValue: String = "",
            inputType: Int = InputType.TYPE_CLASS_TEXT,
            onConfirm: (String) -> Unit,
        ) {
            val dialog = StyledInputDialog()
            dialog.dialogTitle = title
            dialog.currentValue = currentValue
            dialog.inputType = inputType
            dialog.onConfirm = onConfirm
            dialog.show()
        }
    }
}
