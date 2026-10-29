package dev.ylp.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

/**
 * A throwaway Gradle build that applies the convention plugins under test.
 *
 * It shares the repository's version catalog and gradle.properties, so the
 * plugins read the same versions as the real build, configuration cache
 * included.
 */
class Fixture(
    private val dir: File,
) {
    private val conventionsRoot = File(System.getProperty("conventions.rootDir"))

    init {
        conventionsRoot.resolve("gradle.properties").copyTo(dir.resolve("gradle.properties"))
        // Copied where Gradle imports it by convention as the `libs` catalog: referencing it by
        // absolute path would break the fixture's own Spotless check on a long checkout path.
        conventionsRoot.resolve("gradle/libs.versions.toml").copyTo(dir.resolve("gradle/libs.versions.toml"))
        settings()
    }

    fun settings(vararg includes: String) {
        file(
            "settings.gradle.kts",
            """
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
            }

            dependencyResolutionManagement {
                repositoriesMode = RepositoriesMode.PREFER_SETTINGS
                repositories {
                    mavenCentral()
                    google {
                        content {
                            includeGroupByRegex("com\\.android.*")
                            includeGroupByRegex("com\\.google.*")
                            includeGroupByRegex("androidx.*")
                        }
                    }
                    exclusiveContent {
                        forRepository {
                            ivy("https://nodejs.org/dist") {
                                patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                                metadataSources { artifact() }
                            }
                        }
                        filter { includeModule("org.nodejs", "node") }
                    }
                }
            }

            rootProject.name = "fixture"
            """.trimIndent() + "\n" +
                includes.joinToString("") {
                    "include(\"$it\")\n"
                },
        )
    }

    fun file(
        path: String,
        content: String,
    ): File =
        dir.resolve(path).apply {
            parentFile.mkdirs()
            writeText(content)
        }

    fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    /** Runs the build with the given environment, as a caller with that environment would. */
    fun build(
        vararg arguments: String,
        environment: Map<String, String>,
    ): BuildResult = runner(*arguments).withEnvironment(environment).build()

    fun fail(vararg arguments: String): BuildResult = runner(*arguments).buildAndFail()

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            // Tasks must really run: a cache hit from a previous fixture proves nothing.
            .withArguments(*arguments, "--no-build-cache", "--stacktrace")
            .forwardOutput()
}
