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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PassagenExpress"
include(":app")
include(":core:designsystem")
include(":core:common")
include(":core:domain")
include(":core:datastore")
include(":core:network")
include(":core:data")
include(":core:printer")
include(":feature:setup")
include(":feature:idle")
include(":feature:city")
include(":feature:date")
include(":feature:trip")
include(":feature:room")
include(":feature:passenger")
include(":feature:payment")
include(":feature:print")
include(":feature:settings")
