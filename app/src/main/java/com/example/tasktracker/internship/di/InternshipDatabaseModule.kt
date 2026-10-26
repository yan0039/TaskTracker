package com.example.tasktracker.internship.di

import android.content.Context
import androidx.room.Room
import com.example.tasktracker.internship.data.InternshipDatabase
import com.example.tasktracker.internship.data.JobApplicationDao
import com.example.tasktracker.internship.util.DeviceLocalClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InternshipDatabaseModule {
    @Provides
    @Singleton
    fun provideInternshipDatabase(@ApplicationContext context: Context): InternshipDatabase =
        Room.databaseBuilder(context, InternshipDatabase::class.java, InternshipDatabase.DB_NAME)
            .build()

    @Provides
    fun provideJobApplicationDao(database: InternshipDatabase): JobApplicationDao =
        database.jobApplicationDao()

    @Provides
    fun provideClock(): Clock = DeviceLocalClock()
}
