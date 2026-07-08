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
import com.rian.difficultycalculator.beatmap.hitobject.Spinner

class EditorSpinnerFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Spinner Editor"
        title.setTextColor(0xFFE63E8C.toInt())
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val scene = editorScene ?: return scroll
        val data = scene.getBeatmapData() ?: return scroll
        val idx = scene.getSelectedObjectIndex()

        if (idx < 0 || idx >= data.hitObjects.objects.size) {
            addCancelButton(layout)
            scroll.addView(layout)
            return scroll
        }

        val obj = data.hitObjects.objects[idx]
        if (obj !is Spinner) {
            val notSpinner = TextView(requireContext())
            notSpinner.text = "Selected object is not a spinner"
            notSpinner.setTextColor(0xFFFF6666.toInt())
            notSpinner.textSize = 14f
            layout.addView(notSpinner)
            addCancelButton(layout)
            scroll.addView(layout)
            return scroll
        }

        val spinner = obj

        // Current duration info
        val info = TextView(requireContext())
        info.text = "Start: ${String.format("%.0f", spinner.startTime)}ms\nEnd: ${String.format("%.0f", spinner.getEndTime())}ms\nDuration: ${String.format("%.0f", spinner.getEndTime() - spinner.startTime)}ms"
        info.setTextColor(0xFFCCCCCC.toInt())
        info.textSize = 13f
        layout.addView(info)

        // End time editor
        val endTimeTitle = TextView(requireContext())
        endTimeTitle.text = "\nEnd Time (ms):"
        endTimeTitle.setTextColor(0xFFE63E8C.toInt())
        endTimeTitle.textSize = 14f
        layout.addView(endTimeTitle)

        val endTimeEdit = EditText(requireContext())
        endTimeEdit.setText(String.format("%.0f", spinner.getEndTime()))
        endTimeEdit.setTextColor(0xFFFFFFFF.toInt())
        endTimeEdit.setBackgroundColor(0xFF252540.toInt())
        endTimeEdit.setPadding(16, 12, 16, 12)
        endTimeEdit.textSize = 14f
        endTimeEdit.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.addView(endTimeEdit)

        // Duration offset editor
        val durationTitle = TextView(requireContext())
        durationTitle.text = "\nDuration Offset (ms, relative to current):"
        durationTitle.setTextColor(0xFFE63E8C.toInt())
        durationTitle.textSize = 14f
        layout.addView(durationTitle)

        val durationEdit = EditText(requireContext())
        durationEdit.setText("5000")
        durationEdit.setTextColor(0xFFFFFFFF.toInt())
        durationEdit.setBackgroundColor(0xFF252540.toInt())
        durationEdit.setPadding(16, 12, 16, 12)
        durationEdit.textSize = 14f
        durationEdit.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.addView(durationEdit)

        // Apply end time
        val applyEndBtn = Button(requireContext())
        applyEndBtn.text = "Apply End Time"
        applyEndBtn.setTextColor(0xFFFFFFFF.toInt())
        applyEndBtn.setBackgroundColor(0xFF3E8CE6.toInt())
        applyEndBtn.textSize = 14f
        applyEndBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        applyEndBtn.setOnClickListener {
            val newEnd = endTimeEdit.text.toString().toDoubleOrNull() ?: spinner.getEndTime()
            if (newEnd > spinner.startTime) {
                replaceSpinner(scene, idx, spinner, spinner.startTime, newEnd)
            }
            dismiss()
        }
        layout.addView(applyEndBtn)

        // Apply duration
        val applyDurBtn = Button(requireContext())
        applyDurBtn.text = "Apply Duration Offset"
        applyDurBtn.setTextColor(0xFFFFFFFF.toInt())
        applyDurBtn.setBackgroundColor(0xFF3E8CE6.toInt())
        applyDurBtn.textSize = 14f
        applyDurBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        applyDurBtn.setOnClickListener {
            val offset = durationEdit.text.toString().toDoubleOrNull() ?: 5000.0
            val newEnd = spinner.startTime + offset
            if (newEnd > spinner.startTime) {
                replaceSpinner(scene, idx, spinner, spinner.startTime, newEnd)
            }
            dismiss()
        }
        layout.addView(applyDurBtn)

        addCancelButton(layout)
        scroll.addView(layout)
        return scroll
    }

    private fun replaceSpinner(scene: EditorScene, idx: Int, oldSpinner: Spinner, startTime: Double, endTime: Double) {
        val data = scene.getBeatmapData() ?: return
        val newSpinner = Spinner(startTime, endTime)
        data.hitObjects.remove(idx)
        data.hitObjects.add(newSpinner)
        scene.refresh()
    }

    private fun addCancelButton(layout: LinearLayout) {
        val btn = Button(requireContext())
        btn.text = "Cancel"
        btn.setTextColor(0xFFCCCCCC.toInt())
        btn.setBackgroundColor(0xFF252540.toInt())
        btn.textSize = 16f
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btn.setOnClickListener { dismiss() }
        layout.addView(btn)
    }
}
