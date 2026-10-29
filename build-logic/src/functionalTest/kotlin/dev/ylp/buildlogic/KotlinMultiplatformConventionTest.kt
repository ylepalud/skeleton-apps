package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinMultiplatformConventionTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", "plugins {\n    id(\"kotlin-multiplatform-convention\")\n}\n")
        fixture.file(
            "src/commonMain/kotlin/dev/ylp/sample/Greeter.kt",
            """
            package dev.ylp.sample

            fun greet(name: String): String = if (name.isEmpty()) "Hello" else "Hello, ${'$'}name"
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `tested code passes the JVM tests, the format and the coverage gate`() {
        fixture.file("src/commonTest/kotlin/dev/ylp/sample/GreeterTest.kt", greeterTest(both = true))

        val result = fixture.build("jvmTest", "spotlessCheck", "koverVerify")

        assertThat(result.task(":jvmTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":spotlessCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":koverVerify")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `code covered below 80 percent fails the coverage gate`() {
        fixture.file("src/commonTest/kotlin/dev/ylp/sample/GreeterTest.kt", greeterTest(both = false))

        val result = fixture.fail("koverVerify")

        assertThat(result.output).contains("branches covered percentage is 50")
    }

    @Test
    fun `a compiler warning fails the build`() {
        fixture.file(
            "src/commonMain/kotlin/dev/ylp/sample/Unused.kt",
            """
            package dev.ylp.sample

            fun unused(value: String?): String = value!!.trim()!!
            """.trimIndent() + "\n",
        )

        val result = fixture.fail("compileKotlinJvm")

        assertThat(result.output).contains("warnings found and -Werror specified")
    }

    private fun greeterTest(both: Boolean): String =
        """
        package dev.ylp.sample

        import kotlin.test.Test
        import kotlin.test.assertEquals

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
