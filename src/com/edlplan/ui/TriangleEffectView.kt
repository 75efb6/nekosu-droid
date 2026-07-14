package com.edlplan.ui

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.View

class TriangleEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var triangleDrawable: TriangleDrawable

    init {
        triangleDrawable = if (attrs != null) createDrawable(attrs) else TriangleDrawable()
        background = triangleDrawable
    }

    protected fun createDrawable(attrs: AttributeSet): TriangleDrawable {
        var preSpawnTriangles = true
        preSpawnTriangles = attrs.getAttributeBooleanValue(NAMESPACE, "preSpawnTriangles", true)
        val triangle = TriangleDrawable(preSpawnTriangles)
        if (attrs.getAttributeBooleanValue(NAMESPACE, "freeze", false)) {
            triangle.setFreeze(true)
        }
        triangle.setEdgeClampRate(attrs.getAttributeFloatValue(NAMESPACE, "edgeClampRate", triangle.getEdgeClampRate()))
        return triangle
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        invalidate()
    }

    fun setXDistribution(xDistribution: TriangleDrawable.PosXDistribution) {
        triangleDrawable.setXDistribution(xDistribution)
    }

    companion object {
        private const val NAMESPACE = "http://ui.edlplan.com/customview"
    }
}
