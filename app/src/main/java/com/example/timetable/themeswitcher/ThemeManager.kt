package com.example.timetable.themeswitcher

object ThemeManager {

    private val listeners = mutableSetOf<ThemeChangedListener>()
    var theme = false
        set(value) {
            field = value
            listeners.forEach { listener -> listener.onThemeChanged(value) }
        }

    interface ThemeChangedListener {

        fun onThemeChanged(isNightMode: Boolean)
    }

    fun addListener(listener: ThemeChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ThemeChangedListener) {
        listeners.remove(listener)
    }
}