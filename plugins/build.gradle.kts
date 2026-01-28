plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("java-gradle-plugin")
}

dependencies {
    implementation(libs.gradle.plugin.kotlin.jvm)
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.android.api)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.arrow.core)
    implementation(libs.kotlin.serialization.json)
    compileOnly(gradleApi())
}

val javaVersion = JavaVersion.valueOf(libs.versions.java.get())
val javaVersionNumber = javaVersion.ordinal + 1

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersionNumber))
}

kotlin {
    jvmToolchain(javaVersionNumber)
}

gradlePlugin {
    plugins.create("Android library") {
        id = "hnau.android.lib"
        implementationClass = "hnau.plugin.HnauAndroidLibPlugin"
    }
}
