package com.example.tasktracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 任务表数据访问接口。Room 在编译期生成实现类 TaskDao_Impl。
 *
 * 异步查询说明：
 * - 返回 Flow<List<Task>> 的查询是「可观察查询」，数据表变化时自动重新发射。
 * - 增删改统一用 suspend，由 Room 自动切到 IO 线程，不阻塞主线程。
 */
@Dao
interface TaskDao {

    /** 插入一条任务；返回新行的主键 id。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    /** 查询全部任务，按创建时间倒序（最新的在最上面）。 */
    @Query("SELECT * FROM task ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    /** 只查询未完成任务，同样按创建时间倒序。 */
    @Query("SELECT * FROM task WHERE isDone = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<Task>>

    /** 更新任务（按主键匹配），常用于切换完成状态或改名。 */
    @Update
    suspend fun update(task: Task)

    /** 删除任务（按主键匹配）。 */
    @Delete
    suspend fun delete(task: Task)

    /** 按 id 删除，便于从 UI 只拿到 id 的场景。 */
    @Query("DELETE FROM task WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
}
