package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SpringTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file(
            "build.gradle.kts",
            """
            plugins {
                id("spring")
            }

            val bootRunProfile =
                tasks.named<JavaExec>("bootRun").map { it.systemProperties["spring.profiles.active"].toString() }

            tasks.register("printBootRunProfile") {
                doLast { println("bootRun profile: " + bootRunProfile.get()) }
            }
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `every declared dependency resolves from the imported platforms`() {
        val runtime = fixture.build("dependencies", "--configuration", "runtimeClasspath").output
        val integrationTests =
            fixture.build("dependencies", "--configuration", "integrationTestRuntimeClasspath").output

        assertThat(runtime)
            .doesNotContain("FAILED")
            .contains("spring-boot-starter-webmvc")
            .contains("micrometer-registry-prometheus")
        assertThat(integrationTests)
            .doesNotContain("FAILED")
            .contains("spring-boot-starter-webmvc")
            .contains("spring-boot-starter-security-oauth2-resource-server")
            .contains("spring-modulith-starter-core")
            .contains("testcontainers-postgresql")
            .contains("jmolecules-hexagonal-architecture")
            .contains("jmolecules-archunit")
    }

    @Test
    fun `bootRun defaults to the local profile`() {
        val result = fixture.build("printBootRunProfile", "--no-configuration-cache")

        assertThat(result.output).contains("bootRun profile: local")
    }

    @Test
    fun `an explicitly requested profile wins over the default`() {
        val result =
            fixture.build("printBootRunProfile", "--no-configuration-cache", "-Dspring.profiles.active=demo")

        assertThat(result.output).contains("bootRun profile: demo")
    }
}
