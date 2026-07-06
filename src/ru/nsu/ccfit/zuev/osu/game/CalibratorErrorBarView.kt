package ru.nsu.ccfit.zuev.osu.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import ru.nsu.ccfit.zuev.osuplus.R

class CalibratorErrorBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val tapErrors = mutableListOf<Float>()
    private val maxDisplayRange = 200f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#252540")
        style = Paint.Style.FILL
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f
    }

    private val zone300Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 70, 180, 220)
        style = Paint.Style.FILL
    }

    private val zone100Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 100, 220, 40)
        style = Paint.Style.FILL
    }

    private val zone50Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 200, 180, 110)
        style = Paint.Style.FILL
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E63E8C")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9999BB")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    fun addError(ms: Float) {
        tapErrors.add(ms)
        invalidate()
    }

    fun clear() {
        tapErrors.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f
        val cornerRadius = 8f

        // Background bar
        canvas.drawRoundRect(RectF(0f, 0f, w, h), cornerRadius, cornerRadius, bgPaint)

        val halfWidth = w / 2f
        val usableWidth = w - 32f
        val left = 16f

        // Color zones
        val range300 = (30f / maxDisplayRange) * (usableWidth / 2f)
        val range100 = (100f / maxDisplayRange) * (usableWidth / 2f)
        val range50 = (200f / maxDisplayRange) * (usableWidth / 2f)

        // Zone 50 (outermost)
        canvas.drawRect(left + halfWidth - range50, 4f, left + halfWidth + range50, h - 4f, zone50Paint)
        // Zone 100
        canvas.drawRect(left + halfWidth - range100, 4f, left + halfWidth + range100, h - 4f, zone100Paint)
        // Zone 300 (innermost)
        canvas.drawRect(left + halfWidth - range300, 4f, left + halfWidth + range300, h - 4f, zone300Paint)

        // Center line
        canvas.drawLine(left + halfWidth, 2f, left + halfWidth, h - 2f, centerPaint)

        // Labels
        val labelY = h - 6f
        canvas.drawText("0", left + halfWidth, labelY, labelPaint)
        canvas.drawText("-${maxDisplayRange.toInt()}", left + 24f, labelY, labelPaint)
        canvas.drawText("+${maxDisplayRange.toInt()}", left + usableWidth, labelY, labelPaint)

        // Tap markers
        if (tapErrors.isNotEmpty()) {
            val dp = resources.displayMetrics.density
            val markerRadius = 5f * dp

            for (i in tapErrors.indices) {
                val error = tapErrors[i]
                val x = left + halfWidth + (error / maxDisplayRange) * (usableWidth / 2f)
                val alpha = (128 + 127 * i / tapErrors.size).coerceIn(80, 255)
                markerPaint.alpha = alpha
                canvas.drawCircle(x, centerY - 4f * dp, markerRadius, markerPaint)
            }
        }
    }
}
