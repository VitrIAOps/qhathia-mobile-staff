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

rootProject.name = "QhathiaStaff"
include(":app")
include(":core:common")
include(":core:network")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:testing")
include(":core:notifications")
include(":features:auth")
include(":features:product-inventory")
include(":features:product-register")
include(":features:device-linking")
include(":features:order-requests")
