import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.compose.desktop)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.ksp)
    id("hnau.android.lib")
}

compose.resources {
    packageOfResClass = "hnau.ktiot.client.projector"
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.hnau.projector)
                implementation(libs.hnau.model)
                implementation(project(":ktiot:scheme"))
                implementation(project(":common:mqtt"))
                implementation(project(":ktiot:client:model"))
                implementation(libs.kotlin.datetime)
                implementation(libs.immutable)
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
