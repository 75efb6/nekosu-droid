package ru.nsu.ccfit.zuev.osu.editor

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class EditorComboFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Combo Editor"
        title.setTextColor(0xFFE63E8C.toInt())
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val scene = editorScene ?: return scroll
        val data = scene.getBeatmapData() ?: return scroll
        val idx = scene.getSelectedObjectIndex()

        if (idx < 0) {
            val noSel = TextView(requireContext())
            noSel.text = "Select an object first"
            noSel.setTextColor(0xFF999999.toInt())
            noSel.textSize = 14f
            layout.addView(noSel)
            addCancelButton(layout)
            scroll.addView(layout)
            return scroll
        }

        val objects = data.hitObjects.objects
        if (idx >= objects.size) {
            addCancelButton(layout)
            scroll.addView(layout)
            return scroll
        }

        val obj = objects[idx]
        val type = obj.javaClass.simpleName

        // Object info
        val info = TextView(requireContext())
        info.text = "Object #$idx ($type)\nPosition: (${String.format("%.0f", obj.position.x)}, ${String.format("%.0f", obj.position.y)})\nTime: ${String.format("%.0f", obj.startTime)}ms"
        info.setTextColor(0xFFCCCCCC.toInt())
        info.textSize = 13f
        layout.addView(info)

        // New Combo toggle
        val comboFlags = scene.getNewComboFlags()
        val isNewCombo = comboFlags[idx] ?: false

        val comboTitle = TextView(requireContext())
        comboTitle.text = "\nNew Combo:"
        comboTitle.setTextColor(0xFFE63E8C.toInt())
        comboTitle.textSize = 14f
        layout.addView(comboTitle)

        val comboToggle = Button(requireContext())
        comboToggle.text = if (isNewCombo) "NEW COMBO: ON (tap to toggle)" else "NEW COMBO: OFF (tap to toggle)"
        comboToggle.setTextColor(0xFFFFFFFF.toInt())
        comboToggle.setBackgroundColor(if (isNewCombo) 0xFFE63E8C.toInt() else 0xFF252540.toInt())
        comboToggle.textSize = 14f
        val toggleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        toggleParams.setMargins(0, 8, 0, 16)
        comboToggle.layoutParams = toggleParams
        comboToggle.setOnClickListener {
            val current = comboFlags[idx] ?: false
            comboFlags[idx] = !current
            scene.refresh()
            dismiss()
        }
        layout.addView(comboToggle)

        // Combo color selector
        val colorTitle = TextView(requireContext())
        colorTitle.text = "Combo Color:"
        colorTitle.setTextColor(0xFFE63E8C.toInt())
        colorTitle.textSize = 14f
        layout.addView(colorTitle)

        val colors = data.colors.comboColors
        if (colors.isNotEmpty()) {
            val colorRow = LinearLayout(requireContext())
            colorRow.orientation = LinearLayout.HORIZONTAL
            colorRow.gravity = Gravity.CENTER

            for (i in colors.indices) {
                val colorSwatch = View(requireContext())
                val size = 48
                colorSwatch.layoutParams = LinearLayout.LayoutParams(size, size)
                val c = colors[i]
                colorSwatch.setBackgroundColor(Color.rgb((c.r() * 255).toInt(), (c.g() * 255).toInt(), (c.b() * 255).toInt()))
                val margin = 8
                (colorSwatch.layoutParams as LinearLayout.LayoutParams).setMargins(margin, margin, margin, margin)

                // Count current combo index for this object
                var comboIdx = 0
                for (j in 0 until idx) {
                    if (comboFlags[j] == true) comboIdx++
                }
                val isSelected = (comboIdx % colors.size) == i
                if (isSelected) {
                    colorSwatch.alpha = 1.0f
                    val border = FrameLayout(requireContext())
                    border.setBackgroundColor(0xFFFFFFFF.toInt())
                    border.layoutParams = LinearLayout.LayoutParams(size + 8, size + 8)
                    (border.layoutParams as LinearLayout.LayoutParams).setMargins(margin - 4, margin - 4, margin - 4, margin - 4)
                    border.addView(colorSwatch)
                    colorRow.addView(border)
                } else {
                    colorSwatch.alpha = 0.6f
                    colorRow.addView(colorSwatch)
                }
            }
            layout.addView(colorRow)
        }

        addCancelButton(layout)
        scroll.addView(layout)
        return scroll
    }

    private fun addCancelButton(layout: LinearLayout) {
        val btn = Button(requireContext())
        btn.text = "Close"
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
