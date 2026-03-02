package hnau.plugin

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.internal.project.DefaultProject
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal enum class AndroidMode { Lib }

internal fun Project.config(androidMode: AndroidMode?) {
    val versions: VersionCatalog =
        extensions
            .getByType(VersionCatalogsExtension::class.java)
            .named("libs")

    val javaVersionString =
        versions
            .requireVersion("java")

    val javaVersionNumberString: String =
        javaVersionString.dropWhile { !it.isDigit() }

    val javaTarget: JvmTarget =
        JvmTarget.fromTarget(javaVersionNumberString)

    val javaVersion: JavaVersion =
        JavaVersion.valueOf(javaVersionString)

    val javaLanguageVersion: JavaLanguageVersion =
        JavaLanguageVersion.of(javaVersionNumberString.toInt())

    plugins.apply("org.jetbrains.kotlin.multiplatform")

    when (androidMode) {
        AndroidMode.Lib -> plugins.apply("com.android.kotlin.multiplatform.library")
        null -> Unit
    }

    val hasSerializationPlugin =
        project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")

    val hasComposePlugin =
        project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose")

    val hasKspPlugin =
        project.plugins.hasPlugin("com.google.devtools.ksp")

    extensions.configure(KotlinMultiplatformExtension::class.java) { extension ->

        extension.jvmToolchain { javaToolchainSpec ->
            javaToolchainSpec
                .languageVersion
                .set(JavaLanguageVersion.of(javaVersionNumberString))
        }

        extension.extensions.configure(KotlinMultiplatformAndroidLibraryExtension::class.java) { androidLibrary ->
            androidLibrary.namespace = "hnau." + path.drop(1).replace(':', '.')
            androidLibrary.compileSdk = versions.requireVersion("androidCompileSdk").toInt()
            androidLibrary.minSdk = versions.requireVersion("androidMinSdk").toInt()
        }

        extension.jvm("desktop") {
            compilerOptions {
                jvmTarget.set(javaTarget)
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
                implementation(versions.findLibrary("hnau-kotlin").get().get())

                if (hasSerializationPlugin) {
                    implementation(versions.findLibrary("kotlin-serialization-core").get().get())
                }

                if (hasComposePlugin) {
                    val composeDependencies = ComposePlugin.Dependencies(project)
                    implementation(composeDependencies.runtime)
                    implementation(composeDependencies.foundation)
                    implementation(composeDependencies.material3)
                    implementation(composeDependencies.materialIconsExtended)
                }

                if (hasKspPlugin) {
                    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
                    withKspProcessorLibraries(
                        versions = versions,
                        suffix = "annotations",
                    ) { dependency ->
                        implementation(dependency)
                    }
                }
            }
        }
    }

    if (hasKspPlugin) {
        withKspProcessorLibraries(
            versions = versions,
            suffix = "processor",
        ) { dependency ->
            dependencies.add(
                "kspCommonMainMetadata",
                dependency,
            )
        }

        tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach { task ->
            if (task.name != "kspCommonMainKotlinMetadata") {
                task.dependsOn("kspCommonMainKotlinMetadata")
            }
        }
    }
}

private val Project.identitifer: String
    get() = (project as DefaultProject).identityPath.toString()

private fun VersionCatalog.requireVersion(alias: String): String =
    findVersion(alias)
        .get()
        .requiredVersion

private val kspProcessorsNames: List<String> =
    listOf("pipe", "enumvalues", "sealup", "loggable")

private fun withKspProcessorLibraries(
    versions: VersionCatalog,
    suffix: String,
    block: (MinimalExternalModuleDependency) -> Unit,
) {
    kspProcessorsNames.forEach { prefix ->
        val name = "$prefix-$suffix"
        val dependency = versions.findLibrary(name).get().get()
        block(dependency)
    }
}
