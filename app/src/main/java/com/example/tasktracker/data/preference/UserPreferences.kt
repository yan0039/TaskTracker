package com.example.tasktracker.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 顶层属性扩展：全进程只允许创建一个 DataStore 实例，写在文件顶层是官方推荐写法。 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"     // 生成文件：files/datastore/user_preferences.preferences_pb
)

/**
 * 用户偏好封装类（首选用法：构造注入 Context，而不是注入 Context 本身）。
 *
 * 设计要点：
 * - Context 是 applicationContext，不持有 Activity 引用，避免内存泄漏。
 * - 读取用 Flow，写入用 suspend，全程异步、不阻塞主线程。
 */
class UserPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_DONE = booleanPreferencesKey("show_done_tasks")
    }

    /** 主题模式，默认跟随系统。UI 侧 collectAsStateWithLifecycle() 订阅。 */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromName(prefs[Keys.THEME_MODE])
    }

    /** 是否显示已完成任务，默认显示。 */
    val showDoneTasks: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_DONE] ?: true
    }

    /** 写入主题模式。 */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    /** 写入「是否显示已完成任务」。 */
    suspend fun setShowDoneTasks(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_DONE] = show
        }
    }
}
