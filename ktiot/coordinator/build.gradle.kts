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

val projectDependencies = listOf(
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
version = "1.0.7"

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.jar.get())
            groupId = project.group as String
            version = project.version as String

            // Создаем POM только с внешними зависимостями
            pom.withXml {
                val node = asNode() as groovy.util.Node

                // Удаляем существующий узел dependencies, если есть
                node.children().removeAll { (it as groovy.util.Node).name() == "dependencies" }

                // Создаем новый узел dependencies
                val dependenciesNode = node.appendNode("dependencies")

                // Добавляем только внешние зависимости из implementation
                val implementationConfig = project.configurations.getByName("implementation")
                implementationConfig.allDependencies.forEach { dep ->
                    if (dep !is org.gradle.api.artifacts.ProjectDependency) {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dep.group)
                        dependencyNode.appendNode("artifactId", dep.name)
                        dependencyNode.appendNode("version", dep.version ?: "")
                        dependencyNode.appendNode("scope", "runtime")
                    }
                }
            }
        }
    }
}
