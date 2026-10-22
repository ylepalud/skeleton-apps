package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OpenApiServerTest {
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
                id("openapi-server")
            }

            openApiServer {
                apiPackage = "dev.ylp.sample.api"
                modelPackage = "dev.ylp.sample.model"
            }
            """.trimIndent() + "\n",
        )
        fixture.file(
            "openapi.yaml",
            """
            openapi: 3.0.3
            info:
              title: Sample
              version: 1.0.0
            paths:
              /greetings/{name}:
                ${'$'}ref: "./greetings.yaml#/paths/greeting"
            """.trimIndent() + "\n",
        )
        fixture.file(
            "greetings.yaml",
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
                  required: [message]
                  properties:
                    message:
                      type: string
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `the contract generates interfaces and models that compile`() {
        val result = fixture.build("compileJava")

        assertThat(result.task(":openApiGenerate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(dir.resolve("build/generated/openapi/src/main/java/dev/ylp/sample/api/GreetingsApi.java")).exists()
        assertThat(dir.resolve("build/generated/openapi/src/main/java/dev/ylp/sample/model/GreetingDto.java")).exists()
    }

    @Test
    fun `a change in a referenced file regenerates the code`() {
        fixture.build("openApiGenerate")
        fixture.file(
            "greetings.yaml",
            dir
                .resolve("greetings.yaml")
                .readText()
                .replace("message:", "text:")
                .replace("[message]", "[text]"),
        )

        val result = fixture.build("openApiGenerate")

        assertThat(result.task(":openApiGenerate")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
