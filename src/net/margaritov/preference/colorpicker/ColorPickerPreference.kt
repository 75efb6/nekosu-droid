package net.margaritov.preference.colorpicker

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import ru.nsu.ccfit.zuev.osuplus.R // adjust to your actual R package

class ColorPickerPreference : Preference, Preference.OnPreferenceClickListener, ColorPickerDialog.OnColorChangedListener {

    internal var mView: View? = null
    internal var mDialog: ColorPickerDialog? = null
    private var mValue = Color.BLACK
    private var mDensity = 0f
    private var mAlphaSliderEnabled = false
    private var mHexValueEnabled = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        val mHexDefaultValue = a.getString(index)
        if (mHexDefaultValue != null && mHexDefaultValue.startsWith("#")) {
            return convertToColorInt(mHexDefaultValue)
        } else {
            return a.getColor(index, Color.BLACK)
        }
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        onColorChanged(if (restoreValue) getPersistedInt(mValue) else defaultValue as Int)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        mDensity = this.context.resources.displayMetrics.density
        onPreferenceClickListener = this

        // Give this preference its own dedicated widget layout so the
        // RecyclerView adapter never assigns it the same view type as a
        // plain Preference. Without this, ColorPickerPreference's
        // onBindViewHolder mutates android.R.id.widget_frame on a view
        // that can later be recycled by an unrelated plain Preference
        // (e.g. "update", "clear", "registerAcc"), which never resets
        // that frame back — the stale swatch/content bleeds into
        // whatever preference reuses the recycled view next.
        widgetLayoutResource = R.layout.preference_widget_colorpicker

        if (attrs != null) {
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", false)
            mHexValueEnabled = attrs.getAttributeBooleanValue(null, "hexValue", false)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mView = holder.itemView
        setPreviewColor(holder)
    }

    private fun setPreviewColor(holder: PreferenceViewHolder) {
        val iView = holder.findViewById(R.id.color_picker_swatch) as? ImageView ?: return
        iView.setBackgroundDrawable(AlphaPatternDrawable((5 * mDensity).toInt()))
        iView.setImageBitmap(getPreviewBitmap())
    }

    private fun getPreviewBitmap(): Bitmap {
        val d = (mDensity * 31).toInt()
        val color = mValue
        val bm = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888)
        val w = bm.width
        val h = bm.height
        var c: Int
        for (i in 0 until w) {
            for (j in i until h) {
                c = if (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2) Color.GRAY else color
                bm.setPixel(i, j, c)
                if (i != j) {
                    bm.setPixel(j, i, c)
                }
            }
        }
        return bm
    }

    override fun onColorChanged(color: Int) {
        if (isPersistent) {
            persistInt(color)
        }
        mValue = color
        mView?.let { view ->
            val iView = view.findViewById<ImageView>(R.id.color_picker_swatch)
            iView?.setImageBitmap(getPreviewBitmap())
        }
        try {
            onPreferenceChangeListener?.onPreferenceChange(this, color)
        } catch (_: NullPointerException) {
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        showDialog(null)
        return false
    }

    protected fun showDialog(state: Bundle?) {
        mDialog = ColorPickerDialog(context, mValue, title.toString())
        mDialog!!.setOnColorChangedListener(this)
        if (mAlphaSliderEnabled) {
            mDialog!!.setAlphaSliderVisible(true)
        }
        if (mHexValueEnabled) {
            mDialog!!.setHexValueEnabled(true)
        }
        if (state != null) {
            mDialog!!.onRestoreInstanceState(state)
        }
        mDialog!!.show()
    }

    fun setAlphaSliderEnabled(enable: Boolean) {
        mAlphaSliderEnabled = enable
    }

    fun setHexValueEnabled(enable: Boolean) {
        mHexValueEnabled = enable
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (mDialog == null || !mDialog!!.isShowing) {
            return superState
        }
        val myState = SavedState(superState)
        myState.dialogBundle = mDialog!!.onSaveInstanceState()
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        showDialog(state.dialogBundle)
    }

    private class SavedState : BaseSavedState {
        var dialogBundle: Bundle? = null

        constructor(source: Parcel) : super(source) {
            dialogBundle = source.readBundle()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeBundle(dialogBundle)
        }

        constructor(superState: Parcelable?) : super(superState)

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState = SavedState(`in`)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    companion object {
        @JvmStatic
        fun convertToARGB(color: Int): String {
            var alpha = Integer.toHexString(Color.alpha(color))
            var red = Integer.toHexString(Color.red(color))
            var green = Integer.toHexString(Color.green(color))
            var blue = Integer.toHexString(Color.blue(color))

            if (alpha.length == 1) alpha = "0$alpha"
            if (red.length == 1) red = "0$red"
            if (green.length == 1) green = "0$green"
            if (blue.length == 1) blue = "0$blue"

            return "#$alpha$red$green$blue"
        }

        @JvmStatic
        fun convertToRGB(color: Int): String {
            var red = Integer.toHexString(Color.red(color))
            var green = Integer.toHexString(Color.green(color))
            var blue = Integer.toHexString(Color.blue(color))

            if (red.length == 1) red = "0$red"
            if (green.length == 1) green = "0$green"
            if (blue.length == 1) blue = "0$blue"

            return "#$red$green$blue"
        }

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun convertToColorInt(argb: String): Int {
            var argb = argb
            if (!argb.startsWith("#")) {
                argb = "#$argb"
            }
            return Color.parseColor(argb)
        }
    }
}