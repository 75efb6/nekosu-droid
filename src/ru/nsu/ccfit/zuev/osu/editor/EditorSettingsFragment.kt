package ru.nsu.ccfit.zuev.osu.editor

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import ru.nsu.ccfit.zuev.osu.ToastLogger

class EditorSettingsFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Editor Menu"
        title.setTextColor(0xFFE63E8C.toInt())
        title.textSize = 22f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val scene = editorScene

        // === File Operations ===
        sectionHeader(layout, "File")
        addButton(layout, "Save (.osu)") { saveBeatmap() }

        // === Edit Operations ===
        sectionHeader(layout, "Edit")
        addButton(layout, "Edit Metadata") {
            dismiss()
            EditorMetadataFragment().withEditor(scene!!).show()
        }
        addButton(layout, "Edit Difficulty") {
            dismiss()
            EditorDifficultyFragment().withEditor(scene!!).show()
        }
        addButton(layout, "Edit Timing Points") {
            dismiss()
            EditorTimingFragment().withEditor(scene!!).show()
        }

        // === Object Operations ===
        sectionHeader(layout, "Objects")
        addButton(layout, "Edit Combos") {
            dismiss()
            scene?.openComboEditor()
        }

        if (scene != null && scene.selectedObjectIndex >= 0 && scene.beatmapData != null) {
            val objects = scene.beatmapData!!.hitObjects.objects
            if (scene.selectedObjectIndex < objects.size &&
                objects[scene.selectedObjectIndex] is com.rian.difficultycalculator.beatmap.hitobject.Slider) {
                addButton(layout, "Edit Slider") {
                    dismiss()
                    scene.openSliderEditor()
                }
            }
            if (scene.selectedObjectIndex < objects.size &&
                objects[scene.selectedObjectIndex] is com.rian.difficultycalculator.beatmap.hitobject.Spinner) {
                addButton(layout, "Edit Spinner Duration") {
                    dismiss()
                    scene.openSpinnerEditor()
                }
            }
        }

        // === Clipboard ===
        sectionHeader(layout, "Clipboard")
        addButton(layout, "Copy Selected") {
            scene?.copySelected()
            ToastLogger.showText("Copied ${scene?.selectedObjects?.size ?: 0} object(s)", false)
        }
        addButton(layout, "Paste") {
            scene?.pasteClipboard()
            ToastLogger.showText("Pasted objects", false)
        }
        addButton(layout, "Duplicate Selected") {
            scene?.copySelected()
            scene?.pasteClipboard()
            ToastLogger.showText("Duplicated", false)
        }

        // === Selection ===
        sectionHeader(layout, "Selection")
        val multiLabel = if (scene?.isMultiSelectMode() == true) "Multi-Select: ON" else "Multi-Select: OFF"
        addButton(layout, multiLabel) {
            scene?.toggleMultiSelect()
            dismiss()
            val s = scene
            if (s != null) EditorSettingsFragment().withEditor(s).show()
        }
        addButton(layout, "Select All") {
            scene?.selectAll()
            ToastLogger.showText("Selected all objects", false)
        }
        addButton(layout, "Deselect All") {
            scene?.deselectAll()
            ToastLogger.showText("Deselected all", false)
        }
        addButton(layout, "Delete Selected") {
            scene?.deleteSelectedObjects()
            ToastLogger.showText("Deleted selected objects", false)
        }

        // === View Settings ===
        sectionHeader(layout, "View")
        val snapLabel = if (scene?.isGridSnapEnabled == true) "Grid Snap: ON" else "Grid Snap: OFF"
        addButton(layout, snapLabel) {
            scene?.toggleGridSnap()
            dismiss()
            val s = scene
            if (s != null) EditorSettingsFragment().withEditor(s).show()
        }

        // === Playback ===
        sectionHeader(layout, "Playback")
        addButton(layout, "Test Play (from start)") {
            dismiss()
            scene?.testPlay()
        }
        addButton(layout, "Return to Menu") {
            dismiss()
            scene?.back()
        }

        scroll.addView(layout)
        return scroll
    }

    private fun sectionHeader(layout: LinearLayout, text: String) {
        val header = TextView(requireContext())
        header.text = "\n$text"
        header.setTextColor(0xFFE63E8C.toInt())
        header.textSize = 16f
        header.setPadding(0, 16, 0, 4)
        layout.addView(header)
    }

    private fun addButton(layout: LinearLayout, text: String, onClick: () -> Unit) {
        val btn = Button(requireContext())
        btn.text = text
        btn.setTextColor(0xFFFFFFFF.toInt())
        btn.setBackgroundColor(0xFF252540.toInt())
        btn.textSize = 16f
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        btn.layoutParams = params
        btn.setOnClickListener { onClick() }
        layout.addView(btn)
    }

    private fun saveBeatmap() {
        val scene = editorScene ?: return
        val data = scene.beatmapData ?: return
        val path = scene.getBeatmapPath() ?: return

        val file = java.io.File(path)
        val success = BeatmapEncoder.encode(data, file, scene.getKiaiFlags())

        ToastLogger.showText(
            if (success) "Beatmap saved!" else "Failed to save beatmap",
            !success
        )
        dismiss()
    }
}
