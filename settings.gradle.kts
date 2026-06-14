rootProject.name = "KtIoT-Commons"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("org.hnau.plugin.settings") version "1.23.4"
}

hnau {
    publish {
        version = "1.8.0"
        gitUrl = "https://github.com/hnau256/ktiot-commons"
    }
}
