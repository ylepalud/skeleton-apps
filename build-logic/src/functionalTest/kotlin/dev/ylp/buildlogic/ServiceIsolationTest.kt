package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ServiceIsolationTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.settings("services:a", "services:b", "libs:shared")
        val service = "plugins {\n    java\n    id(\"service-isolation\")\n}\n"
        fixture.file("services/b/build.gradle.kts", service)
        fixture.file("libs/shared/build.gradle.kts", "plugins {\n    java\n}\n")
    }

    @Test
    fun `a service depending on another service fails check`() {
        fixture.file(
            "services/a/build.gradle.kts",
            "plugins {\n    java\n    id(\"service-isolation\")\n}\n\n" +
                "dependencies {\n    implementation(project(\":services:b\"))\n}\n",
        )

        val result = fixture.fail(":services:a:verifyServiceIsolation")

        assertThat(result.output)
            .contains(":services:a depends on another service")
            .contains("implementation -> :services:b")
    }

    @Test
    fun `a service depending on a shared library passes`() {
        fixture.file(
            "services/a/build.gradle.kts",
            "plugins {\n    java\n    id(\"service-isolation\")\n}\n\n" +
                "dependencies {\n    implementation(project(\":libs:shared\"))\n}\n",
        )

        val result = fixture.build(":services:a:verifyServiceIsolation")

        assertThat(result.task(":services:a:verifyServiceIsolation")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
