// Archivo: settings.gradle.kts

pluginManagement {
    repositories {
        google()

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

rootProject.name = "PianIApp"
include(":app")
// include(":data")
// include(":domain")