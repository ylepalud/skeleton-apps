pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        // The Android Gradle Plugin, a dependency of the convention plugins.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    /*
     * PREFER_SETTINGS rather than FAIL_ON_PROJECT_REPOS: the node-gradle plugin
     * adds its own Ivy repository on frontend projects to download the pinned
     * Node.js toolchain, which the strict mode would reject. PREFER_SETTINGS
     * ignores it, so the same repository is declared here, restricted to the
     * org.nodejs:node module so it never takes part in JVM dependency resolution.
     */
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        mavenCentral()
        // Android libraries and the build tools the Android Gradle Plugin downloads.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist") {
                    name = "Node.js"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("org.nodejs", "node") }
        }
    }
}

rootProject.name = "skeleton-apps"

include("apps:services:api")
include("apps:contracts:api")
include("apps:services:api-app")
include("apps:bruno")
include("apps:clients:shared")
include("apps:clients:android")
