package com.example.timetable.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Class::class), version = 1, exportSchema = false)
abstract class ClassDatabase: RoomDatabase() {

    abstract fun classDao() : ClassDao

    companion object {
        @Volatile
        private var INSTANCE: ClassDatabase? = null

        fun getInstance(context: Context): ClassDatabase {
            val tempInstance = INSTANCE
            if(tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(context,
                    ClassDatabase::class.java,
                    "class_database")
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}