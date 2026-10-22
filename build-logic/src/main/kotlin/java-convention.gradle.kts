/*
 * Baseline Java conventions shared by every JVM module: toolchain, compiler
 * checks, formatting and the test platform. Free of any framework concern.
 */
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

group = "dev.ylp"

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = providers.gradleProperty("javaToolchainVersion").map { JavaLanguageVersion.of(it) }
        // Pinned so that local builds and CI compile with the same distribution.
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencies {
    implementation(libs.findLibrary("jspecify").get())
    errorprone(libs.findLibrary("errorprone-core").get())
    errorprone(libs.findLibrary("nullaway").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing", "-Werror"))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        excludedPaths = ".*/build/generated/.*"
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "dev.ylp")
        option("NullAway:JSpecifyMode", "true")
    }
}

// Tests deliberately pass nulls and build partial fixtures.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.check("NullAway", CheckSeverity.OFF)
}

// Every test suite runs on JUnit Jupiter, at the catalog version.
testing {
    suites.withType<JvmTestSuite>().configureEach {
        useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)
    }
}

// Targets are explicit file trees: a string pattern walks the whole project
// directory, build/ included, while other Spotless tasks write there in parallel.
spotless {
    java {
        target(fileTree("src") { include("**/*.java") })
        googleJavaFormat(providers.gradleProperty("googleJavaFormatVersion").get())
    }
    kotlinGradle {
        target(fileTree(projectDir) { include("*.gradle.kts").exclude("build", ".gradle", "node_modules") })
        ktlint()
    }
}
