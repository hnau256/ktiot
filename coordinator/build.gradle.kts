plugins {
    id(hnau.plugins.kotlin.serialization.get().pluginId)
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.jvm.get().pluginId)
}

dependencies {
    implementation(hnau.commons.app.model)
    implementation(project(":mqtt"))
    implementation(project(":scheme"))
}