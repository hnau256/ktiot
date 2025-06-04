plugins {
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.logging)
    implementation(libs.coroutines)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.hnau.kotlin)
    implementation(libs.hnau.model)
    implementation(project(":common:logging"))
    implementation(project(":common:mqtt"))
    implementation(project(":ktiot:scheme"))
}
