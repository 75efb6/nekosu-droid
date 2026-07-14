package net.margaritov.preference.colorpicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ColorPickerPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var mDensity = 1f

    private var mBorderColor = 0xff6e6e6e.toInt()
    private var mColor = 0xff000000.toInt()

    private lateinit var mBorderPaint: Paint
    private lateinit var mColorPaint: Paint

    private var mDrawingRect: RectF? = null
    private var mColorRect: RectF? = null

    private var mAlphaPattern: AlphaPatternDrawable? = null

    init {
        init()
    }

    private fun init() {
        mBorderPaint = Paint()
        mColorPaint = Paint()
        mDensity = context.resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        val rect = mColorRect ?: return

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(mDrawingRect!!, mBorderPaint)
        }

        mAlphaPattern?.draw(canvas)

        mColorPaint.color = mColor
        canvas.drawRect(rect, mColorPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mDrawingRect = RectF().apply {
            left = paddingLeft.toFloat()
            right = (w - paddingRight).toFloat()
            top = paddingTop.toFloat()
            bottom = (h - paddingBottom).toFloat()
        }

        setUpColorRect()
    }

    private fun setUpColorRect() {
        val dRect = mDrawingRect!!

        val left = dRect.left + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX
        val right = dRect.right - BORDER_WIDTH_PX

        mColorRect = RectF(left, top, right, bottom)

        mAlphaPattern = AlphaPatternDrawable((5 * mDensity).toInt())
        mAlphaPattern!!.setBounds(
            Math.round(mColorRect!!.left),
            Math.round(mColorRect!!.top),
            Math.round(mColorRect!!.right),
            Math.round(mColorRect!!.bottom)
        )
    }

    fun setColor(color: Int) {
        mColor = color
        invalidate()
    }

    fun getColor(): Int = mColor

    fun setBorderColor(color: Int) {
        mBorderColor = color
        invalidate()
    }

    fun getBorderColor(): Int = mBorderColor

    companion object {
        private const val BORDER_WIDTH_PX = 1f
    }
}
