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
version = "1.0.7"

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group as String
            version = project.version as String

            pom.withXml {
                fun groovy.util.Node.childNodes() = children().filterIsInstance<groovy.util.Node>()

                fun groovy.util.Node.child(name: String) = childNodes().find { it.name().toString().endsWith(name) }

                fun String.isLocalGroup() = startsWith("KtIoT")

                fun collectExternalDeps(
                    deps: Set<ResolvedDependency>,
                    visited: MutableSet<String> = mutableSetOf(),
                ): List<ResolvedDependency> =
                    deps.flatMap { dep ->
                        if (!visited.add(dep.moduleGroup + ":" + dep.moduleName)) return@flatMap emptyList()
                        if (dep.moduleGroup.isLocalGroup()) {
                            collectExternalDeps(dep.children, visited)
                        } else {
                            listOf(dep)
                        }
                    }

                val dependenciesNode = (asNode() as groovy.util.Node).child("dependencies") ?: return@withXml

                val existingDeps =
                    dependenciesNode
                        .childNodes()
                        .mapNotNull { node ->
                            val groupId = node.child("groupId")?.text() ?: return@mapNotNull null
                            val artifactId = node.child("artifactId")?.text() ?: return@mapNotNull null
                            groupId to artifactId
                        }.toSet()

                val localModuleDeps =
                    projectDependencies.flatMap { proj ->
                        val firstLevel =
                            proj.configurations
                                .findByName("desktopRuntimeClasspath")
                                ?.resolvedConfiguration
                                ?.firstLevelModuleDependencies
                                ?: emptySet()
                        collectExternalDeps(firstLevel)
                    }

                localModuleDeps
                    .distinctBy { it.moduleGroup to it.moduleName }
                    .filterNot { (it.moduleGroup to it.moduleName) in existingDeps }
                    .forEach { dep ->
                        dependenciesNode.appendNode("dependency").apply {
                            appendNode("groupId", dep.moduleGroup)
                            appendNode("artifactId", dep.moduleName)
                            appendNode("version", dep.moduleVersion)
                            appendNode("scope", "runtime")
                        }
                    }

                dependenciesNode
                    .childNodes()
                    .filter { it.child("groupId")?.text()?.isLocalGroup() == true }
                    .forEach { dependenciesNode.remove(it) }
            }
        }
    }
}
