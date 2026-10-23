/*
 * Builds the OCI image of a JVM application with Jib, without a Dockerfile.
 * The base image is distroless and pinned by digest; it runs as a non-root
 * user. CI passes the registry repository and the commit SHA as the tag.
 */
import dev.ylp.buildlogic.dockerExecutable

plugins {
    java
    id("com.google.cloud.tools.jib")
}

jib {
    from {
        image = providers.gradleProperty("jibBaseImage").get()
    }
    to {
        image = providers.gradleProperty("imageRepository").getOrElse("${rootProject.name}/${project.name}")
        tags = setOf(providers.gradleProperty("imageTag").getOrElse("local"))
    }
    dockerClient {
        executable = dockerExecutable().get()
    }
    container {
        ports = listOf("8080", "8081")
        jvmFlags = listOf("-XX:MaxRAMPercentage=75")
        // CI passes the repository URL, which links the published image to it.
        providers.gradleProperty("imageSource").orNull?.let {
            labels = mapOf("org.opencontainers.image.source" to it)
        }
    }
}

// A Spring Boot application ships its production classpath only: developmentOnly
// dependencies (Docker Compose support) stay out of the image, as in the boot jar.
pluginManager.withPlugin("org.springframework.boot") {
    jib.configurationName = "productionRuntimeClasspath"
}

// Jib's tasks read the project model at execution time.
listOf("jib", "jibDockerBuild", "jibBuildTar").forEach { name ->
    tasks.named(name) {
        notCompatibleWithConfigurationCache("Jib does not support the configuration cache")
    }
}
