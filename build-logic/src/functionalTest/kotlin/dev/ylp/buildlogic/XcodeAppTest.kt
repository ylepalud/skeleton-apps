package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class XcodeAppTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        build()
    }

    private fun build(extra: String = "") =
        fixture.file(
            "build.gradle.kts",
            """
            plugins {
                id("xcode-app")
            }

            xcodeApp {
                projectDir = layout.projectDirectory.dir("ios")
                scheme = "Demo"
                bundleId = "dev.example.demo"
                $extra
            }
            """.trimIndent() + "\n",
        )

    @Test
    fun `check lints the swift sources and verifies the coverage of the tests, with the configuration cache`() {
        val result = fixture.build("check", "--dry-run")

        assertThat(result.output)
            .contains(":iosLint SKIPPED")
            .contains(":iosGenerateProject SKIPPED")
            .contains(":iosTest SKIPPED")
            .contains(":iosCoverageReport SKIPPED")
            .contains(":iosVerifyCoverage SKIPPED")
            .contains("Configuration cache entry stored")
    }

    @Test
    fun `without xcode every task is skipped`() {
        build("xcodeAvailable = false")

        val result = fixture.build("iosLint", "iosRun")

        assertThat(result.output)
            .contains("> Task :iosLint SKIPPED")
            .contains("> Task :iosGenerateProject SKIPPED")
            .contains("> Task :iosRun SKIPPED")
    }

    @Test
    fun `a coverage under the minimum fails the build`() {
        coverage(0.62)

        val result = fixture.fail("iosVerifyCoverage", "-x", "iosCoverageReport")

        assertThat(result.output).contains("Line coverage of Demo.app is 62.0%, under the minimum of 80%")
    }

    @Test
    fun `a coverage at the minimum passes`() {
        coverage(0.8)

        val result = fixture.build("iosVerifyCoverage", "-x", "iosCoverageReport")

        assertThat(result.output).contains("Line coverage of Demo.app: 80.0%")
    }

    @Test
    fun `the xcode project is generated from project yml`() {
        assumeTrue(
            System.getenv("PATH").split(File.pathSeparator).any { File(it, "xcodegen").canExecute() },
            "XcodeGen is not installed",
        )
        fixture.file(
            "ios/project.yml",
            """
            name: Demo
            options:
              deploymentTarget:
                iOS: "18.0"
            targets:
              Demo:
                type: application
                platform: iOS
                sources: [Demo]
            """.trimIndent() + "\n",
        )
        fixture.file("ios/Demo/DemoApp.swift", "import SwiftUI\n")

        fixture.build("iosGenerateProject")

        assertThat(dir.resolve("ios/Demo.xcodeproj/project.pbxproj")).exists()
    }

    private fun coverage(lineCoverage: Double) {
        build("xcodeAvailable = true")
        fixture.file(
            "build/ios/coverage.json",
            """{"targets": [{"name": "Demo.app", "lineCoverage": $lineCoverage}, {"name": "DemoTests.xctest", "lineCoverage": 1.0}]}""",
        )
    }
}
