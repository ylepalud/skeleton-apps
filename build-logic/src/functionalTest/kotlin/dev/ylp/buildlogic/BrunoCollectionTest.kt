package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BrunoCollectionTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", JavaSources.javaProject("bruno-collection"))
    }

    @Test
    fun `check verifies the collection against the contract, with the configuration cache`() {
        val result = fixture.build("check", "--dry-run")

        assertThat(result.output)
            .contains(":npmInstall SKIPPED")
            .contains(":verifyCollection SKIPPED")
            .contains("Configuration cache entry stored")
    }

    @Test
    fun `updating the collection stays out of check`() {
        val check = fixture.build("check", "--dry-run")
        val update = fixture.build("updateCollection", "--dry-run")

        assertThat(check.output).doesNotContain(":updateCollection")
        assertThat(update.output).contains(":updateCollection SKIPPED")
    }

    @Test
    fun `the contract location is configurable`() {
        fixture.file(
            "build.gradle.kts",
            """
            plugins {
                id("bruno-collection")
            }

            brunoCollection {
                contract = layout.projectDirectory.file("api/openapi.yaml")
            }

            tasks.register("printContract") {
                val contract = brunoCollection.contract.map { it.asFile.name }
                doLast { println("contract: " + contract.get()) }
            }
            """.trimIndent() + "\n",
        )

        val result = fixture.build("printContract")

        assertThat(result.output).contains("contract: openapi.yaml")
    }
}
