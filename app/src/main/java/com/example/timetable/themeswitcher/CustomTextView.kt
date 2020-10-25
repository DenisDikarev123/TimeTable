package com.example.timetable.themeswitcher

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat

class CustomTextView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr),
    ThemeManager.ThemeChangedListener {

    override fun onFinishInflate() {
        super.onFinishInflate()
        ThemeManager.addListener(this)
        setColorTextByTheme(ThemeManager.theme)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ThemeManager.addListener(this)
        setColorTextByTheme(ThemeManager.theme)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ThemeManager.removeListener(this)
    }

    override fun onThemeChanged(isNightMode: Boolean) {
        setColorTextByTheme(isNightMode)
    }

    private fun setColorTextByTheme(isNightMode: Boolean) {
        if (isNightMode) {
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        } else {
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
    }
}