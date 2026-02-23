pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "One"
include(":app")
include(":wear")
include(":core")

file("features").listFiles()
    ?.filter { it.isDirectory }
    ?.forEach { featureDir ->
        val submodules = featureDir.listFiles()
            ?.filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
        if (submodules != null && submodules.isNotEmpty()) {
            submodules.forEach { include(":features:${featureDir.name}:${it.name}") }
        } else if (File(featureDir, "build.gradle.kts").exists()) {
            include(":features:${featureDir.name}")
        }
    }
