package com.example.timetable.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClassDao {

    @Query("SELECT * FROM class_table")
    fun getAllClasses() : List<Class>

    @Query("SELECT * FROM class_table WHERE dayOfWeek = :dayOfWeek AND classWeek = :weekNum")
    suspend fun getClassesForDay(dayOfWeek: String, weekNum: String): MutableList<Class>

    //use it for widget
    @Query("SELECT * FROM class_table WHERE dayOfWeek = :dayOfWeek AND classWeek = :weekNum")
    fun getClassesForDaySynchronously(dayOfWeek: String, weekNum: String): MutableList<Class>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllClasses(classList: MutableList<Class>)
}