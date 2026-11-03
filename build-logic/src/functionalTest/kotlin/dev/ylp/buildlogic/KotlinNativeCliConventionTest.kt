package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinNativeCliConventionTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", "plugins {\n    id(\"kotlin-native-cli-convention\")\n}\n")
        fixture.file(
            "src/commonMain/kotlin/dev/ylp/sample/Greeter.kt",
            """
            package dev.ylp.sample

            fun greet(name: String): String = if (name.isEmpty()) "Hello" else "Hello, ${'$'}name"
            """.trimIndent() + "\n",
        )
        fixture.file(
            "src/nativeMain/kotlin/Main.kt",
            """
            import dev.ylp.sample.greet

            fun main(args: Array<String>) = println(greet(args.firstOrNull().orEmpty()))
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `the common code is tested and measured on the JVM`() {
        fixture.file(
            "src/commonTest/kotlin/dev/ylp/sample/GreeterTest.kt",
            """
            package dev.ylp.sample

            import kotlin.test.Test
            import kotlin.test.assertEquals

            class GreeterTest {
                @Test
                fun greets() {
                    assertEquals("Hello, Ada", greet("Ada"))
                    assertEquals("Hello", greet(""))
                }
            }
            """.trimIndent() + "\n",
        )

        val result = fixture.build("jvmTest", "koverVerify", "spotlessCheck")

        assertThat(result.task(":jvmTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":koverVerify")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `untested code fails the coverage gate`() {
        val result = fixture.fail("koverVerify")

        assertThat(result.output).contains("covered percentage is 0")
    }

    @Test
    fun `an executable is built for macOS and Linux, the Linux one on a Linux host only`() {
        val tasks = fixture.build("tasks", "--all").output
        assertThat(tasks).contains("linkReleaseExecutableMacosArm64", "linkReleaseExecutableLinuxX64")

        // Linking is left to the CLI job on Linux: here, the task must skip elsewhere.
        if (!System.getProperty("os.name").startsWith("Linux")) {
            assertThat(fixture.build("linkDebugExecutableLinuxX64").task(":linkDebugExecutableLinuxX64")?.outcome)
                .isEqualTo(TaskOutcome.SKIPPED)
        }
    }
}
