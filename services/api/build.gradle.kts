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

/*
 * The domain model page of the documentation site, written by ModularityTest
 * from the module model. docsBuild and docsServe depend on it; the page is
 * declared as the output, so a deleted page is generated again.
 */
tasks.register<Test>("docsDomainModel") {
    group = "documentation"
    description = "Generates docs/architecture/domain-model.md from the modules and their jMolecules annotations."
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("*.ModularityTest.domainModelPage*") }
    // Each module's description is its package Javadoc, read from the sources.
    inputs.files(fileTree("src/main/java") { include("**/package-info.java") }).withPropertyName("packageInfos")
    val page = rootDir.resolve("docs/architecture/domain-model.md")
    systemProperty("domainModel.page", page.absolutePath)
    outputs.file(page).withPropertyName("page")
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}
