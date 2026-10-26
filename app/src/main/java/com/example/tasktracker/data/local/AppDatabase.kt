package com.example.tasktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room 数据库抽象类。
 *
 * - entities    ：声明参与建表的实体类。
 * - version     ：数据库版本号；改表结构必须 +1，否则运行时报错。
 * - exportSchema ：关闭 schema 导出可避免未配 schemaDirectory 的告警；
 *                  生产项目建议开启并纳入版本管理（本 Demo 关闭）。
 *
 * 注意：这里只写抽象方法，实例由 Hilt 在 AppModule 中创建。
 */
@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** 暴露 DAO，Repository 通过它访问数据。 */
    abstract fun taskDao(): TaskDao

    companion object {
        /** 数据库文件名常量，供 Module 使用。 */
        const val DB_NAME = "task_tracker.db"
    }
}
