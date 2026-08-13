package net.margaritov.preference.colorpicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface OnColorChangedListener {
        fun onColorChanged(color: Int)
    }

    private var mDensity = 1f

    private var mListener: OnColorChangedListener? = null

    private lateinit var mSatValPaint: Paint
    private lateinit var mSatValTrackerPaint: Paint
    private lateinit var mHuePaint: Paint
    private lateinit var mHueTrackerPaint: Paint
    private lateinit var mAlphaPaint: Paint
    private lateinit var mAlphaTextPaint: Paint
    private lateinit var mBorderPaint: Paint

    private var mValShader: Shader? = null
    private var mSatShader: Shader? = null
    private var mHueShader: Shader? = null
    private var mAlphaShader: Shader? = null

    private var mAlpha = 0xff
    private var mHue = 360f
    private var mSat = 0f
    private var mVal = 0f

    private var mAlphaSliderText = ""
    private var mSliderTrackerColor = 0xff1c1c1c.toInt()
    private var mBorderColor = 0xff6e6e6e.toInt()
    private var mShowAlphaPanel = false

    private var mLastTouchedPanel = PANEL_SAT_VAL

    private var mDrawingOffset = 0f

    private var mDrawingRect: RectF? = null
    private var mSatValRect: RectF? = null
    private var mHueRect: RectF? = null
    private var mAlphaRect: RectF? = null

    private var mAlphaPattern: AlphaPatternDrawable? = null

    private var mStartTouchPoint: Point? = null

    private var huePanelWidth = 30f
    private var alphaPanelHeight = 20f
    private var panelSpacing = 10f
    private var paletteCircleTrackerRadius = 5f
    private var rectangleTrackerOffset = 2f

    init {
        init()
    }

    private fun init() {
        mDensity = context.resources.displayMetrics.density
        paletteCircleTrackerRadius *= mDensity
        rectangleTrackerOffset *= mDensity
        huePanelWidth *= mDensity
        alphaPanelHeight *= mDensity
        panelSpacing *= mDensity

        mDrawingOffset = calculateRequiredOffset()

        initPaintTools()

        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun initPaintTools() {
        mSatValPaint = Paint()
        mSatValTrackerPaint = Paint()
        mHuePaint = Paint()
        mHueTrackerPaint = Paint()
        mAlphaPaint = Paint()
        mAlphaTextPaint = Paint()
        mBorderPaint = Paint()

        mSatValTrackerPaint.style = Paint.Style.STROKE
        mSatValTrackerPaint.strokeWidth = 2f * mDensity
        mSatValTrackerPaint.isAntiAlias = true

        mHueTrackerPaint.color = mSliderTrackerColor
        mHueTrackerPaint.style = Paint.Style.STROKE
        mHueTrackerPaint.strokeWidth = 2f * mDensity
        mHueTrackerPaint.isAntiAlias = true

        mAlphaTextPaint.color = 0xff1c1c1c.toInt()
        mAlphaTextPaint.textSize = 14f * mDensity
        mAlphaTextPaint.isAntiAlias = true
        mAlphaTextPaint.textAlign = Paint.Align.CENTER
        mAlphaTextPaint.isFakeBoldText = true
    }

    private fun calculateRequiredOffset(): Float {
        var offset = Math.max(paletteCircleTrackerRadius, rectangleTrackerOffset)
        offset = Math.max(offset, BORDER_WIDTH_PX * mDensity)
        return offset * 1.5f
    }

    private fun buildHueColorArray(): IntArray {
        val hue = IntArray(361)
        var count = 0
        for (i in hue.size - 1 downTo 0) {
            hue[count] = Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
            count++
        }
        return hue
    }

    override fun onDraw(canvas: Canvas) {
        if (mDrawingRect!!.width() <= 0 || mDrawingRect!!.height() <= 0) return

        drawSatValPanel(canvas)
        drawHuePanel(canvas)
        drawAlphaPanel(canvas)
    }

    private fun drawSatValPanel(canvas: Canvas) {
        val rect = mSatValRect!!

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(mDrawingRect!!.left, mDrawingRect!!.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, mBorderPaint)
        }

        if (mValShader == null) {
            mValShader = LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                0xffffffff.toInt(), 0xff000000.toInt(), Shader.TileMode.CLAMP)
        }

        val rgb = Color.HSVToColor(floatArrayOf(mHue, 1f, 1f))

        mSatShader = LinearGradient(rect.left, rect.top, rect.right, rect.top,
            0xffffffff.toInt(), rgb, Shader.TileMode.CLAMP)
        val mShader = ComposeShader(mValShader!!, mSatShader!!, PorterDuff.Mode.MULTIPLY)
        mSatValPaint.shader = mShader

        canvas.drawRect(rect, mSatValPaint)

        val p = satValToPoint(mSat, mVal)

        mSatValTrackerPaint.color = 0xff000000.toInt()
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), paletteCircleTrackerRadius - 1f * mDensity, mSatValTrackerPaint)

        mSatValTrackerPaint.color = 0xffdddddd.toInt()
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), paletteCircleTrackerRadius, mSatValTrackerPaint)
    }

    private fun drawHuePanel(canvas: Canvas) {
        val rect = mHueRect!!

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(rect.left - BORDER_WIDTH_PX,
                rect.top - BORDER_WIDTH_PX,
                rect.right + BORDER_WIDTH_PX,
                rect.bottom + BORDER_WIDTH_PX,
                mBorderPaint)
        }

        if (mHueShader == null) {
            mHueShader = LinearGradient(rect.left, rect.top, rect.left, rect.bottom, buildHueColorArray(), null, Shader.TileMode.CLAMP)
            mHuePaint.shader = mHueShader
        }

        canvas.drawRect(rect, mHuePaint)

        val rectHeight = 4 * mDensity / 2

        val p = hueToPoint(mHue)

        val r = RectF()
        r.left = rect.left - rectangleTrackerOffset
        r.right = rect.right + rectangleTrackerOffset
        r.top = p.y - rectHeight
        r.bottom = p.y + rectHeight

        canvas.drawRoundRect(r, 2f, 2f, mHueTrackerPaint)
    }

    private fun drawAlphaPanel(canvas: Canvas) {
        if (!mShowAlphaPanel || mAlphaRect == null || mAlphaPattern == null) return

        val rect = mAlphaRect!!

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(rect.left - BORDER_WIDTH_PX,
                rect.top - BORDER_WIDTH_PX,
                rect.right + BORDER_WIDTH_PX,
                rect.bottom + BORDER_WIDTH_PX,
                mBorderPaint)
        }

        mAlphaPattern!!.draw(canvas)

        val hsv = floatArrayOf(mHue, mSat, mVal)
        val color = Color.HSVToColor(hsv)
        val acolor = Color.HSVToColor(0, hsv)

        mAlphaShader = LinearGradient(rect.left, rect.top, rect.right, rect.top,
            color, acolor, Shader.TileMode.CLAMP)

        mAlphaPaint.shader = mAlphaShader

        canvas.drawRect(rect, mAlphaPaint)

        if (mAlphaSliderText.isNotEmpty() && mAlphaSliderText != "") {
            canvas.drawText(mAlphaSliderText, rect.centerX(), rect.centerY() + 4 * mDensity, mAlphaTextPaint)
        }

        val rectWidth = 4 * mDensity / 2

        val p = alphaToPoint(mAlpha)

        val r = RectF()
        r.left = p.x - rectWidth
        r.right = p.x + rectWidth
        r.top = rect.top - rectangleTrackerOffset
        r.bottom = rect.bottom + rectangleTrackerOffset

        canvas.drawRoundRect(r, 2f, 2f, mHueTrackerPaint)
    }

    private fun hueToPoint(hue: Float): Point {
        val rect = mHueRect!!
        val height = rect.height()

        val p = Point()
        p.y = (height - (hue * height / 360f) + rect.top).toInt()
        p.x = rect.left.toInt()
        return p
    }

    private fun satValToPoint(sat: Float, `val`: Float): Point {
        val rect = mSatValRect!!
        val height = rect.height()
        val width = rect.width()

        val p = Point()
        p.x = (sat * width + rect.left).toInt()
        p.y = ((1f - `val`) * height + rect.top).toInt()
        return p
    }

    private fun alphaToPoint(alpha: Int): Point {
        val rect = mAlphaRect!!
        val width = rect.width()

        val p = Point()
        p.x = (width - (alpha * width / 0xff.toFloat()) + rect.left).toInt()
        p.y = rect.top.toInt()
        return p
    }

    private fun pointToSatVal(x: Float, y: Float): FloatArray {
        val rect = mSatValRect!!
        val result = FloatArray(2)

        val width = rect.width()
        val height = rect.height()

        val x1 = when {
            x < rect.left -> 0f
            x > rect.right -> width
            else -> x - rect.left
        }

        val y1 = when {
            y < rect.top -> 0f
            y > rect.bottom -> height
            else -> y - rect.top
        }

        result[0] = 1f / width * x1
        result[1] = 1f - (1f / height * y1)

        return result
    }

    private fun pointToHue(y: Float): Float {
        val rect = mHueRect!!
        val height = rect.height()

        val y1 = when {
            y < rect.top -> 0f
            y > rect.bottom -> height
            else -> y - rect.top
        }

        return 360f - (y1 * 360f / height)
    }

    private fun pointToAlpha(x: Int): Int {
        val rect = mAlphaRect!!
        val width = rect.width().toInt()

        val x1 = when {
            x < rect.left -> 0
            x > rect.right -> width
            else -> x - rect.left.toInt()
        }

        return 0xff - (x1 * 0xff / width)
    }

    override fun onTrackballEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        var update = false

        if (event.action == MotionEvent.ACTION_MOVE) {
            when (mLastTouchedPanel) {
                PANEL_SAT_VAL -> {
                    var sat = mSat + x / 50f
                    var `val` = mVal - y / 50f

                    sat = sat.coerceIn(0f, 1f)
                    `val` = `val`.coerceIn(0f, 1f)

                    mSat = sat
                    mVal = `val`

                    update = true
                }
                PANEL_HUE -> {
                    var hue = mHue - y * 10f
                    hue = hue.coerceIn(0f, 360f)
                    mHue = hue
                    update = true
                }
                PANEL_ALPHA -> {
                    if (!mShowAlphaPanel || mAlphaRect == null) {
                        update = false
                    } else {
                        var alpha = (mAlpha - x * 10).toInt()
                        alpha = alpha.coerceIn(0, 0xff)
                        mAlpha = alpha
                        update = true
                    }
                }
            }
        }

        if (update) {
            mListener?.onColorChanged(Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal)))
            invalidate()
            return true
        }

        return super.onTrackballEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var update = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartTouchPoint = Point(event.x.toInt(), event.y.toInt())
                update = moveTrackersIfNeeded(event)
            }
            MotionEvent.ACTION_MOVE -> {
                update = moveTrackersIfNeeded(event)
            }
            MotionEvent.ACTION_UP -> {
                mStartTouchPoint = null
                update = moveTrackersIfNeeded(event)
            }
        }

        if (update) {
            mListener?.onColorChanged(Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal)))
            invalidate()
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun moveTrackersIfNeeded(event: MotionEvent): Boolean {
        if (mStartTouchPoint == null) return false

        var update = false

        val startX = mStartTouchPoint!!.x
        val startY = mStartTouchPoint!!.y

        if (mHueRect!!.contains(startX.toFloat(), startY.toFloat())) {
            mLastTouchedPanel = PANEL_HUE
            mHue = pointToHue(event.y)
            update = true
        } else if (mSatValRect!!.contains(startX.toFloat(), startY.toFloat())) {
            mLastTouchedPanel = PANEL_SAT_VAL
            val result = pointToSatVal(event.x, event.y)
            mSat = result[0]
            mVal = result[1]
            update = true
        } else if (mAlphaRect != null && mAlphaRect!!.contains(startX.toFloat(), startY.toFloat())) {
            mLastTouchedPanel = PANEL_ALPHA
            mAlpha = pointToAlpha(event.x.toInt())
            update = true
        }

        return update
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 0
        var height = 0

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var widthAllowed = MeasureSpec.getSize(widthMeasureSpec)
        var heightAllowed = MeasureSpec.getSize(heightMeasureSpec)

        widthAllowed = chooseWidth(widthMode, widthAllowed)
        heightAllowed = chooseHeight(heightMode, heightAllowed)

        if (!mShowAlphaPanel) {
            height = (widthAllowed - panelSpacing - huePanelWidth).toInt()

            if (height > heightAllowed || tag == "landscape") {
                height = heightAllowed
                width = (height + panelSpacing + huePanelWidth).toInt()
            } else {
                width = widthAllowed
            }
        } else {
            width = (heightAllowed - alphaPanelHeight + huePanelWidth).toInt()

            if (width > widthAllowed) {
                width = widthAllowed
                height = (widthAllowed - huePanelWidth + alphaPanelHeight).toInt()
            } else {
                height = heightAllowed
            }
        }

        setMeasuredDimension(width, height)
    }

    private fun chooseWidth(mode: Int, size: Int): Int {
        return if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            size
        } else {
            getPrefferedWidth()
        }
    }

    private fun chooseHeight(mode: Int, size: Int): Int {
        return if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            size
        } else {
            getPrefferedHeight()
        }
    }

    private fun getPrefferedWidth(): Int {
        var width = getPrefferedHeight()
        if (mShowAlphaPanel) {
            width -= (panelSpacing + alphaPanelHeight).toInt()
        }
        return (width + huePanelWidth + panelSpacing).toInt()
    }

    private fun getPrefferedHeight(): Int {
        var height = (200 * mDensity).toInt()
        if (mShowAlphaPanel) {
            height += (panelSpacing + alphaPanelHeight).toInt()
        }
        return height
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mDrawingRect = RectF().apply {
            left = mDrawingOffset + paddingLeft
            right = w - mDrawingOffset - paddingRight
            top = mDrawingOffset + paddingTop
            bottom = h - mDrawingOffset - paddingBottom
        }

        setUpSatValRect()
        setUpHueRect()
        setUpAlphaRect()
    }

    private fun setUpSatValRect() {
        val dRect = mDrawingRect!!
        var panelSide = dRect.height() - BORDER_WIDTH_PX * 2

        if (mShowAlphaPanel) {
            panelSide -= panelSpacing + alphaPanelHeight
        }

        val left = dRect.left + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = top + panelSide
        val right = left + panelSide

        mSatValRect = RectF(left, top, right, bottom)
    }

    private fun setUpHueRect() {
        val dRect = mDrawingRect!!

        val left = dRect.right - huePanelWidth + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX - (if (mShowAlphaPanel) panelSpacing + alphaPanelHeight else 0f)
        val right = dRect.right - BORDER_WIDTH_PX

        mHueRect = RectF(left, top, right, bottom)
    }

    private fun setUpAlphaRect() {
        if (!mShowAlphaPanel) return

        val dRect = mDrawingRect!!

        val left = dRect.left + BORDER_WIDTH_PX
        val top = dRect.bottom - alphaPanelHeight + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX
        val right = dRect.right - BORDER_WIDTH_PX

        mAlphaRect = RectF(left, top, right, bottom)

        mAlphaPattern = AlphaPatternDrawable((5 * mDensity).toInt())
        mAlphaPattern!!.setBounds(
            Math.round(mAlphaRect!!.left),
            Math.round(mAlphaRect!!.top),
            Math.round(mAlphaRect!!.right),
            Math.round(mAlphaRect!!.bottom)
        )
    }

    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        mListener = listener
    }

    fun setBorderColor(color: Int) {
        mBorderColor = color
        invalidate()
    }

    fun getBorderColor(): Int = mBorderColor

    fun getColor(): Int = Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal))

    fun setColor(color: Int) {
        setColor(color, false)
    }

    fun setColor(color: Int, callback: Boolean) {
        val alpha = Color.alpha(color)

        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        mAlpha = alpha
        mHue = hsv[0]
        mSat = hsv[1]
        mVal = hsv[2]

        if (callback && mListener != null) {
            mListener!!.onColorChanged(Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal)))
        }

        invalidate()
    }

    fun getDrawingOffset(): Float = mDrawingOffset

    fun setAlphaSliderVisible(visible: Boolean) {
        if (mShowAlphaPanel != visible) {
            mShowAlphaPanel = visible
            mValShader = null
            mSatShader = null
            mHueShader = null
            mAlphaShader = null
            requestLayout()
        }
    }

    fun getAlphaSliderVisible(): Boolean = mShowAlphaPanel

    fun setSliderTrackerColor(color: Int) {
        mSliderTrackerColor = color
        mHueTrackerPaint.color = mSliderTrackerColor
        invalidate()
    }

    fun getSliderTrackerColor(): Int = mSliderTrackerColor

    fun setAlphaSliderText(res: Int) {
        val text = context.getString(res)
        setAlphaSliderText(text)
    }

    fun setAlphaSliderText(text: String) {
        mAlphaSliderText = text
        invalidate()
    }

    fun getAlphaSliderText(): String = mAlphaSliderText

    companion object {
        private const val PANEL_SAT_VAL = 0
        private const val PANEL_HUE = 1
        private const val PANEL_ALPHA = 2
        private const val BORDER_WIDTH_PX = 1f
    }
}
