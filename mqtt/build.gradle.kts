plugins {
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        jvmMain {
            dependencies {
                implementation(libs.paho)
            }
        }
    }
}
