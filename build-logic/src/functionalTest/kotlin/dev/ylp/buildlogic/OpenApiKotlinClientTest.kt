package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OpenApiKotlinClientTest {
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
                id("openapi-kotlin-client")
            }

            openApiClient {
                contract = layout.projectDirectory.file("contract/openapi.yaml")
                packageName = "dev.ylp.sample.generated"
            }
            """.trimIndent() + "\n",
        )
        fixture.file(
            "contract/openapi.yaml",
            """
            openapi: 3.0.3
            info:
              title: Sample
              version: 1.0.0
            security:
              - bearerAuth: []
            paths:
              /greetings/{name}:
                ${'$'}ref: "./greetings.yaml#/paths/greeting"
            components:
              securitySchemes:
                bearerAuth:
                  type: http
                  scheme: bearer
            """.trimIndent() + "\n",
        )
        fixture.file(
            "contract/greetings.yaml",
            """
            paths:
              greeting:
                get:
                  tags: [greetings]
                  operationId: greet
                  parameters:
                    - name: name
                      in: path
                      required: true
                      schema:
                        type: string
                  responses:
                    "200":
                      description: The greeting.
                      content:
                        application/json:
                          schema:
                            ${'$'}ref: "#/components/schemas/Greeting"
            components:
              schemas:
                Greeting:
                  type: object
                  required: [message, sentAt]
                  properties:
                    message:
                      type: string
                    sentAt:
                      type: string
                      format: date-time
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `the contract generates an internal client that compiles`() {
        val result = fixture.build("compileKotlinJvm")

        assertThat(result.task(":openApiGenerate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":compileKotlinJvm")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val generated = dir.resolve("build/generated/openapi/src/commonMain/kotlin/dev/ylp/sample/generated")
        assertThat(generated.resolve("apis/GreetingsApi.kt")).exists()
        val topLevelPublic =
            Regex("^(?:open |data |sealed |abstract |enum )*(?:class|interface|object|fun|val|var|typealias) ", RegexOption.MULTILINE)
        val leaks =
            generated
                .walk()
                .filter {
                    it.extension == "kt" &&
                        topLevelPublic.containsMatchIn(
                            it.readText(),
                        )
                }.map { it.name }
                .toList()
        assertThat(leaks).isEmpty()
    }

    @Test
    fun `the library calls the generated client from its own code`() {
        fixture.file(
            "src/commonMain/kotlin/dev/ylp/sample/Greetings.kt",
            """
            package dev.ylp.sample

            import dev.ylp.sample.generated.apis.GreetingsApi

            class Greetings internal constructor(
                private val api: GreetingsApi,
            ) {
                suspend fun greet(name: String): String = api.greet(name).body().message
            }
            """.trimIndent() + "\n",
        )

        val result = fixture.build("compileKotlinJvm")

        assertThat(result.task(":compileKotlinJvm")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `a change in a referenced file regenerates the client`() {
        fixture.build("openApiGenerate")
        fixture.file("contract/greetings.yaml", dir.resolve("contract/greetings.yaml").readText().replace("message", "text"))

        val result = fixture.build("openApiGenerate")

        assertThat(result.task(":openApiGenerate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
