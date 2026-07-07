package ru.nsu.ccfit.zuev.osu.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.rian.difficultycalculator.beatmap.hitobject.HitObject;

import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * Editor toolbar fragment overlay.
 * Provides tools for placing, selecting, and editing hit objects.
 */
public class EditorToolbarFragment extends Fragment {

    private EditorScene editorScene;
    private LinearLayout toolbarLayout;
    private TextView toolLabel;
    private TextView objectInfoLabel;
    private TextView timeLabel;
    private SeekBar timelineSeekBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        toolbarLayout = new LinearLayout(getContext());
        toolbarLayout.setOrientation(LinearLayout.VERTICAL);
        toolbarLayout.setBackgroundColor(0xCC222222);
        toolbarLayout.setPadding(8, 8, 8, 8);

        // Tool buttons row
        LinearLayout toolRow = new LinearLayout(getContext());
        toolRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSelect = createToolButton("Select", EditorScene.EditorTool.Select);
        Button btnCircle = createToolButton("Circle", EditorScene.EditorTool.Circle);
        Button btnSlider = createToolButton("Slider", EditorScene.EditorTool.Slider);
        Button btnSpinner = createToolButton("Spinner", EditorScene.EditorTool.Spinner);
        Button btnDelete = createToolButton("Delete", EditorScene.EditorTool.Delete);

        toolRow.addView(btnSelect);
        toolRow.addView(btnCircle);
        toolRow.addView(btnSlider);
        toolRow.addView(btnSpinner);
        toolRow.addView(btnDelete);
        toolbarLayout.addView(toolRow);

        // Tool label
        toolLabel = new TextView(getContext());
        toolLabel.setText("Tool: Select");
        toolLabel.setTextColor(0xFFCCCCCC);
        toolLabel.setTextSize(12);
        toolbarLayout.addView(toolLabel);

        // Object info
        objectInfoLabel = new TextView(getContext());
        objectInfoLabel.setText("No object selected");
        objectInfoLabel.setTextColor(0xFFAAAAAA);
        objectInfoLabel.setTextSize(10);
        toolbarLayout.addView(objectInfoLabel);

        // Time info
        timeLabel = new TextView(getContext());
        timeLabel.setText("00:00.000");
        timeLabel.setTextColor(0xFF888888);
        timeLabel.setTextSize(10);
        toolbarLayout.addView(timeLabel);

        // Timeline seek bar
        timelineSeekBar = new SeekBar(getContext());
        timelineSeekBar.setMax(1000);
        timelineSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && editorScene != null) {
                    float time = (progress / 1000f) * editorScene.getBeatmapData().getDuration();
                    editorScene.seekTo(time);
                    updateTimeDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        toolbarLayout.addView(timelineSeekBar);

        // Action buttons row
        LinearLayout actionRow = new LinearLayout(getContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnDeleteSelected = createActionButton("Delete Selected");
        btnDeleteSelected.setOnClickListener(v -> {
            if (editorScene != null) {
                editorScene.deleteSelectedObject();
                updateObjectInfo();
            }
        });
        actionRow.addView(btnDeleteSelected);

        Button btnTimingAdd = createToolButton("Add Timing", EditorScene.EditorTool.TimingAdd);
        actionRow.addView(btnTimingAdd);

        toolbarLayout.addView(actionRow);

        return toolbarLayout;
    }

    private Button createToolButton(String label, EditorScene.EditorTool tool) {
        Button btn = new Button(getContext());
        btn.setText(label);
        btn.setTextSize(10);
        btn.setPadding(4, 4, 4, 4);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(2, 2, 2, 2);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            if (editorScene != null) {
                editorScene.setCurrentTool(tool);
                toolLabel.setText("Tool: " + label);
            }
        });

        return btn;
    }

    private Button createActionButton(String label) {
        Button btn = new Button(getContext());
        btn.setText(label);
        btn.setTextSize(10);
        btn.setPadding(4, 4, 4, 4);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(2, 2, 2, 2);
        btn.setLayoutParams(params);

        return btn;
    }

    public void setEditorScene(EditorScene scene) {
        this.editorScene = scene;
    }

    public void updateObjectInfo() {
        if (editorScene == null || editorScene.getBeatmapData() == null) return;

        int index = editorScene.getSelectedObjectIndex();
        if (index >= 0) {
            HitObject obj = editorScene.getBeatmapData().hitObjects.getObjects().get(index);
            String type = obj.getClass().getSimpleName();
            String info = String.format(Locale.US, "#%d %s at (%.0f, %.0f) time=%.0fms",
                    index, type, obj.getPosition().x, obj.getPosition().y, obj.getStartTime());
            objectInfoLabel.setText(info);
        } else {
            objectInfoLabel.setText("No object selected");
        }
    }

    public void updateTimeDisplay() {
        if (editorScene == null) return;

        float time = editorScene.getCurrentTime();
        float total = (float) editorScene.getBeatmapData().getDuration();

        timeLabel.setText(formatTime(time) + " / " + formatTime(total));

        if (timelineSeekBar != null && total > 0) {
            timelineSeekBar.setProgress((int) ((time / total) * 1000));
        }
    }

    private String formatTime(float ms) {
        int totalSec = (int) (ms / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        int millis = (int) (ms % 1000);
        return String.format("%02d:%02d.%03d", min, sec, millis);
    }
}
