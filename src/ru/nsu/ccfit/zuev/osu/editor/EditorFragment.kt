package ru.nsu.ccfit.zuev.osu.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.edlplan.ui.ActivityOverlay

open class EditorFragment : Fragment() {

    var editorScene: EditorScene? = null
        private set

    fun withEditor(scene: EditorScene): EditorFragment {
        this.editorScene = scene
        @Suppress("UNCHECKED_CAST")
        return this as EditorFragment
    }

    open fun show() {
        val tag = javaClass.name + "@" + hashCode()
        ActivityOverlay.addOverlay(this, tag)
    }

    open fun dismiss() {
        if (isAdded) {
            activity?.supportFragmentManager?.let { fm ->
                fm.beginTransaction().remove(this).commitAllowingStateLoss()
            }
        }
    }
}
