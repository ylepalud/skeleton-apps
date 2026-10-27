import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

plugins {
    base
}

// The convention plugins are a product of this repository: their functional
// tests run with every root `check`.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

/*
 * Local stack (compose.yaml, profile "app"): the images that go to the cluster,
 * run together. Starting is idempotent: after a change, starting again rebuilds
 * the images and recreates only the containers whose image changed.
 */

/** Polls a URL until it answers 200, so that the stack is usable when the task ends. */
abstract class WaitForReadiness : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val maxWait: Property<Duration>

    @TaskAction
    fun waitForReadiness() {
        val deadline = System.nanoTime() + maxWait.get().toNanos()
        val request = HttpRequest.newBuilder(URI.create(url.get())).timeout(Duration.ofSeconds(2)).build()
        HttpClient.newHttpClient().use { client ->
            while (System.nanoTime() < deadline) {
                val status = runCatching { client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() }
                if (status.getOrNull() == 200) {
                    return
                }
                Thread.sleep(1_000)
            }
        }
        throw GradleException("${url.get()} did not answer 200 within ${maxWait.get().seconds} s")
    }
}

// The docker binary, resolved from the caller's PATH (as dev.ylp.buildlogic.dockerExecutable
// in build-logic, which this script does not see): a long-lived Gradle daemon
// may have been started with a PATH that does not include it.
val docker: String =
    providers
        .environmentVariable("PATH")
        .map { path ->
            path
                .split(File.pathSeparator)
                .map { File(it, "docker") }
                .firstOrNull { it.canExecute() }
                ?.absolutePath ?: "docker"
        }.getOrElse("docker")

val composeUp =
    tasks.register<Exec>("composeUp") {
        description = "Starts the compose stack with the application images."
        dependsOn(":services:api:jibDockerBuild", ":services:api-app:dockerBuild")
        commandLine(docker, "compose", "--profile", "app", "up", "--detach", "--wait")
    }

tasks.register<WaitForReadiness>("startLocalStack") {
    group = "local-dev"
    description = "Builds the images and starts the whole stack: web app and API on http://localhost:8088."
    dependsOn(composeUp)
    url = "http://localhost:8091/actuator/health/readiness"
    maxWait = Duration.ofMinutes(2)
    doLast { println("Local stack ready: http://localhost:8088 (identity provider: http://localhost:8090)") }
}

tasks.register<Exec>("stopLocalStack") {
    group = "local-dev"
    description = "Stops the stack; the database volume is kept."
    commandLine(docker, "compose", "--profile", "app", "down")
}

tasks.register<Exec>("resetDatabase") {
    group = "local-dev"
    description = "Stops the stack and deletes its database; the next start migrates an empty one."
    commandLine(docker, "compose", "--profile", "app", "down", "--volumes")
}

/*
 * Documentation site (mkdocs.yml): docs/ built by Zensical, with an API reference
 * rendered from the contract. Every tool runs in a container pinned in
 * gradle.properties, so Docker is the only prerequisite. None of these tasks is
 * part of `check`: CI runs them in its own `docs` job.
 */

// Containers run as the caller, so that what they write in build/ stays theirs.
val hostUser: Provider<String> =
    providers
        .exec { commandLine("id", "-u") }
        .standardOutput.asText
        .zip(providers.exec { commandLine("id", "-g") }.standardOutput.asText) { uid, gid -> "${uid.trim()}:${gid.trim()}" }

/** `docker run` of a pinned image on the repository, mounted at /work. */
fun dockerRun(
    image: String,
    vararg arguments: String,
    options: List<String> = emptyList(),
): List<String> =
    listOf(docker, "run", "--rm", "--user", hostUser.get(), "-v", "${rootDir.absolutePath}:/work", "-w", "/work") +
        options + providers.gradleProperty(image).get() + arguments

val docsApiReference =
    tasks.register<Exec>("docsApiReference") {
        group = "documentation"
        description = "Renders the API reference from the contract into docs/product/api-reference.html."
        // The YAML files only: the contract project's build/ is another task's output.
        inputs.files(fileTree("contracts/api") { include("*.yaml") }).withPropertyName("contract")
        outputs.file("docs/product/api-reference.html")
        // --inlineBundle: the page loads no script from a CDN.
        commandLine(
            dockerRun(
                "docsApiReferenceImage",
                "build-docs",
                "contracts/api/openapi.yaml",
                "--inlineBundle",
                "--title",
                "API reference",
                "-o",
                "docs/product/api-reference.html",
            ),
        )
    }

tasks.register<Exec>("docsBuild") {
    group = "documentation"
    description = "Builds the documentation site into build/docs/site; a broken link or anchor fails it."
    dependsOn(docsApiReference, ":services:api:docsDomainModel")
    // SITE_URL, when set, is the address the site is published at.
    val siteUrl = providers.environmentVariable("SITE_URL").map { listOf("-e", "SITE_URL=$it") }.getOrElse(emptyList())
    commandLine(dockerRun("docsSiteImage", "build", "--strict", "--clean", options = siteUrl))
}

tasks.register<Exec>("docsServe") {
    group = "documentation"
    description = "Serves the documentation site on http://localhost:8000, rebuilt on every change."
    dependsOn(docsApiReference, ":services:api:docsDomainModel")
    commandLine(dockerRun("docsSiteImage", "serve", "--dev-addr", "0.0.0.0:8000", options = listOf("-p", "8000:8000")))
}

tasks.register<Exec>("docsLint") {
    group = "documentation"
    description = "Lints the Markdown of the README and docs/ (.markdownlint-cli2.jsonc)."
    commandLine(dockerRun("docsLintImage"))
}
