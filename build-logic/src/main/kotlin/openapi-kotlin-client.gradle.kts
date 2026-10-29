/*
 * A Kotlin Multiplatform API client generated from an OpenAPI contract: Ktor
 * calls and kotlinx.serialization models in commonMain. The contract is the
 * source of truth, so the library stops compiling as soon as the code that
 * calls the client drifts from it.
 *
 * The generated code is internal: the library exposes its own model and keeps
 * the transport out of its public API. It is excluded from coverage.
 */
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("kotlin-multiplatform-convention")
    id("org.openapi.generator")
}

interface OpenApiKotlinClientExtension {
    /** Entry file of the contract; the files it references sit next to it. */
    val contract: RegularFileProperty

    /** Package of the generated client, excluded from coverage with its sub-packages. */
    val packageName: Property<String>
}

val openApiClient = extensions.create<OpenApiKotlinClientExtension>("openApiClient")

val libs = the<VersionCatalogsExtension>().named("libs")

fun library(alias: String) = libs.findLibrary(alias).get()

val generatedDir = layout.buildDirectory.dir("generated/openapi")

val openApiGenerate =
    tasks.named<GenerateTask>("openApiGenerate") {
        generatorName = "kotlin"
        library = "multiplatform"
        inputSpec = openApiClient.contract
        outputDir = generatedDir
        // Start from an empty folder, so a renamed or removed schema leaves no stale class behind.
        cleanupOutput = true
        packageName.set(openApiClient.packageName)
        generateApiTests = false
        generateModelTests = false
        generateApiDocumentation = false
        generateModelDocumentation = false
        configOptions =
            mapOf(
                // Internal declarations: callers go through the library's own API.
                "nonPublicApi" to "true",
                "omitGradleWrapper" to "true",
                // The only date library the multiplatform client supports.
                "dateLibrary" to "kotlinx-datetime",
            )
        // The multiplatform templates ignore nonPublicApi in a few files (auth, byte arrays):
        // make every top-level declaration internal, so none leaks into the library's API.
        val sources = generatedDir.map { it.dir("src/commonMain/kotlin").asFile }
        doLast {
            val topLevelPublic =
                Regex(
                    "^((?:open |data |sealed |abstract |enum )*(?:class|interface|object|fun|val|var|typealias) )",
                    RegexOption.MULTILINE,
                )
            sources
                .get()
                .walk()
                .filter { it.extension == "kt" }
                .forEach { file -> file.writeText(file.readText().replace(topLevelPublic, "internal $1")) }
        }
        // The entry file references sibling files: any contract file change regenerates.
        inputs.files(
            openApiClient.contract.map { contract ->
                fileTree(contract.asFile.parentFile) {
                    include("**/*.yaml", "**/*.yml", "**/*.json")
                    exclude("build")
                }
            },
        )
    }

kotlin {
    // The generated ApiClient makes safe calls on non-null maps; warnings are errors, and the
    // compiler cannot suppress them for one source folder only.
    compilerOptions {
        freeCompilerArgs.add("-Xwarning-level=UNNECESSARY_SAFE_CALL:disabled")
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(files(generatedDir.map { it.dir("src/commonMain/kotlin") }).builtBy(openApiGenerate))
            dependencies {
                api(library("ktor-client-core"))
                implementation(library("ktor-client-content-negotiation"))
                implementation(library("ktor-serialization-kotlinx-json"))
                implementation(library("kotlinx-serialization-json"))
            }
        }
        // One HTTP engine per platform, found by Ktor at runtime.
        androidMain.dependencies { implementation(library("ktor-client-okhttp")) }
        jvmMain.dependencies { implementation(library("ktor-client-okhttp")) }
        iosMain.dependencies { implementation(library("ktor-client-darwin")) }
        commonTest.dependencies { implementation(library("ktor-client-mock")) }
    }
}

kover {
    reports {
        filters {
            excludes {
                packages(openApiClient.packageName.map { listOf(it) })
            }
        }
    }
}
