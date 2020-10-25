package com.example.timetable.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.timetable.ClassState
import com.example.timetable.helpers.CalendarHelper
import com.example.timetable.MainActivity
import com.example.timetable.R
import com.example.timetable.database.Class
import com.example.timetable.database.ClassDatabase
import com.example.timetable.helpers.DateFormatter
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.IllegalArgumentException


class TimetableWidgetProvider: AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        appWidgetIds?.forEach { widgetId ->

            //intent for click events
            val pendingIntent = Intent(context, MainActivity::class.java).let{ intent ->
                //don't use 0 for request code if have multiple pending intents
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            //intent for updating data
            val serviceIntent = Intent(context, TimetableWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            //intent for collection items click
            val collectionIntent = Intent(context, TimetableWidgetProvider::class.java).run {
                action = COLLECTION_CLICK_ACTION
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

                PendingIntent.getBroadcast(context, 0, this, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            val views =
                RemoteViews(context?.packageName, R.layout.widget_timetable_layout).apply {
                    setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)
                    setPendingIntentTemplate(R.id.widget_list_view, collectionIntent)
                    setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                    setEmptyView(R.id.widget_list_view, R.id.empty_widget_text)

                    val calendar = Calendar.getInstance()
                    val stringDate = DateFormatter.convertCalendarToString(calendar)

                    val classDao = context?.let { ClassDatabase.getInstance(it).classDao() }
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val dayOfWeekName = CalendarHelper.getDayOfWeekName(dayOfWeek)
                    val weekNum = CalendarHelper.calculateCurrentWeek(calendar)

                    runBlocking {
                        val classListForDay =
                            classDao?.getClassesForDay(dayOfWeekName, weekNum)!!
                        val numClassesString = context.resources.getQuantityString(
                            R.plurals.number_of_classes,
                            classListForDay.size,
                            classListForDay.size
                        )
                        val noClassesString = context.getString(R.string.message_no_classes)
                        if (classListForDay.size == 0) {
                            setViewVisibility(R.id.widget_image_weekend, View.VISIBLE)
                            setTextViewText(R.id.widget_header_class_number, noClassesString)
                        } else {
                            setViewVisibility(R.id.widget_image_weekend, View.GONE)
                            setTextViewText(R.id.widget_header_class_number, numClassesString)
                        }
                        //setup AlarmManager for new update
                        setNewUpdateTime(context, classListForDay)
                    }
                    setTextViewText(R.id.widget_header_date, stringDate)
                }
            appWidgetManager?.updateAppWidget(appWidgetIds, views)
            appWidgetManager?.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list_view)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if(intent?.action == COLLECTION_CLICK_ACTION) {
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            context?.startActivity(activityIntent)
        }
    }

    private fun setNewUpdateTime(context: Context, classListForDay: MutableList<Class>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TimetableWidgetProvider::class.java))
        val intent = Intent(context, TimetableWidgetProvider::class.java).let {
            it.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            it.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            PendingIntent.getBroadcast(context, 2, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val newUpdateTime = calculateNewUpdateTime(classListForDay)

        alarmManager.set(AlarmManager.RTC, newUpdateTime.timeInMillis, intent)
    }

    private fun calculateNewUpdateTime(classListForDay: MutableList<Class>): Calendar {
        val calendar = Calendar.getInstance()
        var classesBeforeNum = 0
        var classesNowIndex: Int? = null
        var classesAfterIndex: Int? = null

        classListForDay.forEachIndexed { index, clazz ->
            if(clazz.startTime != null && clazz.endTime != null) {
                val classState = CalendarHelper.checkClassState(clazz.startTime, clazz.endTime, calendar)
                when (classState) {
                    ClassState.BEFORE -> classesBeforeNum++
                    ClassState.NOW -> classesNowIndex = index
                    ClassState.AFTER -> {
                        //we need only index of first classes that will be today
                        if(classesAfterIndex == null) classesAfterIndex = index
                    }
                    ClassState.NOT_TODAY -> {}
                }
            }
        }
        when {
            //if there are no classes today then next update will be at midnight next day + 5sec
            classListForDay.size == 0 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 5)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            //if all classes for today have ended, than next update will be at 00:00
            classListForDay.size == classesBeforeNum -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 5)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            //if there is class which is going, than next update will be at end time of this class + 5sec
            classesNowIndex != null -> {
                //classesNowNum hold index of going class in list
                val clazz = classListForDay[classesNowIndex!!]
                val indexEndTime  = clazz.endTime!!.indexOf('-')
                val endHour = clazz.endTime.substring(0 until indexEndTime).toInt()
                val endMinute = clazz.endTime.substring(indexEndTime + 1).toInt()
                calendar.set(Calendar.HOUR_OF_DAY, endHour)
                calendar.set(Calendar.MINUTE, endMinute)
                calendar.set(Calendar.SECOND, 5)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            //if there is class for today, that next update will be at start time of this class + 5sec
            classesAfterIndex != null -> {
                //classesAfterIndex hold index of next class for today
                val clazz = classListForDay[classesAfterIndex!!]
                val indexStartTime  = clazz.startTime!!.indexOf('-')
                val startHour = clazz.startTime.substring(0 until indexStartTime).toInt()
                val startMinute = clazz.startTime.substring(indexStartTime + 1).toInt()
                calendar.set(Calendar.HOUR_OF_DAY, startHour)
                calendar.set(Calendar.MINUTE, startMinute)
                calendar.set(Calendar.SECOND, 5)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            else -> throw IllegalArgumentException("no one of conditions is true")
        }
        return calendar
    }

    companion object {
        private const val COLLECTION_CLICK_ACTION = "com.example.timetable.widget.COLLECTION_ACTION"
    }
}
