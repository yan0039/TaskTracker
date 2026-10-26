package com.example.tasktracker.data.preference

/**
 * 主题模式枚举。
 * - SYSTEM ：跟随系统
 * - LIGHT  ：强制浅色
 * - DARK   ：强制深色
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** 把持久化的字符串安全地转回枚举，非法值兜底为 SYSTEM。 */
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
