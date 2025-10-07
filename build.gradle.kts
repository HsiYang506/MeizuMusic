// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // apply false 表示插件在此处被声明，但只在需要的模块中实际应用
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // 正确添加 Hilt 插件声明
    alias(libs.plugins.hilt.android.plugin) apply false

    // 正确添加 Kapt 插件声明
    alias(libs.plugins.kotlin.kapt.plugin) apply false
}