/*
 * Adds an `integrationTest` suite, run after the unit tests and wired into
 * `check`. It inherits the unit-test dependencies, so a test library is
 * declared once for both suites.
 */
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("java-convention")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
            }
            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }
    }
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}

configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

// Same rule as the unit tests: fixtures may pass nulls on purpose.
tasks.named<JavaCompile>("compileIntegrationTestJava") {
    options.errorprone.check("NullAway", CheckSeverity.OFF)
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
