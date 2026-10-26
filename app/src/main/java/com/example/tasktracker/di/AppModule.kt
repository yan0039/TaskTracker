package com.example.tasktracker.di

import android.content.Context
import androidx.room.Room
import com.example.tasktracker.data.local.AppDatabase
import com.example.tasktracker.data.local.TaskDao
import com.example.tasktracker.data.preference.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 全局依赖模块。
 *
 * - @InstallIn(SingletonComponent::class)：这些对象随 App 生命周期存活（全局单例）。
 * - @Provides：告诉 Hilt「如何创建这个类型」。
 * - @Singleton：保证同一类型全局只有一个实例。
 *
 * 说明：TaskRepository 用的是 @Inject 构造注入（见 TaskRepository 类），
 * 不需要在 Module 里再写一遍；只有「无法改源码的类」（如 Room、DataStore 封装）
 * 或「需要 Context 才能构造的类」才需要 @Provides。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** 提供 Room 数据库单例。 */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DB_NAME
    ).build()

    /** 提供 TaskDao：从数据库取出。 */
    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    /** 提供 UserPreferences：注意必须传 applicationContext，避免内存泄漏。 */
    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences = UserPreferences(context.applicationContext)
}
