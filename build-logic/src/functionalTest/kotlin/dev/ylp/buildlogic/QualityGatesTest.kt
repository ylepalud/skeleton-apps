package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class QualityGatesTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", JavaSources.javaProject("quality-gates"))
    }

    @Test
    fun `integration tests run as part of check`() {
        fixture.file("src/main/java/dev/ylp/sample/Greeter.java", JavaSources.GREETER)
        fixture.file("src/integrationTest/java/dev/ylp/sample/GreeterTest.java", JavaSources.GREETER_TEST)

        val result = fixture.build("check")

        assertThat(result.task(":integrationTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(dir.resolve("build/test-results/integrationTest/TEST-dev.ylp.sample.GreeterTest.xml")).exists()
    }

    @Test
    fun `integration tests reuse unit test sources`() {
        fixture.file("src/main/java/dev/ylp/sample/Greeter.java", JavaSources.GREETER)
        fixture.file("src/test/java/dev/ylp/sample/GreeterTest.java", JavaSources.GREETER_TEST)
        fixture.file(
            "src/integrationTest/java/dev/ylp/sample/GreeterIntegrationTest.java",
            """
            package dev.ylp.sample;

            class GreeterIntegrationTest extends GreeterTest {}
            """.trimIndent() + "\n",
        )

        val result = fixture.build("integrationTest")

        assertThat(result.task(":integrationTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(
            dir.resolve("build/test-results/integrationTest/TEST-dev.ylp.sample.GreeterIntegrationTest.xml"),
        ).exists()
    }

    @Test
    fun `fully covered code passes the coverage gate`() {
        fixture.file("src/main/java/dev/ylp/sample/Greeter.java", JavaSources.GREETER)
        fixture.file("src/test/java/dev/ylp/sample/GreeterTest.java", JavaSources.GREETER_TEST)

        val result = fixture.build("check")

        assertThat(result.task(":jacocoTestCoverageVerification")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `insufficiently covered code fails the coverage gate`() {
        fixture.file(
            "src/main/java/dev/ylp/sample/Greeter.java",
            """
            package dev.ylp.sample;

            public final class Greeter {
              private Greeter() {}

              public static String greet(String name) {
                return "Hello " + name;
              }

              public static String farewell(String name) {
                StringBuilder message = new StringBuilder("Goodbye");
                message.append(' ').append(name).append('!');
                return message.toString();
              }
            }
            """.trimIndent() + "\n",
        )
        fixture.file("src/test/java/dev/ylp/sample/GreeterTest.java", JavaSources.GREETER_TEST)

        val result = fixture.fail("check")

        assertThat(result.task(":jacocoTestCoverageVerification")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Rule violated")
    }

    @Test
    fun `mutation testing stays out of check`() {
        val result = fixture.build("check", "--dry-run")

        assertThat(result.output).contains(":jacocoTestCoverageVerification").doesNotContain(":pitest")
    }
}
