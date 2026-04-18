package com.example.docuspeak

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class HighlightOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#4433B5E5") // Semi-transparent blue
        style = Paint.Style.FILL
    }

    private val currentHighlights = mutableListOf<RectF>()

    fun setHighlight(rect: RectF?) {
        currentHighlights.clear()
        if (rect != null) {
            currentHighlights.add(rect)
        }
        invalidate()
    }

    fun clear() {
        currentHighlights.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in currentHighlights) {
            canvas.drawRect(rect, paint)
        }
    }
}
