package ru.nsu.ccfit.zuev.osu.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData
import java.util.Locale

class EditorToolbarFragment : EditorFragment() {

    private lateinit var toolbarLayout: LinearLayout
    private lateinit var toolLabel: TextView
    private lateinit var objectInfoLabel: TextView
    private lateinit var timeLabel: TextView
    private lateinit var timelineSeekBar: SeekBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        toolbarLayout = LinearLayout(context)
        toolbarLayout.orientation = LinearLayout.VERTICAL
        toolbarLayout.setBackgroundColor(0xCC222222.toInt())
        toolbarLayout.setPadding(8, 8, 8, 8)

        val toolRow = LinearLayout(context)
        toolRow.orientation = LinearLayout.HORIZONTAL

        toolRow.addView(createToolButton("Select", EditorScene.EditorTool.Select))
        toolRow.addView(createToolButton("Circle", EditorScene.EditorTool.Circle))
        toolRow.addView(createToolButton("Slider", EditorScene.EditorTool.Slider))
        toolRow.addView(createToolButton("Spinner", EditorScene.EditorTool.Spinner))
        toolRow.addView(createToolButton("Delete", EditorScene.EditorTool.Delete))
        toolbarLayout.addView(toolRow)

        val timingRow = LinearLayout(context)
        timingRow.orientation = LinearLayout.HORIZONTAL
        timingRow.addView(createToolButton("Add BPM", EditorScene.EditorTool.TimingAdd))
        timingRow.addView(createToolButton("Del BPM", EditorScene.EditorTool.TimingDelete))
        toolbarLayout.addView(timingRow)

        toolLabel = TextView(context)
        toolLabel.text = "Tool: Select"
        toolLabel.setTextColor(0xFFCCCCCC.toInt())
        toolLabel.textSize = 12f
        toolbarLayout.addView(toolLabel)

        objectInfoLabel = TextView(context)
        objectInfoLabel.text = "No object selected"
        objectInfoLabel.setTextColor(0xFFAAAAAA.toInt())
        objectInfoLabel.textSize = 10f
        toolbarLayout.addView(objectInfoLabel)

        timeLabel = TextView(context)
        timeLabel.text = "00:00.000"
        timeLabel.setTextColor(0xFF888888.toInt())
        timeLabel.textSize = 10f
        toolbarLayout.addView(timeLabel)

        timelineSeekBar = SeekBar(context)
        timelineSeekBar.max = 1000
        timelineSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && editorScene?.beatmapData != null) {
                    val total = editorScene!!.beatmapData!!.getDuration().toFloat()
                    val time = (progress.toFloat() / 1000f) * total
                    editorScene!!.seekTo(time)
                    updateTimeDisplay()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        toolbarLayout.addView(timelineSeekBar)

        val actionRow = LinearLayout(context)
        actionRow.orientation = LinearLayout.HORIZONTAL

        val btnDeleteSelected = createActionButton("Del Sel")
        btnDeleteSelected.setOnClickListener {
            editorScene?.deleteSelectedObjects()
            updateObjectInfo()
        }
        actionRow.addView(btnDeleteSelected)

        val btnCopy = createActionButton("Copy")
        btnCopy.setOnClickListener { editorScene?.copySelected() }
        actionRow.addView(btnCopy)

        val btnPaste = createActionButton("Paste")
        btnPaste.setOnClickListener { editorScene?.pasteClipboard() }
        actionRow.addView(btnPaste)
        toolbarLayout.addView(actionRow)

        val propRow = LinearLayout(context)
        propRow.orientation = LinearLayout.HORIZONTAL

        val btnCombo = createActionButton("Combo")
        btnCombo.setOnClickListener { editorScene?.openComboEditor() }
        propRow.addView(btnCombo)

        val btnSliderProps = createActionButton("Slider")
        btnSliderProps.setOnClickListener { editorScene?.openSliderEditor() }
        propRow.addView(btnSliderProps)
        toolbarLayout.addView(propRow)

        return toolbarLayout
    }

    private fun createToolButton(label: String, tool: EditorScene.EditorTool): Button {
        val btn = Button(context)
        btn.text = label
        btn.textSize = 10f
        btn.setPadding(4, 4, 4, 4)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(2, 2, 2, 2)
        btn.layoutParams = params

        btn.setOnClickListener {
            editorScene?.currentTool = tool
            toolLabel.text = "Tool: $label"
        }

        return btn
    }

    private fun createActionButton(label: String): Button {
        val btn = Button(context)
        btn.text = label
        btn.textSize = 10f
        btn.setPadding(4, 4, 4, 4)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(2, 2, 2, 2)
        btn.layoutParams = params

        return btn
    }

    fun setEditorScene(scene: EditorScene) {
        this.editorScene = scene
    }

    fun updateObjectInfo() {
        if (editorScene?.beatmapData == null) return

        val index = editorScene!!.selectedObjectIndex
        if (index >= 0) {
            val obj = editorScene!!.beatmapData!!.hitObjects.objects[index]
            val type = obj.javaClass.simpleName
            val info = String.format(
                Locale.US, "#%d %s at (%.0f, %.0f) time=%.0fms",
                index, type, obj.position.x, obj.position.y, obj.startTime
            )
            objectInfoLabel.text = info
        } else {
            objectInfoLabel.text = "No object selected"
        }
    }

    fun updateTimeDisplay() {
        if (editorScene == null) return

        val time = editorScene!!.currentTime
        val total = editorScene?.beatmapData?.getDuration()?.toFloat() ?: 0f

        timeLabel.text = "${formatTime(time)} / ${formatTime(total)}"

        if (total > 0) {
            timelineSeekBar.progress = ((time / total) * 1000).toInt()
        }
    }

    private fun formatTime(ms: Float): String {
        val totalSec = (ms / 1000).toInt()
        val min = totalSec / 60
        val sec = totalSec % 60
        val millis = (ms % 1000).toInt()
        return String.format("%02d:%02d.%03d", min, sec, millis)
    }
}
