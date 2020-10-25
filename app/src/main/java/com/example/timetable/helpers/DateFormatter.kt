package com.example.timetable.helpers

import android.util.Log
import java.util.*

class DateFormatter {

    companion object {
        private const val TAG = "DateFormatter"

        fun convertCalendarToString (calendar: Calendar): String {
            val year = calendar.get(Calendar.YEAR)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("ru"))

            val dateString = "$day $month $year"
            Log.d(TAG, dateString)
            return dateString
        }
    }
}