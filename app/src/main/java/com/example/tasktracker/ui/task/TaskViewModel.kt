package com.example.tasktracker.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasktracker.data.local.Task
import com.example.tasktracker.data.preference.ThemeMode
import com.example.tasktracker.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 列表页 ViewModel。
 *
 * 设计要点：
 * - @HiltViewModel + @Inject constructor：Hilt 自动把 Repository 注入进来。
 * - UI 状态用 StateFlow 暴露，Compose 侧 collectAsStateWithLifecycle() 订阅。
 * - 写操作用 viewModelScope.launch，页面销毁自动取消，不泄漏。
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    /**
     * 列表页完整 UI 状态：任务列表 + 主题模式 + 是否显示已完成。
     * 用 combine 把三个 Flow 合并成一个 StateFlow，UI 只需订阅一个对象。
     */
    val uiState: StateFlow<TaskUiState> = combine(
        repository.allTasks,
        repository.themeMode,
        repository.showDoneTasks
    ) { tasks, theme, showDone ->
        TaskUiState(
            tasks = if (showDone) tasks else tasks.filter { !it.isDone },
            totalCount = tasks.size,
            doneCount = tasks.count { it.isDone },
            themeMode = theme,
            showDoneTasks = showDone
        )
    }.stateIn(
        scope = viewModelScope,
        // 界面订阅前就保持订阅，配置变更（旋屏）时不丢失数据
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState()
    )

    /** 新增任务。 */
    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addTask(title) }
    }

    /** 切换完成状态。 */
    fun toggleDone(task: Task) {
        viewModelScope.launch { repository.toggleDone(task) }
    }

    /** 删除任务。 */
    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    /** 切换主题模式。 */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    /** 切换「是否显示已完成任务」。 */
    fun setShowDoneTasks(show: Boolean) {
        viewModelScope.launch { repository.setShowDoneTasks(show) }
    }
}

/**
 * 列表页 UI 状态（不可变数据类）。
 * 所有提供给 Compose 的状态都应该是不可变的，便于 Compose 做重组优化。
 */
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val totalCount: Int = 0,
    val doneCount: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showDoneTasks: Boolean = true
)
