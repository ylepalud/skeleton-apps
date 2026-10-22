package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ContainerImageTest {
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
                id("container-image")
            }

            val from = jib.from.image
            val to = jib.to.image
            val tags = jib.to.tags

            tasks.register("printImage") {
                doLast { println("from=" + from + " to=" + to + " tags=" + tags) }
            }
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `the image builds from the pinned distroless base with a local tag by default`() {
        val result = fixture.build("printImage", "--no-configuration-cache")

        assertThat(result.output)
            .containsPattern("from=gcr.io/distroless/java25-debian13:nonroot@sha256:[0-9a-f]{64}")
            .contains("to=apps/fixture")
            .contains("tags=[local]")
    }

    @Test
    fun `CI sets the repository and the commit tag`() {
        val result =
            fixture.build(
                "printImage",
                "--no-configuration-cache",
                "-PimageRepository=ghcr.io/owner/api",
                "-PimageTag=0123abc",
            )

        assertThat(result.output).contains("to=ghcr.io/owner/api").contains("tags=[0123abc]")
    }

    @Test
    fun `a spring boot application ships its production classpath only`() {
        fixture.file(
            "build.gradle.kts",
            """
            plugins {
                id("spring")
                id("container-image")
            }

            val classpath = jib.configurationName

            tasks.register("printClasspath") {
                doLast { println("classpath=" + classpath.get()) }
            }
            """.trimIndent() + "\n",
        )

        val result = fixture.build("printClasspath", "--no-configuration-cache")

        assertThat(result.output).contains("classpath=productionRuntimeClasspath")
    }

    @Test
    fun `jib tasks do not break a configuration-cached build`() {
        val result = fixture.build("jibDockerBuild", "--dry-run")

        assertThat(result.output).contains(":jibDockerBuild SKIPPED")
    }
}
