import dev.ylp.buildlogic.dockerExecutable

plugins {
    id("frontend-convention")
}

// The image of the web app for local runs; CI builds and pushes it with buildx.
tasks.register<Exec>("dockerBuild") {
    group = "build"
    description = "Builds the nginx image of the web app, tagged <root project>/api-app:<imageTag>."
    dependsOn("npmRunBuild")
    val tag = providers.gradleProperty("imageTag").orElse("local")
    val repository = "${rootProject.name}/api-app"
    inputs.files("Dockerfile", "nginx/default.conf.template")
    inputs.dir("dist/api-app/browser")
    commandLine(dockerExecutable().get(), "build", "--tag", "$repository:${tag.get()}", ".")
}
