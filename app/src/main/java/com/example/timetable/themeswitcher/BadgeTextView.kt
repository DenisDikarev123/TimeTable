package com.example.timetable.themeswitcher

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.example.timetable.R

class BadgeTextView
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
        setBackgroundByTheme(ThemeManager.theme)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ThemeManager.addListener(this)
        setBackgroundByTheme(ThemeManager.theme)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ThemeManager.removeListener(this)
    }

    override fun onThemeChanged(isNightMode: Boolean) {
        setBackgroundByTheme(isNightMode)
    }

    private fun setBackgroundByTheme(isNightMode: Boolean) {
        background = if (isNightMode) {
            resources.getDrawable(R.drawable.background_badge, null)
        } else {
            resources.getDrawable(R.drawable.background_badge_night, null)
        }
    }
}