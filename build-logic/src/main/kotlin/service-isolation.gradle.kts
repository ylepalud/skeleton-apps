/*
 * Keeps deployables independent: a service never depends on another service's
 * build output. Services talk through their published HTTP contracts, never
 * through the build graph.
 */
plugins {
    base
}

val servicePathPrefix = ":services:"
val servicePath = path

// Collected at configuration time so the task holds plain strings only, which
// keeps it compatible with the configuration cache.
val violations =
    provider {
        configurations
            .filter { it.isCanBeDeclared }
            .flatMap { configuration ->
                configuration.dependencies
                    .withType<ProjectDependency>()
                    .filter { it.path.startsWith(servicePathPrefix) && it.path != servicePath }
                    .map { "${configuration.name} -> ${it.path}" }
            }.sorted()
    }

val verifyServiceIsolation =
    tasks.register("verifyServiceIsolation") {
        group = "verification"
        description = "Fails when this service declares a build dependency on another service."
        // Task-local copies: the action must not capture the build script.
        val found = violations
        val service = servicePath
        inputs.property("violations", found)
        doLast {
            val list = found.get()
            if (list.isNotEmpty()) {
                throw GradleException(
                    "$service depends on another service:\n" + list.joinToString("\n") { "  - $it" },
                )
            }
        }
    }

tasks.named("check") {
    dependsOn(verifyServiceIsolation)
}
