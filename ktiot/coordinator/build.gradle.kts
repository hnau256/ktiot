plugins {
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
}

java {
    val javaVersion = JavaVersion.valueOf(libs.versions.java.get())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

val projectDependencies =
    listOf(
        project(":common:logging"),
        project(":common:mqtt"),
        project(":ktiot:scheme"),
    )

projectDependencies.forEach { depProject -> evaluationDependsOn(depProject.path) }

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.logging)
    implementation(libs.coroutines)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.hnau.kotlin)
    implementation(libs.hnau.model)
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

group = "com.github.hnau256"
version = "1.1.0"

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
