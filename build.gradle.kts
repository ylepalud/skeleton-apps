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

val composeUp =
    tasks.register<Exec>("composeUp") {
        description = "Starts the compose stack with the application images."
        dependsOn(":services:api:jibDockerBuild", ":services:api-app:dockerBuild")
        commandLine("docker", "compose", "--profile", "app", "up", "--detach", "--wait")
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
    commandLine("docker", "compose", "--profile", "app", "down")
}

tasks.register<Exec>("resetDatabase") {
    group = "local-dev"
    description = "Stops the stack and deletes its database; the next start migrates an empty one."
    commandLine("docker", "compose", "--profile", "app", "down", "--volumes")
}
