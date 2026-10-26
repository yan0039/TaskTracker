package com.example.tasktracker.data.repository

import com.example.tasktracker.data.local.Task
import com.example.tasktracker.data.local.TaskDao
import com.example.tasktracker.data.preference.ThemeMode
import com.example.tasktracker.data.preference.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 仓库层：App 的「唯一数据入口」。
 *
 * 职责：
 * 1. 对 ViewModel 屏蔽数据来源（Room 还是 DataStore）。
 * 2. 组合多个数据源（任务来自 Room，主题来自 DataStore）。
 * 3. 将来加缓存 / 换网络源，只改这一层，UI 无感知。
 *
 * @Inject constructor：Hilt 构造注入，无需手写工厂。
 */
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val userPreferences: UserPreferences
) {

    // ---------------- 任务相关（Room） ----------------

    /** 观察全部任务。 */
    val allTasks: Flow<List<Task>> = taskDao.observeAll()

    /** 观察未完成任务。 */
    val activeTasks: Flow<List<Task>> = taskDao.observeActive()

    /** 新增任务。 */
    suspend fun addTask(title: String) {
        taskDao.insert(Task(title = title.trim()))
    }

    /** 切换某条任务的完成状态。 */
    suspend fun toggleDone(task: Task) {
        taskDao.update(task.copy(isDone = !task.isDone))
    }

    /** 更新任务（整体替换）。 */
    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }

    /** 删除任务。 */
    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }

    // ---------------- 偏好相关（DataStore） ----------------

    /** 观察主题模式。 */
    val themeMode: Flow<ThemeMode> = userPreferences.themeMode

    /** 观察是否显示已完成任务。 */
    val showDoneTasks: Flow<Boolean> = userPreferences.showDoneTasks

    /** 设置主题模式。 */
    suspend fun setThemeMode(mode: ThemeMode) {
        userPreferences.setThemeMode(mode)
    }

    /** 设置是否显示已完成任务。 */
    suspend fun setShowDoneTasks(show: Boolean) {
        userPreferences.setShowDoneTasks(show)
    }
}
