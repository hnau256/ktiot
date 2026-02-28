plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
}

group = "com.github.hnau256"
version = "1.6.0"

java {
    val javaVersion = JavaVersion.valueOf(libs.versions.java.get())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withSourcesJar()
}

val projectDependencies =
    listOf(
        project(":common:logging"),
        project(":common:mqtt"),
        project(":ktiot:scheme"),
    )

projectDependencies.forEach { depProject -> evaluationDependsOn(depProject.path) }

dependencies {
    implementation(libs.logging)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.hnau.kotlin)
    implementation(libs.hnau.model)

    implementation(libs.pipe.annotations)
    implementation(libs.sealup.annotations)
    implementation(libs.enumvalues.annotations)

    ksp(libs.pipe.processor)
    ksp(libs.sealup.processor)
    ksp(libs.enumvalues.processor)

    projectDependencies.forEach { implementation(it) }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Зависимость от сборки всех проектных модулей
    projectDependencies.forEach { depProject ->
        dependsOn(depProject.tasks.named("desktopMainClasses"))
    }

    // Включаем классы из всех проектных зависимостей
    projectDependencies.forEach { depProject ->
        from(depProject.layout.buildDirectory.dir("classes/kotlin/desktop/main")) {
            include("**/*.class")
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(layout.buildDirectory.dir("generated/ksp/main/kotlin"))
}

fun String.isLocalGroup() = startsWith("KtIoT")

fun collectExternalDeps(
    deps: Set<ResolvedDependency>,
    visited: MutableSet<String> = mutableSetOf(),
): List<ResolvedDependency> =
    deps.flatMap { dep ->
        if (!visited.add("${dep.moduleGroup}:${dep.moduleName}")) return@flatMap emptyList()
        if (dep.moduleGroup.isLocalGroup()) collectExternalDeps(dep.children, visited) else listOf(dep)
    }

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.jar)
            artifact(tasks.named("sourcesJar"))

            pom {
                withXml {
                    val depsNode = asNode().appendNode("dependencies")

                    configurations["runtimeClasspath"]
                        .resolvedConfiguration
                        .firstLevelModuleDependencies
                        .let { collectExternalDeps(it) }
                        .distinctBy { it.moduleGroup to it.moduleName }
                        .forEach { dep ->
                            depsNode.appendNode("dependency").apply {
                                appendNode("groupId", dep.moduleGroup)
                                appendNode("artifactId", dep.moduleName)
                                appendNode("version", dep.moduleVersion)
                                appendNode("scope", "runtime")
                            }
                        }
                }
            }
        }
    }
}
