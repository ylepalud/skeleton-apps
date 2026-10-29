package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AndroidAppConventionTest {
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
                id("android-app-convention")
            }

            android {
                namespace = "dev.ylp.sample"
                defaultConfig { applicationId = "dev.ylp.sample" }
            }
            """.trimIndent() + "\n",
        )
        fixture.file("src/main/AndroidManifest.xml", "<manifest />\n")
        fixture.file(
            "src/main/kotlin/dev/ylp/sample/Greeter.kt",
            """
            package dev.ylp.sample

            fun greet(name: String): String = if (name.isEmpty()) "Hello" else "Hello, ${'$'}name"
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `tested code passes the JVM tests on the JUnit platform and the coverage gate`() {
        fixture.file("src/test/kotlin/dev/ylp/sample/GreeterTest.kt", greeterTest(both = true))

        val result = fixture.build("testDebugUnitTest", "koverVerifyDebug", "spotlessCheck")

        assertThat(result.task(":testDebugUnitTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":koverVerifyDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":testReleaseUnitTest")).isNull()
        assertThat(dir.resolve("build/test-results/testDebugUnitTest/TEST-dev.ylp.sample.GreeterTest.xml")).exists()
    }

    @Test
    fun `code covered below 80 percent fails the coverage gate`() {
        fixture.file("src/test/kotlin/dev/ylp/sample/GreeterTest.kt", greeterTest(both = false))

        val result = fixture.fail("koverVerifyDebug")

        assertThat(result.output).contains("branches covered percentage is 50")
    }

    private fun greeterTest(both: Boolean): String =
        """
        package dev.ylp.sample

        import org.junit.jupiter.api.Assertions.assertEquals
        import org.junit.jupiter.api.Test

        class GreeterTest {
            @Test
            fun greetsByName() {
                assertEquals("Hello, Ada", greet("Ada"))
            }
        """.trimIndent() + "\n" +
            (
                if (both) {
                    """
                    |
                    |    @Test
                    |    fun greetsAnonymously() {
                    |        assertEquals("Hello", greet(""))
                    |    }
                    |
                    """.trimMargin()
                } else {
                    ""
                }
            ) + "}\n"
}
