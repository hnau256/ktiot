plugins {
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.kotlin.serialization.get().pluginId)
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.paho)
                implementation(libs.ktor.http)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.paho)
            }
        }
    }
}
