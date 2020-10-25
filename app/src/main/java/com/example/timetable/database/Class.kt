package com.example.timetable.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "class_table")
data class Class(
    @PrimaryKey val id: String,
    val name: String?,
    val number: Int?,
    val type: String,
    val teacherName: String,
    val audience: String,
    val dayOfWeek: String?,
    val startTime: String?,
    val endTime: String?,
    val classWeek: String?
) {
    constructor(
        name: String?,
        number: Int?,
        type: String,
        teacherName: String,
        audience: String,
        dayOfWeek: String?,
        startTime: String?,
        endTime: String?,
        classWeek: String?): this(UUID.randomUUID().toString(), name, number, type, teacherName, audience, dayOfWeek, startTime, endTime, classWeek)
}