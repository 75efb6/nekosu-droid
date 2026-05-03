package com.reco1l.legacy.ui

import android.animation.Animator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.preference.ListPreference
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import ru.nsu.ccfit.zuev.osuplus.R

class StyledSelectionDialog : BaseFragment() {

    override val layoutID = R.layout.dialog_styled_selection

    private var dialogTitle = ""
    private var entries = emptyList<String>()
    private var entryValues = emptyList<String>()
    private var currentValue: String? = null
    private var onSelect: ((String) -> Unit)? = null

    init {
        isDismissOnBackgroundClick = true
    }

    override fun onLoadView() {
        findViewById<TextView>(R.id.selection_title)!!.text = dialogTitle

        val list = findViewById<LinearLayout>(R.id.selection_list)!!
        val inflater = LayoutInflater.from(requireContext())

        entries.forEachIndexed { index, label ->
            val value = entryValues.getOrNull(index) ?: return@forEachIndexed
            val row = inflater.inflate(R.layout.item_selection_row, list, false)

            row.findViewById<TextView>(R.id.item_label).apply {
                text = label
                if (value == currentValue) setTextColor(requireContext().getColor(R.color.accentPrimary))
            }

            val check = row.findViewById<ImageView>(R.id.item_check)
            if (value == currentValue) {
                check.setImageResource(R.drawable.ic_check)
                check.visibility = View.VISIBLE
            }

            row.setOnClickListener {
                dismiss()
                onSelect?.invoke(value)
            }

            list.addView(row)
        }

        val scrollView = findViewById<ScrollView>(R.id.selection_scroll)!!
        scrollView.post {
            val maxHeight = (resources.displayMetrics.heightPixels * 0.42f).toInt()
            if (scrollView.measuredHeight > maxHeight) {
                scrollView.layoutParams.height = maxHeight
                scrollView.requestLayout()
            }
        }

        playOnLoadAnim()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.body)!!
        body.alpha = 0f
        body.translationY = 200f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    override fun dismiss() {
        val body = findViewById<View>(R.id.body) ?: return super.dismiss()
        body.animate().cancel()
        body.animate()
            .translationYBy(200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) = super@StyledSelectionDialog.dismiss()
            })
            .start()
        playBackgroundHideOutAnim(200)
    }

    companion object {

        @JvmStatic
        fun show(context: Context, preference: ListPreference): Boolean {
            val entries = preference.entries ?: return false
            val entryValues = preference.entryValues ?: return false

            show(
                context = context,
                title = preference.title?.toString() ?: "",
                entries = entries.map { it.toString() },
                entryValues = entryValues.map { it.toString() },
                currentValue = preference.value,
            ) { selected ->
                if (preference.callChangeListener(selected)) {
                    preference.value = selected
                }
            }
            return true
        }

        @JvmStatic
        fun show(
            context: Context,
            title: String,
            entries: List<String>,
            entryValues: List<String>,
            currentValue: String?,
            onSelect: (String) -> Unit,
        ) {
            val dialog = StyledSelectionDialog()
            dialog.dialogTitle = title
            dialog.entries = entries
            dialog.entryValues = entryValues
            dialog.currentValue = currentValue
            dialog.onSelect = onSelect
            dialog.show()
        }
    }
}
