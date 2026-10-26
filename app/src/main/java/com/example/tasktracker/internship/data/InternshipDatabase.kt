package com.example.tasktracker.internship.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [JobApplicationEntity::class], version = 1, exportSchema = true)
abstract class InternshipDatabase : RoomDatabase() {
    abstract fun jobApplicationDao(): JobApplicationDao

    companion object {
        const val DB_NAME = "internship_tracker.db"
    }
}
