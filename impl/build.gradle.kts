plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.kotlin.datetime)
    implementation(libs.hnau.model)
    implementation(libs.slf4j.simple)
    implementation(project(":common:mqtt"))
    implementation(project(":ktiot:scheme"))
    implementation(project(":ktiot:coordinator"))
}


application {
    mainClass.set("hnau.impl.ImplKt")
}