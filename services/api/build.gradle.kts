plugins {
    id("spring")
    id("container-image")
}

dependencies {
    implementation(project(":contracts:api"))
}

springBoot {
    // Exposed by the info endpoint. No build time, so the build stays reproducible and cacheable.
    buildInfo {
        excludes = setOf("time")
    }
}
