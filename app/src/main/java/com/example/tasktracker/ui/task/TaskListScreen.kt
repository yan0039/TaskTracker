package com.example.tasktracker.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tasktracker.data.local.Task
import com.example.tasktracker.data.preference.ThemeMode

/**
 * 任务列表页。
 *
 * @param onAddClick  跳转到添加页（由 NavHost 提供）
 * @param onShowMessage 展示 Snackbar 的事件回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddClick: () -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    // 订阅 StateFlow，跟随生命周期，避免后台无谓重组
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Scaffold 提供 TopAppBar / FAB / Snackbar 的标准位置
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TaskTracker") },
                actions = {
                    // 主题切换开关：开 = 深色，关 = 浅色
                    ThemeSwitch(
                        themeMode = uiState.themeMode,
                        onToggle = { enabled ->
                            viewModel.setThemeMode(
                                if (enabled) ThemeMode.DARK else ThemeMode.LIGHT
                            )
                            onShowMessage(if (enabled) "已切换深色主题" else "已切换浅色主题")
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加任务")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 顶部统计信息
            TaskSummary(
                total = uiState.totalCount,
                done = uiState.doneCount
            )

            // 是否显示已完成任务
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("显示已完成任务", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.showDoneTasks,
                    onCheckedChange = { viewModel.setShowDoneTasks(it) },
                    modifier = Modifier.semantics {
                        contentDescription = "显示已完成任务"
                    }
                )
            }

            HorizontalDivider()

            if (uiState.tasks.isEmpty()) {
                EmptyHint()
            } else {
                // 任务列表：LazyColumn 只渲染可见项，长列表也流畅
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)  // 给 FAB 留位置
                ) {
                    items(
                        items = uiState.tasks,
                        key = { task -> task.id },   // ★ 用 id 做 key，删除时状态不串行
                        contentType = { "task" }
                    ) { task ->
                        TaskRow(
                            task = task,
                            onToggle = {
                                viewModel.toggleDone(task)
                                onShowMessage(
                                    if (!task.isDone) "已完成：${task.title}" else "已取消：${task.title}"
                                )
                            },
                            onDelete = {
                                viewModel.deleteTask(task)
                                onShowMessage("已删除：${task.title}")
                            }
                        )
                    }
                }
            }
        }
    }
}

/** 单条任务行：Checkbox + 标题 + 删除按钮。 */
@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            // 完成后加删除线，视觉反馈明确
            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
            color = if (task.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除任务",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** 主题切换开关（Switch 组件）。 */
@Composable
private fun ThemeSwitch(
    themeMode: ThemeMode,
    onToggle: (Boolean) -> Unit
) {
    Switch(
        checked = themeMode == ThemeMode.DARK,
        onCheckedChange = onToggle,
        modifier = Modifier.semantics { contentDescription = "深色主题" }
    )
}

/** 顶部统计条。 */
@Composable
private fun TaskSummary(total: Int, done: Int) {
    Text(
        text = "共 $total 项，已完成 $done 项",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 空列表提示。 */
@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "还没有任务，点右下角 + 添加一个吧",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
