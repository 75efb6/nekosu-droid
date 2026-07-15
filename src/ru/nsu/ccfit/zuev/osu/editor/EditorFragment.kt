package ru.nsu.ccfit.zuev.osu.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.edlplan.ui.ActivityOverlay
import com.edlplan.ui.fragment.BackPressListener

open class EditorFragment : Fragment(), BackPressListener {

    var editorScene: EditorScene? = null
        internal set

    fun withEditor(scene: EditorScene): EditorFragment {
        this.editorScene = scene
        @Suppress("UNCHECKED_CAST")
        return this as EditorFragment
    }

    open fun show() {
        val tag = javaClass.name + "@" + hashCode()
        ActivityOverlay.addOverlay(this, tag)
    }

    override fun callDismissOnBackPress() {
        dismiss()
    }

    open fun dismiss() {
        ActivityOverlay.dismissOverlay(this)
    }
}
