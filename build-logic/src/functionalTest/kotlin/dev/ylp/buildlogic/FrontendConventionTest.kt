package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FrontendConventionTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", JavaSources.javaProject("frontend-convention"))
    }

    @Test
    fun `check lints and tests the frontend after generating the API client`() {
        val result = fixture.build("check", "--dry-run")

        assertThat(result.output)
            .contains(":generateApi SKIPPED")
            .contains(":npmRunLint SKIPPED")
            .contains(":npmRunTest SKIPPED")
            .doesNotContain(":npmRunE2e")
    }

    @Test
    fun `assemble builds the production bundle`() {
        val result = fixture.build("assemble", "--dry-run")

        assertThat(result.output).contains(":nodeSetup SKIPPED").contains(":npmRunBuild SKIPPED")
    }

    @Test
    fun `dependencies are installed from the lockfile`() {
        fixture.file(
            "build.gradle.kts",
            """
            plugins {
                id("frontend-convention")
            }

            tasks.register("printInstallCommand") {
                val command = node.npmInstallCommand
                doLast { println("install: " + command.get()) }
            }
            """.trimIndent() + "\n",
        )

        val result = fixture.build("printInstallCommand")

        assertThat(result.output).contains("install: ci")
    }

    @Test
    fun `the API contract location is configurable`() {
        fixture.file(
            "build.gradle.kts",
            """
            plugins {
                id("frontend-convention")
            }

            frontend {
                apiContract = layout.projectDirectory.file("api/openapi.yaml")
            }

            tasks.register("printContract") {
                val contract = frontend.apiContract.map { it.asFile.name }
                doLast { println("contract: " + contract.get()) }
            }
            """.trimIndent() + "\n",
        )

        val result = fixture.build("printContract")

        assertThat(result.output).contains("contract: openapi.yaml")
    }
}
