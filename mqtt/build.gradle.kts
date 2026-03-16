plugins {
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.paho)
            }
        }

        val sharedJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.paho)
            }
        }

        androidMain {
            dependsOn(sharedJvmMain)
        }

        jvmMain {
            dependsOn(sharedJvmMain)
        }
    }
}
