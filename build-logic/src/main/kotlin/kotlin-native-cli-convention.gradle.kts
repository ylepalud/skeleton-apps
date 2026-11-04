/*
 * A command-line client in Kotlin/Native: an executable for macOS (Apple
 * Silicon) and Linux (x86_64), and a JVM target that ships nowhere: it runs
 * the common tests and measures coverage, as for the shared library.
 *
 * The macOS binary links only on macOS; the Linux one only on Linux, as it
 * links against the system's SQLite.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlinx.kover")
    id("com.diffplug.spotless")
}

group = "dev.ylp"

val libs = the<VersionCatalogsExtension>().named("libs")

fun library(alias: String) = libs.findLibrary(alias).get()

kotlin {
    jvmToolchain(providers.gradleProperty("javaToolchainVersion").get().toInt())

    jvm()
    macosArm64 { binaries.executable() }
    linuxX64 { binaries.executable() }

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

val onLinux = providers.systemProperty("os.name").get().startsWith("Linux")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
    if (binary.target.konanTarget == org.jetbrains.kotlin.konan.target.KonanTarget.LINUX_X64) {
        val linuxHost = onLinux
        onlyIf("Linux binaries link on a Linux host only") { linuxHost }
    }
}

kover {
    reports {
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
