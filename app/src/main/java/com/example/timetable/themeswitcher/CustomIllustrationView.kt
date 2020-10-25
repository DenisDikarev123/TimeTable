package com.example.timetable.themeswitcher

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.timetable.R

class CustomIllustrationView @JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr),
    ThemeManager.ThemeChangedListener {

    override fun onFinishInflate() {
        super.onFinishInflate()
        ThemeManager.addListener(this)
        setIllustrationByTheme(ThemeManager.theme)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ThemeManager.addListener(this)
        setIllustrationByTheme(ThemeManager.theme)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ThemeManager.removeListener(this)
    }

    override fun onThemeChanged(isNightMode: Boolean) {
        setIllustrationByTheme(isNightMode)
    }

    private fun setIllustrationByTheme(isNightMode: Boolean) {
        if (isNightMode) {
            setImageDrawable(resources.getDrawable(R.drawable.illustration_having_fun, null))
        } else {
            setImageDrawable(resources.getDrawable(R.drawable.illustration_having_fun_night, null))
        }
    }
}