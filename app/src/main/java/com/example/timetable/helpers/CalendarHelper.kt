package com.example.timetable.helpers

import com.example.timetable.ClassState
import java.util.*

class CalendarHelper {

    companion object {

        fun calculateCurrentWeek(currentDate: Calendar): String {
            //suppose that all classes starts from 1 september
            val firstDay = Calendar.getInstance()
            firstDay.set(2020, 9, 1)
            val currentWeek = currentDate.get(Calendar.WEEK_OF_YEAR)
            val firstWeek = firstDay.get(Calendar.WEEK_OF_YEAR)

            return if(((currentWeek - firstWeek) % 2) == 0) {
                "I"
            } else {
                "II"
            }
        }

        fun getDayOfWeekName(dayOfWeek: Int): String {
            return when(dayOfWeek){
                2 -> "ПОНЕДЕЛЬНИК"
                3 -> "ВТОРНИК"
                4 -> "СРЕДА"
                5 -> "ЧЕТВЕРГ"
                6 -> "ПЯТНИЦА"
                7 -> "СУББОТА"
                1 -> "ВОСКРЕСЕНЬЕ"
                else -> throw IllegalArgumentException()
            }
        }

        fun checkClassState(startClassTime: String, endClassTime: String, selectedDate: Calendar): ClassState {
            //delimiter index for start time
            val indexStartTime  = startClassTime.indexOf('-')
            //delimiter index for end time
            val indexEndTime  = endClassTime.indexOf('-')
            val startHour = startClassTime.substring(0 until indexStartTime).toInt()
            val startMinute = startClassTime.substring(indexStartTime + 1).toInt()
            val endHour = endClassTime.substring(0 until indexEndTime).toInt()
            val endMinute = endClassTime.substring(indexEndTime + 1).toInt()

            val currentCalendar = Calendar.getInstance()
            val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentCalendar.get(Calendar.MINUTE)

            //check if selected day today
            val isToday =
                isSelectedDayToday(
                    selectedDate,
                    currentCalendar
                )
            //return state not today
            if(!isToday) return ClassState.NOT_TODAY

            if((endHour < currentHour) || (endHour == currentHour && endMinute < currentMinute)) {
                //return state before
                return ClassState.BEFORE
            }
            if(currentHour in (startHour + 1) until endHour) {
                //return state now
                return ClassState.NOW
            }
            if (currentHour == startHour && currentMinute >= startMinute) {
                //return state now
                return ClassState.NOW
            }
            if (currentHour == endHour && currentMinute <= endMinute) {
                //return state now
                return ClassState.NOW
            }
            if((startHour > currentHour) || (startHour == currentHour && startMinute > currentMinute)) {
                //return state after
                return ClassState.AFTER
            }
            return throw IllegalArgumentException()
        }

        private fun isSelectedDayToday(selectedDate: Calendar, currentDate: Calendar): Boolean {
            return (selectedDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)
                    && selectedDate.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH)
                    && selectedDate.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH))
        }
    }
}
