plugins {
    id("frontend-convention")
}

// The image of the web app for local runs; CI builds and pushes it with buildx.
tasks.register<Exec>("dockerBuild") {
    group = "build"
    description = "Builds the nginx image of the web app, tagged apps/api-app:<imageTag>."
    dependsOn("npmRunBuild")
    val tag = providers.gradleProperty("imageTag").orElse("local")
    inputs.files("Dockerfile", "nginx/default.conf.template")
    inputs.dir("dist/api-app/browser")
    commandLine("docker", "build", "--tag", "apps/api-app:${tag.get()}", ".")
}
