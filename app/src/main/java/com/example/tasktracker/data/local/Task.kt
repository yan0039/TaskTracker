package com.example.tasktracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办任务实体，对应数据库中的 task 表。
 *
 * 字段说明：
 * - id        ：主键，自增。autoGenerate = true 时由 Room 自动分配。
 * - title     ：任务标题，非空。
 * - isDone    ：是否完成。Room 支持 Boolean，底层存为 0/1。
 * - createdAt ：创建时间戳（毫秒）。默认取当前系统时间。
 */
@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    val isDone: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)
