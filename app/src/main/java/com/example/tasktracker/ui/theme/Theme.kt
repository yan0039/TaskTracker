package com.example.tasktracker.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.tasktracker.data.preference.ThemeMode

private val LightColors = lightColorScheme(
    primary = Indigo500,
    onPrimary = PureWhite,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Cyan500,
    secondaryContainer = Cyan100,
    onSecondaryContainer = Cyan900,
    background = LightBackground,
    onBackground = Ink,
    surface = LightSurface,
    onSurface = Ink,
    surfaceContainerLow = LightSurfaceContainer
)

private val DarkColors = darkColorScheme(
    primary = Indigo200,
    onPrimary = DarkOnPrimary,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo100,
    secondary = Cyan200,
    secondaryContainer = Cyan900,
    onSecondaryContainer = Cyan100,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainerLow = DarkSurfaceContainer
)

/**
 * App 全局主题。
 *
 * @param themeMode 来自 DataStore 的主题模式，决定是否用深色配色。
 */
@Composable
fun TaskTrackerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    // 只有 SYSTEM 模式才跟随系统，LIGHT / DARK 强制指定
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors

    // 系统栏（状态栏 / 导航栏）跟随 App 主题，而不是系统设置：
    // 导航栏填成主题背景色，图标明暗随 darkTheme 切换，避免深色主题下出现白条。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TaskTrackerTypography,
        content = content
    )
}

/** Compose 的 LocalView.context 可能被 ContextWrapper 包裹，这里向上找到宿主 Activity。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
