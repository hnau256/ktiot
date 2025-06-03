package hnau.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.internal.project.DefaultProject
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal enum class AndroidMode { Lib, App }

private const val CommonLogginIdentifier = ":common:logging"

internal fun Project.config(
    androidMode: AndroidMode,
) {

    val versions: VersionCatalog = extensions
        .getByType(VersionCatalogsExtension::class.java)
        .named("libs")

    val javaVersionString = versions
        .requireVersion("java")

    val javaVersion: JavaVersion =
        JavaVersion.valueOf(javaVersionString)

    plugins.apply("org.jetbrains.kotlin.multiplatform")

    when (androidMode) {
        AndroidMode.Lib -> plugins.apply("com.android.library")
        AndroidMode.App -> plugins.apply("com.android.application")
    }

    val hasSerializationPlugin =
        project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")

    val hasComposePlugin =
        project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose")

    val hasKspPlugin =
        project.plugins.hasPlugin("com.google.devtools.ksp")

    extensions.configure(KotlinMultiplatformExtension::class.java) { extension ->

        extension.androidTarget {
            compilations.configureEach { jvmCompilation ->
                jvmCompilation.kotlinOptions {
                    jvmTarget = javaVersion.toString()
                }
            }
        }

        extension.jvmToolchain { javaToolchainSpec ->
            javaToolchainSpec
                .languageVersion
                .set(JavaLanguageVersion.of(javaVersionString.dropWhile { !it.isDigit() }))
        }
        extension.jvm("desktop") {
            compilations.configureEach { jvmCompilation ->
                jvmCompilation.kotlinOptions {
                    jvmTarget = javaVersion.toString()
                }
            }
        }
        if (hasComposePlugin) {
            extension.sourceSets.getByName("desktopMain").apply {
                dependencies {
                    val composeDependencies = ComposePlugin.Dependencies(project)
                    implementation(composeDependencies.desktop.currentOs)
                }
            }
        }
        extension.sourceSets.getByName("commonMain").apply {
            languageSettings.enableLanguageFeature("ContextReceivers")
            dependencies {

                implementation(versions.findLibrary("logging").get().get())
                implementation(versions.findLibrary("arrow-core").get().get())
                implementation(versions.findLibrary("arrow-coroutines").get().get())
                implementation(versions.findLibrary("coroutines").get().get())
                implementation(versions.findLibrary("hnau-kotlin").get().get())

                if (identitifer != CommonLogginIdentifier) {
                    implementation(project(CommonLogginIdentifier))
                }

                if (hasSerializationPlugin) {
                    implementation(versions.findLibrary("arrow-serialization").get().get())
                    implementation(versions.findLibrary("kotlin-serialization-core").get().get())
                }

                if (hasComposePlugin) {
                    val composeDependencies = ComposePlugin.Dependencies(project)
                    implementation(composeDependencies.runtime)
                    implementation(composeDependencies.foundation)
                    implementation(composeDependencies.material3)
                    implementation(composeDependencies.materialIconsExtended)
                }
            }
        }
    }

    if (hasKspPlugin) {
        dependencies.add(
            "kspCommonMainMetadata",
            versions.findLibrary("pipe-processor").get().get()
        )
    }

    extensions.configure(BaseExtension::class.java) { extension ->

        val compileSdk = versions.requireVersion("androidCompileSdk").toInt()
        val minSdk = versions.requireVersion("androidMinSdk").toInt()
        extension.compileSdkVersion(compileSdk)
        extension.buildToolsVersion(versions.requireVersion("androidBuildTools"))

        extension.defaultConfig { config ->
            config.minSdk = minSdk
            config.targetSdk = compileSdk
        }

        extension.compileOptions { options ->
            options.targetCompatibility = javaVersion
            options.sourceCompatibility = javaVersion
        }
        extension.namespace = "hnau." + path.drop(1).replace(':', '.')
    }
}

private val Project.identitifer: String
    get() = (project as DefaultProject).identityPath.toString()

private fun VersionCatalog.requireVersion(
    alias: String,
): String = findVersion(alias)
    .get()
    .requiredVersion