package com.example.chatmate

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan

class CustomTypefaceSpan(private val newType: Typeface) : TypefaceSpan("") {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, newType)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newType)
    }

    private fun applyCustomTypeFace(paint: Paint, newTypeface: Typeface) {
        val oldTypeface: Typeface = paint.typeface
        val oldStyle = oldTypeface.style
        val fakeStyle = oldStyle.and(newTypeface.style.inv())

        if (fakeStyle and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }

        if (fakeStyle and Typeface.ITALIC != 0) {
            paint.textSkewX = TEXT_SKEW_X
        }

        paint.typeface = newTypeface
    }

    companion object {
        const val TEXT_SKEW_X = -0.25f
    }
}
