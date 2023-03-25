package com.example.chatmate

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.style.LeadingMarginSpan

class CodeBlockSpan(
    private val backgroundColor: Int,
    private val cornerRadius: Float,
    private val padding: Float,
    private val leftPadding: Float,
//    private val topPadding: Float
) : LeadingMarginSpan {
    private var lastTop = 0

    override fun getLeadingMargin(first: Boolean): Int = 0

    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?,
    ) {
        if (lastTop != top) {
            val oldColor = paint.color
            paint.color = backgroundColor

            val left = x + leftPadding * dir
            val right = canvas.width * dir
//            val topAdjusted = top.toFloat() - if (first) 0f else topPadding

            val rect = if (first) {
                RectF(left, top.toFloat(), right.toFloat(), (bottom + padding))
            } else {
                RectF(left, top.toFloat() - padding, right.toFloat(), bottom.toFloat())
            }

            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.color = oldColor

            lastTop = top
        }
    }
}
