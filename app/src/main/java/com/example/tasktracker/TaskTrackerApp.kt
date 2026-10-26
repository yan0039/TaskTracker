package com.example.tasktracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 自定义 Application。
 *
 * Hilt 初始化：只需一个 @HiltAndroidApp 注解。
 * 编译期，Hilt 会为它生成基类 Hilt_TaskTrackerApp 并接管依赖图根节点，
 * 因此无需手写任何 DataStore / Database 初始化代码——
 * 这些单例的创建统一由 AppModule 声明，Hilt 在首次需要时才懒加载创建。
 */
@HiltAndroidApp
class TaskTrackerApp : Application()
