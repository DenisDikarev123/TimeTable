package com.example.timetable.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.timetable.ClassState
import com.example.timetable.R
import com.example.timetable.database.Class
import com.example.timetable.database.ClassDao
import com.example.timetable.database.ClassDatabase
import com.example.timetable.helpers.CalendarHelper
import kotlinx.coroutines.runBlocking
import java.util.*


class TimetableWidgetService: RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TimetableRemoteViewsFactory(this.applicationContext, intent)
    }
    class TimetableRemoteViewsFactory(private val context: Context, intent: Intent): RemoteViewsFactory {

        private val calendar = Calendar.getInstance()

        private lateinit var classDao: ClassDao
        private lateinit var classListForDay: MutableList<Class>

        override fun onCreate() {
            classDao = ClassDatabase.getInstance(context).classDao()

            Log.d(TAG, "onCreate")
        }

        override fun getLoadingView(): RemoteViews? {
            return RemoteViews(context.packageName, R.layout.widget_timetable_load_item)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun onDataSetChanged() {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayOfWeekName = CalendarHelper.getDayOfWeekName(dayOfWeek)
            val weekNum = CalendarHelper.calculateCurrentWeek(calendar)

            runBlocking {
                classListForDay = classDao.getClassesForDay(dayOfWeekName, weekNum)
            }
            Log.d(TAG, "onDataSetChanged")
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getViewAt(position: Int): RemoteViews {
            val cl = classListForDay[position]
            val classState = cl.startTime?.let { cl.endTime?.let { it1 -> CalendarHelper.checkClassState(it, it1, calendar) } }
            Log.d(TAG, "getViewAt position = $position")
            return RemoteViews(context.packageName, R.layout.widget_timetable_item).apply {
                setTextViewText(R.id.widget_text_class_number, classListForDay[position].number.toString())
                setTextViewText(R.id.widget_text_class_type, classListForDay[position].type)
                setTextViewText(R.id.widget_text_class_name, classListForDay[position].name)
                setTextViewText(R.id.widget_text_class_teacher_name, classListForDay[position].teacherName)
                setTextViewText(R.id.widget_text_class_start_time, cl.startTime?.let { correctTime(it) })
                setTextViewText(R.id.widget_text_class_end_time, cl.endTime?.let { correctTime(it) } )
                setTextViewText(R.id.widget_text_class_audience, classListForDay[position].audience)
                when (classState) {
                    ClassState.NOT_TODAY -> {
                        setViewVisibility(R.id.widget_text_class_countdown, View.GONE)
                    }
                    ClassState.BEFORE -> {
                        setViewVisibility(R.id.widget_text_class_countdown, View.GONE)
                    }
                    ClassState.NOW -> {
                        setViewVisibility(R.id.widget_text_class_countdown, View.VISIBLE)
                    }
                    ClassState.AFTER -> {
                        setViewVisibility(R.id.widget_text_class_countdown, View.GONE)
                    }
                }
                val fillInIntent = Intent()
                setOnClickFillInIntent(R.id.widget_item_root_layout, fillInIntent)
            }
        }

        override fun getCount(): Int {
            return classListForDay.size
        }


        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun onDestroy() {
        }

        private fun correctTime(time: String): String {
            return time.replace('-', ':')
        }

        companion object {
            private const val TAG = "TimetableWidgetService"
        }
    }
}