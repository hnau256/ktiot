plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.kotlin.datetime)
    implementation(libs.hnau.kotlin)
    implementation(libs.hnau.model)
    implementation(libs.pipe.annotations)
    implementation(libs.sealup.annotations)
    implementation(libs.slf4j.simple)
    implementation(project(":common:mqtt"))
    implementation(project(":ktiot:scheme"))
    implementation(project(":ktiot:coordinator"))

    ksp(libs.pipe.processor)
    ksp(libs.sealup.processor)
}


application {
    mainClass.set("hnau.impl.ImplKt")
}