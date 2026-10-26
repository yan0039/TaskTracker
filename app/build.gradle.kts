// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // 必须，否则 @Composable 编译不过
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.ksp)              // Room 使用 KSP；Hilt 使用 kapt
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.tasktracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.portfolio.internshiptracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true          // 开启 Compose
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // —— 基础 ——
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // —— Compose（BOM 统一版本，后续 compose 库全部不带版本号）——
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    // ★ Navigation for Compose 不在 BOM 内，必须显式写版本
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // ★ collectAsStateWithLifecycle 在 lifecycle-runtime-compose 中（2.8 起独立成库）
    implementation(libs.androidx.lifecycle.runtime.compose)

    // —— Room ——
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)             // 提供 suspend / Flow 支持
    ksp(libs.room.compiler)                   // 注解处理器用 ksp 而非 kapt

    // —— Hilt ——
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)   // hiltViewModel()

    // —— DataStore（旧版 TaskTracker 源码仍在工程内，当前投递数据只由 Room 管理）——
    implementation(libs.androidx.datastore.preferences)

    // —— 调试工具（仅 debug 构建）——
    debugImplementation(libs.androidx.ui.tooling)

    // —— 测试 ——
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
