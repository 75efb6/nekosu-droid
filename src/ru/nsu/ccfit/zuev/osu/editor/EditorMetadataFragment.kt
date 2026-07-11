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

class EditorMetadataFragment : EditorFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(0xF01A1A2E.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(requireContext())
        title.text = "Edit Metadata"
        title.setTextColor(0xFFE63E8C.toInt())
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val data = editorScene?.getBeatmapData()

        val titleEdit = addField(layout, "Title", data?.metadata?.title ?: "")
        val artistEdit = addField(layout, "Artist", data?.metadata?.artist ?: "")
        val creatorEdit = addField(layout, "Creator", data?.metadata?.creator ?: "")
        val versionEdit = addField(layout, "Difficulty Name", data?.metadata?.version ?: "")
        val tagsEdit = addField(layout, "Tags", data?.metadata?.tags ?: "")
        val sourceEdit = addField(layout, "Source", data?.metadata?.source ?: "")

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
            data?.metadata?.title = titleEdit.text.toString()
            data?.metadata?.artist = artistEdit.text.toString()
            data?.metadata?.creator = creatorEdit.text.toString()
            data?.metadata?.version = versionEdit.text.toString()
            data?.metadata?.tags = tagsEdit.text.toString()
            data?.metadata?.source = sourceEdit.text.toString()
            dismiss()
        }
        layout.addView(saveBtn)

        addCancelButton(layout)

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
