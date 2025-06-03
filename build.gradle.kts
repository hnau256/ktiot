tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

plugins {
    alias(libs.plugins.compose.desktop) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.app) apply false
    alias(libs.plugins.android.lib) apply false
}
