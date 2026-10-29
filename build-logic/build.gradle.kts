plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

// The Gradle plugins applied by the convention plugins. Their versions come
// from the shared version catalog, like every other classpath dependency.
dependencies {
    implementation(libs.gradle.spring.boot)
    implementation(libs.gradle.spotless)
    implementation(libs.gradle.errorprone)
    implementation(libs.gradle.pitest)
    implementation(libs.gradle.node)
    implementation(libs.gradle.jib)
    implementation(libs.gradle.openapi.generator)
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.kotlin.serialization)
    implementation(libs.gradle.android)
    implementation(libs.gradle.kover)
    implementation(libs.gradle.compose.compiler)
}

// Functional tests run every convention plugin against throwaway projects,
// including the cases where a quality gate must fail the build.
testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(gradleTestKit())
                implementation(libs.assertj.core)
            }
            targets.configureEach {
                testTask.configure {
                    // Each test starts a nested Gradle build; keep them apart from
                    // the outer daemon's memory and let them run in parallel.
                    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
                    systemProperty("conventions.rootDir", rootDir.parentFile.absolutePath)
                }
            }
        }
    }
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])
}

tasks.named("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

// The build logic is code too: Kotlin sources and scripts follow ktlint.
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "src/**/*.gradle.kts")
        ktlint()
    }
}
