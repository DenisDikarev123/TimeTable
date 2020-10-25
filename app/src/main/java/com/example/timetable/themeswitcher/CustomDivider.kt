package com.example.timetable.themeswitcher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class CustomDivider
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr),
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
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
        } else {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }
    }
}