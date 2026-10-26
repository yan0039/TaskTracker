// build.gradle.kts（工程根目录）
plugins {
    // 只声明、不应用（apply false），具体应用交给子模块
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false   // Compose 编译器插件
    alias(libs.plugins.ksp) apply false              // KSP（Room/Hilt 注解处理）
    alias(libs.plugins.hilt) apply false             // Hilt 插件
}
