plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("java-gradle-plugin")
}

dependencies {
    implementation(libs.gradle.plugin.kotlin.jvm)
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.arrow.core)
    implementation(libs.kotlin.serialization.json)
}

val javaVersionNumber: Int = libs.versions.java
    .get()
    .dropWhile { !it.isDigit() }
    .toInt()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersionNumber))
}

kotlin {
    jvmToolchain(javaVersionNumber)
}

gradlePlugin {
    plugins.create("Android application") {
        id = "hnau.android.app"
        implementationClass = "hnau.plugin.HnauAndroidAppPlugin"
    }
    plugins.create("Android library") {
        id = "hnau.android.lib"
        implementationClass = "hnau.plugin.HnauAndroidLibPlugin"
    }
}
