plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("hnau.android.lib")
}

kotlin {
    linuxX64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.hnau.model)
                implementation(libs.kotlinx.atomicfu)
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
