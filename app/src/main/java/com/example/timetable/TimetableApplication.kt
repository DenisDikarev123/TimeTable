package com.example.timetable

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.timetable.themeswitcher.ThemeManager

class TimetableApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkThemeEnabled =
            preferences.getBoolean(getString(R.string.night_theme_flag), true)
        ThemeManager.theme = isDarkThemeEnabled
    }
}