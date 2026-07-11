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
import com.rian.difficultycalculator.beatmap.hitobject.Slider
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath
import com.rian.difficultycalculator.beatmap.hitobject.SliderPathType
import com.rian.difficultycalculator.math.Vector2

class EditorSliderFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Slider Editor"
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
        if (obj !is Slider) {
            val notSlider = TextView(requireContext())
            notSlider.text = "Selected object is not a slider"
            notSlider.setTextColor(0xFFFF6666.toInt())
            notSlider.textSize = 14f
            layout.addView(notSlider)
            addCancelButton(layout)
            scroll.addView(layout)
            return scroll
        }

        val slider = obj

        // Current path type
        val pathTypeLabel = TextView(requireContext())
        pathTypeLabel.text = "Path Type: ${slider.path.pathType}"
        pathTypeLabel.setTextColor(0xFFCCCCCC.toInt())
        pathTypeLabel.textSize = 14f
        layout.addView(pathTypeLabel)

        // Path type selector
        val types = arrayOf("Linear", "PerfectCurve", "Catmull", "Bezier")
        val currentType = slider.path.pathType

        for (type in types) {
            val btn = Button(requireContext())
            btn.text = type
            btn.setTextColor(0xFFFFFFFF.toInt())
            val isSelected = type == currentType.name
            btn.setBackgroundColor(if (isSelected) 0xFFE63E8C.toInt() else 0xFF252540.toInt())
            btn.textSize = 14f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 6, 0, 6)
            btn.layoutParams = params
            btn.setOnClickListener {
                // Recreate slider with new path type
                val newType = SliderPathType.valueOf(type)
                replaceSliderPathType(scene, idx, slider, newType)
                dismiss()
            }
            layout.addView(btn)
        }

        // Repeat count
        val repeatTitle = TextView(requireContext())
        repeatTitle.text = "\nRepeat Count: ${slider.getRepeatCount()}"
        repeatTitle.setTextColor(0xFFE63E8C.toInt())
        repeatTitle.textSize = 14f
        layout.addView(repeatTitle)

        val repeatEdit = EditText(requireContext())
        repeatEdit.setText(slider.getRepeatCount().toString())
        repeatEdit.setTextColor(0xFFFFFFFF.toInt())
        repeatEdit.setBackgroundColor(0xFF252540.toInt())
        repeatEdit.setPadding(16, 12, 16, 12)
        repeatEdit.textSize = 14f
        repeatEdit.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.addView(repeatEdit)

        val repeatBtn = Button(requireContext())
        repeatBtn.text = "Apply Repeat Count"
        repeatBtn.setTextColor(0xFFFFFFFF.toInt())
        repeatBtn.setBackgroundColor(0xFF3E8CE6.toInt())
        repeatBtn.textSize = 14f
        repeatBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        repeatBtn.setOnClickListener {
            val newRepeat = repeatEdit.text.toString().toIntOrNull() ?: slider.getRepeatCount()
            if (newRepeat >= 1) {
                replaceSliderRepeat(scene, idx, slider, newRepeat)
            }
            dismiss()
        }
        layout.addView(repeatBtn)

        // Control points info
        val cpTitle = TextView(requireContext())
        cpTitle.text = "\nControl Points (${slider.path.controlPoints.size}):"
        cpTitle.setTextColor(0xFFE63E8C.toInt())
        cpTitle.textSize = 14f
        layout.addView(cpTitle)

        for (i in slider.path.controlPoints.indices) {
            val cp = slider.path.controlPoints[i]
            val cpText = TextView(requireContext())
            cpText.text = String.format("  #%d: (%.0f, %.0f)", i, cp.x, cp.y)
            cpText.setTextColor(0xFFCCCCCC.toInt())
            cpText.textSize = 12f
            layout.addView(cpText)
        }

        // Distance
        val distText = TextView(requireContext())
        distText.text = "\nExpected Distance: ${String.format("%.1f", slider.path.expectedDistance)}"
        distText.setTextColor(0xFFCCCCCC.toInt())
        distText.textSize = 13f
        layout.addView(distText)

        addCancelButton(layout)
        scroll.addView(layout)
        return scroll
    }

    private fun replaceSliderPathType(scene: EditorScene, idx: Int, oldSlider: Slider, newType: SliderPathType) {
        val data = scene.getBeatmapData() ?: return
        val newControlPoints = ArrayList<Vector2>(oldSlider.path.controlPoints)
        val newPath = SliderPath(newType, newControlPoints, oldSlider.path.expectedDistance)

        val timing = data.timingPoints.timing.getControlPoints().let {
            for (i in it.indices.reversed()) {
                if (oldSlider.startTime >= it[i].time) return@let it[i]
            }
            if (it.isEmpty()) null else it[0]
        } ?: return

        val difficulty = data.timingPoints.difficulty.getControlPoints().let {
            for (i in it.indices.reversed()) {
                if (oldSlider.startTime >= it[i].time) return@let it[i]
            }
            if (it.isEmpty()) null else it[0]
        } ?: return

        val newSlider = Slider(
            oldSlider.startTime,
            oldSlider.position,
            timing,
            difficulty,
            oldSlider.getRepeatCount(),
            newPath,
            data.difficulty.sliderMultiplier,
            data.difficulty.sliderTickRate,
            1.0,
            true
        )

        data.hitObjects.remove(idx)
        data.hitObjects.add(newSlider)
        scene.refresh()
    }

    private fun replaceSliderRepeat(scene: EditorScene, idx: Int, oldSlider: Slider, newRepeat: Int) {
        val data = scene.getBeatmapData() ?: return
        val newControlPoints = ArrayList<Vector2>(oldSlider.path.controlPoints)
        val newPath = SliderPath(oldSlider.path.pathType, newControlPoints, oldSlider.path.expectedDistance)

        val timing = data.timingPoints.timing.getControlPoints().let {
            for (i in it.indices.reversed()) {
                if (oldSlider.startTime >= it[i].time) return@let it[i]
            }
            if (it.isEmpty()) null else it[0]
        } ?: return

        val difficulty = data.timingPoints.difficulty.getControlPoints().let {
            for (i in it.indices.reversed()) {
                if (oldSlider.startTime >= it[i].time) return@let it[i]
            }
            if (it.isEmpty()) null else it[0]
        } ?: return

        val newSlider = Slider(
            oldSlider.startTime,
            oldSlider.position,
            timing,
            difficulty,
            newRepeat,
            newPath,
            data.difficulty.sliderMultiplier,
            data.difficulty.sliderTickRate,
            1.0,
            true
        )

        data.hitObjects.remove(idx)
        data.hitObjects.add(newSlider)
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
