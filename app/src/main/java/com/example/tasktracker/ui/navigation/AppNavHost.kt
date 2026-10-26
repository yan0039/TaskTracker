package com.example.tasktracker.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tasktracker.ui.task.AddTaskSheet
import com.example.tasktracker.ui.task.TaskListScreen
import com.example.tasktracker.ui.task.TaskViewModel
import com.example.tasktracker.ui.theme.TaskTrackerTheme
import kotlinx.coroutines.launch

/**
 * 应用导航宿主。
 *
 * 结构：
 * - 外层 TaskTrackerTheme：用 DataStore 里的主题模式包裹整个 App。
 * - Scaffold + SnackbarHost：全局 Snackbar 容器，任意页面的提示都走这里。
 * - NavHost：两个目的地 —— 任务列表页、添加任务页。
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // Snackbar 宿主与协程作用域（用于触发显示）
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 主题来源：顶层 ViewModel（与列表页共享同一个 TaskViewModel 实例）
    val rootViewModel: TaskViewModel = hiltViewModel()
    val uiState by rootViewModel.uiState.collectAsStateWithLifecycle()

    // 统一的提示函数
    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()  // 避免提示排队
            snackbarHostState.showSnackbar(msg)
        }
    }

    // ★ 主题包裹：DataStore 里的主题模式决定深色 / 浅色
    TaskTrackerTheme(themeMode = uiState.themeMode) {
        Scaffold(
            snackbarHost = {
                // 底部留出让位给列表页 FAB 的空间，避免 Snackbar 盖住「+」按钮
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp)
                )
            },
            // 系统栏内边距由各页面的 Scaffold 自己处理，这里只做 Snackbar 容器
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.TASK_LIST,
                modifier = Modifier.padding(innerPadding)
            ) {
                // 目的地 1：任务列表页
                composable(Routes.TASK_LIST) {
                    TaskListScreen(
                        onAddClick = { navController.navigate(Routes.ADD_TASK) },
                        onShowMessage = showMessage
                    )
                }

                // 目的地 2：添加任务页（底部弹窗）
                composable(Routes.ADD_TASK) {
                    AddTaskSheet(
                        onDismiss = { navController.popBackStack() },
                        onAdded = { title ->
                            navController.popBackStack()
                            showMessage("已添加：$title")
                        }
                    )
                }
            }
        }
    }
}
