package net.margaritov.preference.colorpicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

class AlphaPatternDrawable(private var mRectangleSize: Int) : Drawable() {

    private val mPaint = Paint()
    private val mPaintWhite = Paint()
    private val mPaintGray = Paint()

    private var numRectanglesHorizontal = 0
    private var numRectanglesVertical = 0

    private var mBitmap: Bitmap? = null

    init {
        mPaintWhite.color = 0xffffffff.toInt()
        mPaintGray.color = 0xffcbcbcb.toInt()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(mBitmap!!, null, bounds, mPaint)
    }

    override fun getOpacity(): Int = PixelFormat.UNKNOWN

    override fun setAlpha(alpha: Int) {
        throw UnsupportedOperationException("Alpha is not supported by this drawwable.")
    }

    override fun setColorFilter(cf: ColorFilter?) {
        throw UnsupportedOperationException("ColorFilter is not supported by this drawwable.")
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)

        val height = bounds.height()
        val width = bounds.width()

        numRectanglesHorizontal = Math.ceil((width.toDouble() / mRectangleSize)).toInt()
        numRectanglesVertical = Math.ceil(height.toDouble() / mRectangleSize).toInt()

        generatePatternBitmap()
    }

    private fun generatePatternBitmap() {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return
        }

        mBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mBitmap!!)

        val r = Rect()
        var verticalStartWhite = true
        for (i in 0..numRectanglesVertical) {
            var isWhite = verticalStartWhite
            for (j in 0..numRectanglesHorizontal) {
                r.top = i * mRectangleSize
                r.left = j * mRectangleSize
                r.bottom = r.top + mRectangleSize
                r.right = r.left + mRectangleSize

                canvas.drawRect(r, if (isWhite) mPaintWhite else mPaintGray)
                isWhite = !isWhite
            }
            verticalStartWhite = !verticalStartWhite
        }
    }
}
