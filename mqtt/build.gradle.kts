plugins {
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.paho)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.paho)
            }
        }
    }
}
