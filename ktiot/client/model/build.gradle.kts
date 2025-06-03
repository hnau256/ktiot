import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("hnau.android.lib")
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.hnau.model)
                implementation(project(":ktiot:scheme"))
                implementation(project(":common:mqtt"))
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.io)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.pipe.annotations)
            }
        }
        androidMain
        desktopMain
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.pipe.processor)
}

tasks.withType<KotlinCompile>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
