/*
 * A Bruno collection that mirrors an OpenAPI contract. Its requests are
 * generated from the contract by the project's `collection` npm scripts;
 * `check` fails as soon as the committed requests drift from the contract,
 * and `updateCollection` rewrites them. Hand-written files (the collection
 * itself, with its auth, and the environments) are never touched.
 *
 * Expected npm scripts: collection:check <contract>, collection:write <contract>.
 */
import com.github.gradle.node.npm.task.NpmTask

plugins {
    base
    id("com.github.node-gradle.node")
}

interface BrunoCollectionExtension {
    /** Entry file of the contract the requests are generated from. */
    val contract: RegularFileProperty
}

val collection = extensions.create<BrunoCollectionExtension>("brunoCollection")
collection.contract.convention(rootProject.layout.projectDirectory.file("apps/contracts/api/openapi.yaml"))

node {
    version = providers.gradleProperty("nodeToolchainVersion")
    download = true
    workDir = layout.buildDirectory.dir("node")
    npmWorkDir = layout.buildDirectory.dir("npm")
    npmInstallCommand = "ci"
}

val npmInstall = tasks.named("npmInstall")

// The contract may be split into $ref files next to its entry file; the
// folder may also hold a Gradle project's build output, hence a file tree.
val contractFiles =
    collection.contract.map { contract ->
        fileTree(contract.asFile.parentFile) {
            include("**/*.yaml", "**/*.yml", "**/*.json")
            exclude("build")
        }
    }

val collectionFiles =
    fileTree(layout.projectDirectory) {
        include("**/*.yml", "scripts/**", "package.json", "package-lock.json")
        exclude("node_modules", "build", ".gradle")
    }

val verifyCollection =
    tasks.register<NpmTask>("verifyCollection") {
        group = "verification"
        description = "Fails when the committed requests differ from those generated from the contract."
        dependsOn(npmInstall)
        args = collection.contract.map { listOf("run", "collection:check", "--", it.asFile.absolutePath) }
        inputs.files(contractFiles)
        inputs.files(collectionFiles)
        // A stamp makes the check up to date until the contract or the collection changes.
        val stamp = layout.buildDirectory.file("collection-verified")
        outputs.file(stamp)
        doLast { stamp.get().asFile.writeText("verified\n") }
    }

tasks.register<NpmTask>("updateCollection") {
    group = "build"
    description = "Rewrites the generated requests from the contract."
    dependsOn(npmInstall)
    args = collection.contract.map { listOf("run", "collection:write", "--", it.asFile.absolutePath) }
}

tasks.named("check") {
    dependsOn(verifyCollection)
}

tasks.named<Delete>("clean") {
    delete("node_modules")
}
