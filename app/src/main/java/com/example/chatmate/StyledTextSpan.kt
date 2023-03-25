package com.example.chatmate

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan

class StyledTextSpan(
    private val newType: Typeface,
    private val textSize: Float,
    private val padding: Float,
) : MetricAffectingSpan(), LeadingMarginSpan {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, newType)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newType)
    }

    private fun applyCustomTypeFace(paint: Paint, newTypeface: Typeface) {
        val oldTypeface: Typeface = paint.typeface
        val oldStyle = oldTypeface.style
        val fakeStyle = oldStyle and newTypeface.style.inv()

        if (fakeStyle and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }

        if (fakeStyle and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }

        paint.typeface = newTypeface
        paint.textSize = textSize // Sets the text size
    }

    override fun getLeadingMargin(first: Boolean): Int {
        return padding.toInt() // Sets the indentation (padding)
    }

    override fun drawLeadingMargin(
        p0: Canvas?,
        p1: Paint?,
        p2: Int,
        p3: Int,
        p4: Int,
        p5: Int,
        p6: Int,
        p7: CharSequence?,
        p8: Int,
        p9: Int,
        p10: Boolean,
        p11: Layout?,
    ) {
    }
}
