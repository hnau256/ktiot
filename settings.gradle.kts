rootProject.name = "KtIoT"

pluginManagement {
    includeBuild("plugins")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

fun collectSubproject(
    projectDir: File,
    projectIdentifier: String = "",
): List<String> {
    fun collectProjectWithSubprojects(
        dir: File,
        projectIdentifier: String,
    ): List<String> {
        fun checkIsProject(
            dir: File,
            projectIdentifier: String,
        ): Boolean {
            if (projectIdentifier == ":plugins") {
                return false
            }
            if (projectIdentifier == ":buildSrc") {
                return false
            }
            return dir
                .list()
                ?.any { file -> file == "settings.gradle.kts" || file == "build.gradle.kts" }
                ?: false
        }

        return when (checkIsProject(dir, projectIdentifier)) {
            true -> listOf(projectIdentifier)
            false -> collectSubproject(dir, projectIdentifier)
        }
    }

    return projectDir
        .listFiles()
        .orEmpty()
        .filter { it.exists() && it.isDirectory }
        .flatMap { subdir ->
            val subdirName = subdir.name
            collectProjectWithSubprojects(
                dir = subdir,
                projectIdentifier = "$projectIdentifier:$subdirName",
            )
        }
}

include(collectSubproject(rootProject.projectDir))
