package com.example.tasktracker.data.preference

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ThemeMode 的纯 JVM 单元测试：验证持久化字符串与枚举的映射。
 *
 * 运行：./gradlew :app:testDebugUnitTest
 */
class ThemeModeTest {

    @Test
    fun `fromName 正常解析三个合法值`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
    }

    @Test
    fun `fromName 对 null 与非法值兜底为 SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("dark"))   // 大小写敏感
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("NIGHT"))
    }
}
