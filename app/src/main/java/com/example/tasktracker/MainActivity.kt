package com.example.tasktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tasktracker.internship.ui.navigation.InternshipNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一 Activity。
 *
 * @AndroidEntryPoint：让 Hilt 能向本 Activity 及其中的 Compose 层注入依赖。
 * 这是 Compose + Hilt 的标准入口配置，无需重写 onCreate 之外的任何方法。
 *
 * 注意：Compose 场景下不需要手写 ComposeView / setContentView，
 * setContent 内部就是往一个 ComposeView 里塞内容，等价但更简洁。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()          // 边到边显示（官方推荐，非废弃 API）
        setContent {
            InternshipNavHost()
        }
    }
}
