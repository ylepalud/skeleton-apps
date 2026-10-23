/*
 * Coverage gate enforced through `check`, and mutation testing run on demand
 * (nightly in CI). Entry points and generated mappers carry no business logic
 * and are left out of the coverage measurement.
 */
plugins {
    id("integration-test")
    jacoco
    id("info.solidsoft.pitest")
}

val nonBusinessClasses = listOf("**/*Application.class", "**/*MapperImpl.class")
val minimumCoverageRatio = "0.8".toBigDecimal()

jacoco {
    toolVersion = providers.gradleProperty("jacocoVersion").get()
}

fun JacocoReportBase.coverBusinessClassesOfAllSuites() {
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    sourceSets(project.sourceSets["main"])
    classDirectories.setFrom(
        project.sourceSets["main"]
            .output.classesDirs
            .map { fileTree(it).exclude(nonBusinessClasses) },
    )
}

val jacocoTestReport =
    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"), tasks.named("integrationTest"))
        coverBusinessClassesOfAllSuites()
        reports {
            html.required = true
            xml.required = true
        }
    }

val jacocoTestCoverageVerification =
    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(jacocoTestReport)
        coverBusinessClassesOfAllSuites()
        violationRules {
            rule {
                listOf("INSTRUCTION", "BRANCH", "LINE").forEach { metric ->
                    limit {
                        counter = metric
                        value = "COVEREDRATIO"
                        minimum = minimumCoverageRatio
                    }
                }
            }
        }
    }

tasks.named("check") {
    dependsOn(jacocoTestCoverageVerification)
}

// Mutation testing targets the domain and application layers of every module:
// adapters are covered by integration tests instead.
pitest {
    pitestVersion = providers.gradleProperty("pitestVersion")
    junit5PluginVersion = providers.gradleProperty("pitestJunit5PluginVersion")
    targetClasses = listOf("${project.group}.*.domain.*", "${project.group}.*.application.*")
    testSourceSets = listOf(sourceSets["test"], sourceSets["integrationTest"])
    threads = 2
    outputFormats = listOf("HTML", "XML")
    timestampedReports = false
    mutationThreshold = 80
}
