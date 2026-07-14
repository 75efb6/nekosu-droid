package net.margaritov.preference.colorpicker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.Locale

class ColorPickerDialog(context: Context, initialColor: Int, private val mTitle: String) :
    AppCompatDialog(context),
    ColorPickerView.OnColorChangedListener,
    View.OnClickListener,
    ViewTreeObserver.OnGlobalLayoutListener {

    interface OnColorChangedListener {
        fun onColorChanged(color: Int)
    }

    private lateinit var mColorPicker: ColorPickerView
    private lateinit var mOldColor: ColorPickerPanelView
    private lateinit var mNewColor: ColorPickerPanelView

    private lateinit var mHexVal: EditText
    private var mHexValueEnabled = false
    private lateinit var mHexDefaultTextColor: ColorStateList

    private var mListener: OnColorChangedListener? = null
    private var mOrientation = 0
    private lateinit var mLayout: View

    init {
        init(initialColor)
    }

    override fun onGlobalLayout() {
        if (context.resources.configuration.orientation != mOrientation) {
            val oldcolor = mOldColor.getColor()
            val newcolor = mNewColor.getColor()
            mLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)
            setUp(oldcolor)
            mNewColor.setColor(newcolor)
            mColorPicker.setColor(newcolor)
        }
    }

    private fun init(color: Int) {
        window?.setFormat(PixelFormat.RGBA_8888)
        setUp(color)
    }

    private fun setUp(color: Int) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mLayout = inflater.inflate(R.layout.dialog_color_picker, null)
        mLayout.viewTreeObserver.addOnGlobalLayoutListener(this)

        mOrientation = context.resources.configuration.orientation
        setContentView(mLayout)

        setTitle(mTitle)

        mColorPicker = mLayout.findViewById(R.id.color_picker_view) as ColorPickerView
        mOldColor = mLayout.findViewById(R.id.old_color_panel) as ColorPickerPanelView
        mNewColor = mLayout.findViewById(R.id.new_color_panel) as ColorPickerPanelView

        mHexVal = mLayout.findViewById(R.id.hex_val) as EditText
        mHexVal.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        mHexDefaultTextColor = mHexVal.textColors

        mHexVal.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                val s = mHexVal.text.toString()
                if (s.length > 5 || s.length < 10) {
                    try {
                        val c = ColorPickerPreference.convertToColorInt(s)
                        mColorPicker.setColor(c, true)
                        mHexVal.setTextColor(mHexDefaultTextColor)
                    } catch (e: IllegalArgumentException) {
                        mHexVal.setTextColor(Color.RED)
                    }
                } else {
                    mHexVal.setTextColor(Color.RED)
                }
                true
            } else {
                false
            }
        }

        (mOldColor.parent as LinearLayout).setPadding(
            Math.round(mColorPicker.getDrawingOffset()),
            0,
            Math.round(mColorPicker.getDrawingOffset()),
            0
        )

        mOldColor.setOnClickListener(this)
        mNewColor.setOnClickListener(this)
        mColorPicker.setOnColorChangedListener(this)
        mOldColor.setColor(color)
        mColorPicker.setColor(color, true)
    }

    override fun onColorChanged(color: Int) {
        mNewColor.setColor(color)
        if (mHexValueEnabled) updateHexValue(color)
    }

    fun setHexValueEnabled(enable: Boolean) {
        mHexValueEnabled = enable
        if (enable) {
            mHexVal.visibility = View.VISIBLE
            updateHexLengthFilter()
            updateHexValue(getColor())
        } else {
            mHexVal.visibility = View.GONE
        }
    }

    fun getHexValueEnabled(): Boolean = mHexValueEnabled

    private fun updateHexLengthFilter() {
        if (getAlphaSliderVisible()) {
            mHexVal.filters = arrayOf(InputFilter.LengthFilter(9))
        } else {
            mHexVal.filters = arrayOf(InputFilter.LengthFilter(7))
        }
    }

    private fun updateHexValue(color: Int) {
        if (getAlphaSliderVisible()) {
            mHexVal.setText(ColorPickerPreference.convertToARGB(color).uppercase(Locale.getDefault()))
        } else {
            mHexVal.setText(ColorPickerPreference.convertToRGB(color).uppercase(Locale.getDefault()))
        }
        mHexVal.setTextColor(mHexDefaultTextColor)
    }

    fun setAlphaSliderVisible(visible: Boolean) {
        mColorPicker.setAlphaSliderVisible(visible)
        if (mHexValueEnabled) {
            updateHexLengthFilter()
            updateHexValue(getColor())
        }
    }

    fun getAlphaSliderVisible(): Boolean = mColorPicker.getAlphaSliderVisible()

    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        mListener = listener
    }

    fun getColor(): Int = mColorPicker.getColor()

    override fun onClick(v: View) {
        if (v.id == R.id.new_color_panel) {
            mListener?.onColorChanged(mNewColor.getColor())
        }
        dismiss()
    }

    override fun onSaveInstanceState(): Bundle {
        val state = super.onSaveInstanceState()!!
        state.putInt("old_color", mOldColor.getColor())
        state.putInt("new_color", mNewColor.getColor())
        return state
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mOldColor.setColor(savedInstanceState.getInt("old_color"))
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true)
    }
}
