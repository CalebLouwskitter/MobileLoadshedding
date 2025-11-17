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

rootProject.name = "MobileLoadshedding"
include(":app")
include(":unityLibrary")
project(":unityLibrary").projectDir = file("unityLibrary")
include(":unityLibrary:mobilenotifications.androidlib")
project(":unityLibrary:mobilenotifications.androidlib").projectDir = file("unityLibrary/mobilenotifications.androidlib")
