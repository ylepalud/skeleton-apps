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
        fixture.settings("apps:services:a", "apps:services:b", "libs:shared")
        val service = "plugins {\n    java\n    id(\"service-isolation\")\n}\n"
        fixture.file("apps/services/b/build.gradle.kts", service)
        fixture.file("libs/shared/build.gradle.kts", "plugins {\n    java\n}\n")
    }

    @Test
    fun `a service depending on another service fails check`() {
        fixture.file(
            "apps/services/a/build.gradle.kts",
            "plugins {\n    java\n    id(\"service-isolation\")\n}\n\n" +
                "dependencies {\n    implementation(project(\":apps:services:b\"))\n}\n",
        )

        val result = fixture.fail(":apps:services:a:verifyServiceIsolation")

        assertThat(result.output)
            .contains(":apps:services:a depends on another service")
            .contains("implementation -> :apps:services:b")
    }

    @Test
    fun `a service depending on a shared library passes`() {
        fixture.file(
            "apps/services/a/build.gradle.kts",
            "plugins {\n    java\n    id(\"service-isolation\")\n}\n\n" +
                "dependencies {\n    implementation(project(\":libs:shared\"))\n}\n",
        )

        val result = fixture.build(":apps:services:a:verifyServiceIsolation")

        assertThat(result.task(":apps:services:a:verifyServiceIsolation")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
