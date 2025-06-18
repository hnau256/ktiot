plugins {
    id("hnau.android.lib")
}

kotlin {
    linuxX64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.datetime)
                implementation(libs.hnau.model)
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

        desktopMain {
            dependsOn(sharedJvmMain)
        }
    }
}
