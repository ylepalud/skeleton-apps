/*
 * A Kotlin Multiplatform library shared by the native clients: Android, iOS,
 * the command line on macOS and Linux, and a JVM target. The JVM target ships
 * nowhere: it carries the architecture tests and measures coverage. The common
 * tests run on every target.
 *
 * Apple targets compile and test only on macOS; elsewhere Kotlin skips them.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    // client:mobile:start
    id("com.android.kotlin.multiplatform.library")
    // client:mobile:end
    id("org.jetbrains.kotlinx.kover")
    id("com.diffplug.spotless")
}

group = "dev.ylp"

val libs = the<VersionCatalogsExtension>().named("libs")

fun library(alias: String) = libs.findLibrary(alias).get()

kotlin {
    jvmToolchain(providers.gradleProperty("javaToolchainVersion").get().toInt())

    // client:mobile:start
    android {
        namespace = "dev.ylp.${project.name}"
        compileSdk = providers.gradleProperty("androidCompileSdk").get().toInt()
        minSdk = providers.gradleProperty("androidMinSdk").get().toInt()
        // The common tests also run against the Android variant, on the host JVM.
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()
    // client:mobile:end
    // client:cli:start
    // The command-line client.
    macosArm64()
    linuxX64()
    // client:cli:end
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

// The Linux binaries link against the system's SQLite, which a Mac does not have for Linux: they
// are linked, and their tests run, on a Linux host only. Elsewhere the Linux code still compiles.
val onLinux = providers.systemProperty("os.name").get().startsWith("Linux")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
    if (binary.target.konanTarget == org.jetbrains.kotlin.konan.target.KonanTarget.LINUX_X64) {
        val linuxHost = onLinux
        onlyIf("Linux binaries link on a Linux host only") { linuxHost }
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
