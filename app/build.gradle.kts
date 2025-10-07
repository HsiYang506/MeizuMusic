plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // 正确地通过别名应用 kapt 和 hilt 插件
    alias(libs.plugins.kotlin.kapt.plugin)
    alias(libs.plugins.hilt.android.plugin)

    id("kotlin-parcelize")
}

android {
    namespace = "com.jywkhyse.meizumusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jywkhyse.meizumusic"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // 使用 TOML 中的别名引用所有依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout) // 添加 SwipeRefreshLayout

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines & Flow
    implementation(libs.kotlinx.coroutines.android)

    // Paging 3
    implementation(libs.androidx.paging.runtime.ktx)

    // Media3 (ExoPlayer & MediaSession)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Coil (Image Loading)
    implementation(libs.coil)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.blurry)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.lrc.view)

    implementation(libs.timber)

    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
// Permitir que Hilt procese las anotaciones
kapt {
    correctErrorTypes = true
}