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
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint

class EditorTimingFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Edit Timing Points"
        title.setTextColor(0xFFE63E8C.toInt())
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val scene = editorScene ?: return scroll
        val data = scene.beatmapData ?: return scroll
        val currentTime = scene.currentTime
        val kiaiFlags = scene.kiaiFlags

        val points = data.timingPoints.timing.controlPoints
        if (points.isNotEmpty()) {
            val listTitle = TextView(requireContext())
            listTitle.text = "Current Timing Points:"
            listTitle.setTextColor(0xFF9999BB.toInt())
            listTitle.textSize = 14f
            layout.addView(listTitle)

            for (i in points.indices) {
                val tp = points[i]
                val isKiai = kiaiFlags[tp.time] ?: false

                val row = LinearLayout(requireContext())
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL

                val rowText = TextView(requireContext())
                rowText.text = String.format("#%d  %.0fms  %.1f BPM  %d/4%s",
                    i, tp.time, tp.getBPM(), tp.timeSignature,
                    if (isKiai) " [KIAI]" else "")
                rowText.setTextColor(if (isKiai) 0xFFFF6666.toInt() else 0xFFCCCCCC.toInt())
                rowText.textSize = 12f
                rowText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(rowText)

                // Kiai toggle button
                val kiaiBtn = Button(requireContext())
                kiaiBtn.text = if (isKiai) "KIAI" else "kiai"
                kiaiBtn.textSize = 10f
                kiaiBtn.setTextColor(0xFFFFFFFF.toInt())
                kiaiBtn.setBackgroundColor(if (isKiai) 0xFFCC3333.toInt() else 0xFF252540.toInt())
                kiaiBtn.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val tpTime = tp.time
                kiaiBtn.setOnClickListener {
                    val current = kiaiFlags[tpTime] ?: false
                    kiaiFlags[tpTime] = !current
                    scene.refresh()
                    dismiss()
                    EditorTimingFragment().withEditor(scene).show()
                }
                row.addView(kiaiBtn)

                layout.addView(row)
            }
        }

        // Add new timing point
        val addTitle = TextView(requireContext())
        addTitle.text = "\nAdd New Timing Point at Current Time (${String.format("%.0f", currentTime)}ms):"
        addTitle.setTextColor(0xFFE63E8C.toInt())
        addTitle.textSize = 14f
        layout.addView(addTitle)

        val bpmEdit = addField(layout, "BPM", "120")
        val sigEdit = addField(layout, "Time Signature (beats per measure)", "4")

        // Kiai toggle for new timing point
        var newKiaiEnabled = false
        val kiaiToggle = Button(requireContext())
        kiaiToggle.text = "Kiai: OFF"
        kiaiToggle.setTextColor(0xFFFFFFFF.toInt())
        kiaiToggle.setBackgroundColor(0xFF252540.toInt())
        kiaiToggle.textSize = 14f
        kiaiToggle.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        kiaiToggle.setOnClickListener {
            newKiaiEnabled = !newKiaiEnabled
            kiaiToggle.text = if (newKiaiEnabled) "Kiai: ON" else "Kiai: OFF"
            kiaiToggle.setBackgroundColor(if (newKiaiEnabled) 0xFFCC3333.toInt() else 0xFF252540.toInt())
        }
        layout.addView(kiaiToggle)

        val addBtn = Button(requireContext())
        addBtn.text = "Add Timing Point"
        addBtn.setTextColor(0xFFFFFFFF.toInt())
        addBtn.setBackgroundColor(0xFF3E8CE6.toInt())
        addBtn.textSize = 14f
        val addParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        addParams.setMargins(0, 12, 0, 12)
        addBtn.layoutParams = addParams
        addBtn.setOnClickListener {
            val bpm = bpmEdit.text.toString().toDoubleOrNull() ?: 120.0
            val sig = sigEdit.text.toString().toIntOrNull() ?: 4
            val msPerBeat = 60000.0 / bpm

            val timing = TimingControlPoint(currentTime.toDouble(), msPerBeat, sig)
            data.timingPoints.timing.add(timing)

            val diff = DifficultyControlPoint(currentTime.toDouble(), 1.0, true)
            data.timingPoints.difficulty.add(diff)

            kiaiFlags[currentTime.toDouble()] = newKiaiEnabled

            scene.refresh()
            dismiss()
        }
        layout.addView(addBtn)

        // Delete timing point
        if (points.isNotEmpty()) {
            val delTitle = TextView(requireContext())
            delTitle.text = "\nDelete Timing Point:"
            delTitle.setTextColor(0xFFFF6666.toInt())
            delTitle.textSize = 14f
            layout.addView(delTitle)

            val indexEdit = addField(layout, "Index to Delete", "0")

            val delBtn = Button(requireContext())
            delBtn.text = "Delete Timing Point"
            delBtn.setTextColor(0xFFFFFFFF.toInt())
            delBtn.setBackgroundColor(0xFFCC3333.toInt())
            delBtn.textSize = 14f
            delBtn.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            delBtn.setOnClickListener {
                val idx = indexEdit.text.toString().toIntOrNull() ?: -1
                if (idx >= 0 && idx < points.size) {
                    kiaiFlags.remove(points[idx].time)
                    data.timingPoints.timing.remove(points[idx])
                }
                scene.refresh()
                dismiss()
            }
            layout.addView(delBtn)
        }

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
}
