import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "emt-3881-replacement-extras-spike"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":sender")
include(":receiver")
