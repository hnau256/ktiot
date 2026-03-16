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
    id("org.hnau.plugin.settings") version "1.2.6"
}

hnau {
    groupId = "org.hnau.ktiot"
    publish {
        version = "1.8.0"
        gitUrl = "https://github.com/hnau256/ktiot-commons"
    }
}
