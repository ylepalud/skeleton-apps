/*
 * Lifecycle of an npm-based frontend, governed like any other module: the
 * pinned Node toolchain is downloaded by the build, and the npm scripts are
 * wired into the standard clean/assemble/check lifecycle.
 *
 * Expected npm scripts: generate:api, lint, test, build, e2e.
 */
import com.github.gradle.node.npm.task.NpmTask

plugins {
    base
    id("com.github.node-gradle.node")
}

interface FrontendExtension {
    /** Entry file of the OpenAPI contract the API client is generated from. */
    val apiContract: RegularFileProperty
}

val frontend = extensions.create<FrontendExtension>("frontend")
frontend.apiContract.convention(rootProject.layout.projectDirectory.file("contracts/api/openapi.yaml"))

node {
    version = providers.gradleProperty("nodeToolchainVersion")
    download = true
    workDir = layout.buildDirectory.dir("node")
    npmWorkDir = layout.buildDirectory.dir("npm")
    // Install exactly what package-lock.json pins, failing if it is out of date.
    npmInstallCommand = "ci"
}

val npmInstall = tasks.named("npmInstall")

val generateApi =
    tasks.register<NpmTask>("generateApi") {
        group = "build"
        description = "Generates the TypeScript API client from the OpenAPI contract."
        dependsOn(npmInstall)
        args = listOf("run", "generate:api")
        // The contract may be split into $ref files next to its entry file. A file tree, not the
        // whole folder, which may also hold a Gradle project's build output.
        inputs.files(
            frontend.apiContract.map { contract ->
                fileTree(contract.asFile.parentFile) {
                    include("**/*.yaml", "**/*.yml", "**/*.json")
                    exclude("build")
                }
            },
        )
        inputs.files("package.json", "package-lock.json").optional()
        outputs.dir("src/app/api/generated")
    }

val npmRunLint =
    tasks.register<NpmTask>("npmRunLint") {
        group = "verification"
        description = "Lints the frontend sources."
        dependsOn(npmInstall, generateApi)
        args = listOf("run", "lint")
    }

val npmRunTest =
    tasks.register<NpmTask>("npmRunTest") {
        group = "verification"
        description = "Runs the frontend unit and component tests."
        dependsOn(npmInstall, generateApi)
        args = listOf("run", "test")
    }

val npmRunBuild =
    tasks.register<NpmTask>("npmRunBuild") {
        group = "build"
        description = "Builds the production bundle."
        dependsOn(npmInstall, generateApi)
        args = listOf("run", "build")
    }

// Browser end-to-end tests stay out of the default lifecycle and run with
// -PrunE2e=true, as CI does.
tasks.register<NpmTask>("npmRunE2e") {
    group = "verification"
    description = "Runs the Playwright end-to-end suite (enabled with -PrunE2e=true)."
    dependsOn(npmInstall, npmRunBuild)
    args = listOf("run", "e2e")
    // Task-local: the condition must not capture the build script.
    val runE2e = providers.gradleProperty("runE2e").map { it.toBoolean() }.orElse(false)
    onlyIf("runE2e is set") { runE2e.get() }
}

tasks.named("assemble") {
    dependsOn(npmRunBuild)
}

tasks.named("check") {
    dependsOn(npmRunLint, npmRunTest)
}

tasks.named<Delete>("clean") {
    delete("node_modules")
}
