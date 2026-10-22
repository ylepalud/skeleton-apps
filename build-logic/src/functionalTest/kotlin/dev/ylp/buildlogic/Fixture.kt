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
        settings()
    }

    fun settings(vararg includes: String) {
        val catalog = conventionsRoot.resolve("gradle/libs.versions.toml").invariantSeparatorsPath
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
                versionCatalogs {
                    create("libs") {
                        from(files("$catalog"))
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
