package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JavaConventionTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", JavaSources.javaProject("java-convention"))
    }

    @Test
    fun `conforming code passes check`() {
        fixture.file("src/main/java/dev/ylp/sample/Greeter.java", JavaSources.GREETER)
        fixture.file("src/test/java/dev/ylp/sample/GreeterTest.java", JavaSources.GREETER_TEST)

        val result = fixture.build("check")

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":spotlessCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `unformatted code fails spotless`() {
        fixture.file(
            "src/main/java/dev/ylp/sample/Greeter.java",
            "package dev.ylp.sample;\npublic final class Greeter { public static String greet(String n){return n;} }\n",
        )

        val result = fixture.fail("spotlessCheck")

        assertThat(result.task(":spotlessJavaCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("format violations")
    }

    @Test
    fun `returning null from a non-nullable method fails compilation`() {
        fixture.file(
            "src/main/java/dev/ylp/sample/Finder.java",
            """
            package dev.ylp.sample;

            public final class Finder {
              public static String find() {
                return null;
              }
            }
            """.trimIndent(),
        )

        val result = fixture.fail("compileJava")

        assertThat(result.output).contains("[NullAway]")
    }

    @Test
    fun `an error prone bug pattern fails compilation`() {
        fixture.file(
            "src/main/java/dev/ylp/sample/Guard.java",
            """
            package dev.ylp.sample;

            public final class Guard {
              public static void check(boolean valid) {
                if (!valid) {
                  new IllegalStateException("invalid");
                }
              }
            }
            """.trimIndent(),
        )

        val result = fixture.fail("compileJava")

        assertThat(result.output).contains("[DeadException]")
    }
}
