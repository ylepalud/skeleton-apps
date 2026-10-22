package dev.ylp.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File

/**
 * The docker binary, resolved from the PATH of the build's caller. A Gradle daemon may have been
 * started with a minimal PATH that does not include it, and processes it launches resolve
 * executables against that PATH, not the caller's.
 */
fun Project.dockerExecutable(): Provider<String> =
    providers
        .environmentVariable("PATH")
        .map { path ->
            path
                .split(File.pathSeparator)
                .map { File(it, "docker") }
                .firstOrNull { it.canExecute() }
                ?.absolutePath ?: "docker"
        }.orElse("docker")
