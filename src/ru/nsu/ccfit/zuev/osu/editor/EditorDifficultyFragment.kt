package ru.nsu.ccfit.zuev.osu.editor

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class EditorDifficultyFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Edit Difficulty"
        title.setTextColor(0xFFE63E8C.toInt())
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val data = editorScene?.beatmapData

        val csEdit = addField(layout, "Circle Size (CS)", data?.difficulty?.cs?.toString() ?: "4")
        val arEdit = addField(layout, "Approach Rate (AR)", data?.difficulty?.ar?.toString() ?: "9")
        val odEdit = addField(layout, "Overall Difficulty (OD)", data?.difficulty?.od?.toString() ?: "8")
        val hpEdit = addField(layout, "HP Drain Rate", data?.difficulty?.hp?.toString() ?: "7")
        val smEdit = addField(layout, "Slider Multiplier", data?.difficulty?.sliderMultiplier?.toString() ?: "1.4")
        val trEdit = addField(layout, "Slider Tick Rate", data?.difficulty?.sliderTickRate?.toString() ?: "1")

        val saveBtn = Button(requireContext())
        saveBtn.text = "Save"
        saveBtn.setTextColor(0xFFFFFFFF.toInt())
        saveBtn.setBackgroundColor(0xFFE63E8C.toInt())
        saveBtn.textSize = 16f
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 24, 0, 12)
        saveBtn.layoutParams = params
        saveBtn.setOnClickListener {
            data?.difficulty?.apply {
                cs = parseFloatSafe(csEdit.text.toString(), cs ?: 4f)
                ar = parseFloatSafe(arEdit.text.toString(), ar ?: 9f)
                od = parseFloatSafe(odEdit.text.toString(), od ?: 8f)
                hp = parseFloatSafe(hpEdit.text.toString(), hp ?: 7f)
                sliderMultiplier = parseDoubleSafe(smEdit.text.toString(), sliderMultiplier ?: 1.4)
                sliderTickRate = parseDoubleSafe(trEdit.text.toString(), sliderTickRate ?: 1.0)
            }
            dismiss()
        }
        layout.addView(saveBtn)

        val cancelBtn = Button(requireContext())
        cancelBtn.text = "Cancel"
        cancelBtn.setTextColor(0xFFCCCCCC.toInt())
        cancelBtn.setBackgroundColor(0xFF252540.toInt())
        cancelBtn.textSize = 16f
        cancelBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cancelBtn.setOnClickListener { dismiss() }
        layout.addView(cancelBtn)

        scroll.addView(layout)
        return scroll
    }

    private fun addField(layout: LinearLayout, label: String, value: String): EditText {
        val lbl = TextView(requireContext())
        lbl.text = label
        lbl.setTextColor(0xFF9999BB.toInt())
        lbl.textSize = 12f
        layout.addView(lbl)

        val edit = EditText(requireContext())
        edit.setText(value)
        edit.setTextColor(0xFFFFFFFF.toInt())
        edit.setBackgroundColor(0xFF252540.toInt())
        edit.setPadding(16, 12, 16, 12)
        edit.textSize = 14f
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 4, 0, 16)
        edit.layoutParams = params
        layout.addView(edit)
        return edit
    }

    private fun parseFloatSafe(s: String, default: Float): Float {
        return s.toFloatOrNull() ?: default
    }

    private fun parseDoubleSafe(s: String, default: Double): Double {
        return s.toDoubleOrNull() ?: default
    }
}
