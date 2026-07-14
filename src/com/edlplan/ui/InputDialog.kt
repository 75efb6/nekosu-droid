package com.edlplan.ui

import android.app.Dialog
import android.content.Context
import android.widget.EditText
import ru.nsu.ccfit.zuev.osuplus.R

class InputDialog(context: Context) : Dialog(context, com.google.android.material.R.style.Theme_Design_BottomSheetDialog) {

    init {
        setContentView(R.layout.dialog_for_input)
    }

    fun showForResult(onResult: OnResult) {
        findViewById<android.widget.Button>(R.id.button3).setOnClickListener {
            onResult.onResult(findViewById<EditText>(R.id.editText).text.toString())
            dismiss()
        }
        show()
    }

    interface OnResult {
        fun onResult(result: String)
    }
}
