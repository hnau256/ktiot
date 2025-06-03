plugins {
    id("hnau.android.lib")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.hnau.model)
            }
        }

        val sharedJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.paho)
                implementation(libs.kotlin.serialization.core)
            }
        }

        androidMain {
            dependsOn(sharedJvmMain)
        }

        desktopMain {
            dependsOn(sharedJvmMain)
        }
    }
}
