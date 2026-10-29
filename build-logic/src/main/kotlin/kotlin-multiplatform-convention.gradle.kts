/*
 * A Kotlin Multiplatform library shared by the native clients: Android, iOS
 * and a JVM target. The JVM target ships nowhere: it carries the architecture
 * tests and measures coverage. The common tests run on every target.
 *
 * iOS targets compile and test only on macOS; elsewhere Kotlin skips them.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlinx.kover")
    id("com.diffplug.spotless")
}

group = "dev.ylp"

val libs = the<VersionCatalogsExtension>().named("libs")

fun library(alias: String) = libs.findLibrary(alias).get()

kotlin {
    jvmToolchain(providers.gradleProperty("javaToolchainVersion").get().toInt())

    android {
        namespace = "dev.ylp.${project.name}"
        compileSdk = providers.gradleProperty("androidCompileSdk").get().toInt()
        minSdk = providers.gradleProperty("androidMinSdk").get().toInt()
        // The common tests also run against the Android variant, on the host JVM.
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()
    jvm()

    compilerOptions {
        allWarningsAsErrors = true
    }

    sourceSets {
        commonMain.dependencies {
            implementation(library("kotlinx-coroutines-core"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(library("kotlinx-coroutines-test"))
        }
    }
}

// Coverage of the hand-written code, measured on the JVM target, at the same bar as the API.
kover {
    reports {
        // Serialisable classes are transport shapes whose branches the serialisation plugin writes.
        filters {
            excludes {
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
        verify {
            rule {
                bound { minValue = 80 }
                bound {
                    minValue = 80
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
