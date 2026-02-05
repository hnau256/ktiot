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

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.logging)
    implementation(libs.coroutines)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.hnau.kotlin)
    implementation(libs.hnau.model)
    implementation(project(":common:logging"))
    implementation(project(":common:mqtt"))
    implementation(project(":ktiot:scheme"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Зависимости для компиляции всех модулей
    val dependentProjects =
        listOf(
            project(":common:logging"),
            project(":common:mqtt"),
            project(":ktiot:scheme"),
        )

    dependentProjects.forEach { depProject ->
        // Ищем задачу компиляции desktop
        val compileTask =
            depProject.tasks.find { task ->
                task.name.contains("desktop", ignoreCase = true) &&
                    task.name.contains("compile", ignoreCase = true) &&
                    task.name.contains("kotlin", ignoreCase = true)
            }
        if (compileTask != null) {
            dependsOn(compileTask)
        }
    }

    // Включаем классы из зависимых модулей
    from(
        provider {
            val loggingProject = project(":common:logging")
            val classesDir = file("${loggingProject.buildDir}/classes/kotlin/desktop/main")
            if (classesDir.exists()) classesDir else files()
        },
    )
    from(
        provider {
            val mqttProject = project(":common:mqtt")
            val classesDir = file("${mqttProject.buildDir}/classes/kotlin/desktop/main")
            if (classesDir.exists()) classesDir else files()
        },
    )
    from(
        provider {
            val schemeProject = project(":ktiot:scheme")
            val classesDir = file("${schemeProject.buildDir}/classes/kotlin/desktop/main")
            if (classesDir.exists()) classesDir else files()
        },
    )
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
