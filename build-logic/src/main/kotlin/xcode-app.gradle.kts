/*
 * An iOS app whose Xcode project XcodeGen generates from a committed project.yml, built and
 * tested by xcodebuild on a simulator. The app folder is not a Gradle project: the root build
 * applies this plugin to drive it.
 *
 * The tasks need macOS with Xcode. Elsewhere they are skipped, so a Linux machine still builds
 * and checks everything else.
 */
import groovy.json.JsonSlurper
import java.util.Locale
import javax.inject.Inject

plugins {
    base
}

interface XcodeAppExtension {
    /** The folder holding project.yml. */
    val projectDir: DirectoryProperty

    /** The scheme that builds the app and runs its tests. */
    val scheme: Property<String>

    /** The app's bundle identifier, which `iosRun` launches. */
    val bundleId: Property<String>

    /** The simulator the tests and the app run on, by name. */
    val simulator: Property<String>

    /** Build settings passed to xcodebuild, such as where the app finds its API. */
    val buildSettings: MapProperty<String, String>

    /** The line coverage of the app below which `iosTest` fails, in percent. */
    val minimumCoverage: Property<Int>

    /** Whether this machine has Xcode; detected, settable for tests of this plugin. */
    val xcodeAvailable: Property<Boolean>
}

val xcodeApp = extensions.create<XcodeAppExtension>("xcodeApp")

val isMacOs = providers.systemProperty("os.name").map { it.startsWith("Mac") }.get()

xcodeApp.simulator.convention(providers.gradleProperty("iosSimulator").orElse("iPhone 17"))
xcodeApp.minimumCoverage.convention(80)
// `xcodebuild -version` fails with the Command Line Tools alone: only a full Xcode passes.
xcodeApp.xcodeAvailable.convention(
    if (isMacOs) {
        providers
            .exec {
                commandLine("xcodebuild", "-version")
                isIgnoreExitValue = true
            }.result
            .map { it.exitValue == 0 }
    } else {
        providers.provider { false }
    },
)

val xcodeAvailable = xcodeApp.xcodeAvailable
val output = layout.buildDirectory.dir("ios")
val xcodeProject = xcodeApp.projectDir.zip(xcodeApp.scheme) { dir, scheme -> dir.dir("$scheme.xcodeproj") }
val destination = xcodeApp.simulator.map { "platform=iOS Simulator,name=$it" }
val settings = xcodeApp.buildSettings.map { map -> map.map { (key, value) -> "$key=$value" } }

// The app's own sources: the generated project and the build output are not.
val sources = xcodeApp.projectDir.map { dir -> dir.asFileTree.matching { exclude("build/**", "*.xcodeproj/**") } }

// Lambdas below read local copies: a script property would drag the script into the configuration cache.
tasks.withType<Exec>().configureEach {
    val available = xcodeAvailable
    onlyIf("Xcode is installed") { available.get() }
}

val generate =
    tasks.register<Exec>("iosGenerateProject") {
        group = "ios"
        description = "Generates the Xcode project from project.yml with XcodeGen."
        inputs.files(sources).withPropertyName("sources")
        outputs.dir(xcodeProject).withPropertyName("project")
        workingDir(xcodeApp.projectDir)
        commandLine("xcodegen", "generate", "--spec", "project.yml", "--quiet")
    }

tasks.register<Exec>("iosLint") {
    group = "ios"
    description = "Checks the Swift sources against swift-format's rules."
    val swiftSources = sources.map { it.matching { include("**/*.swift") } }
    inputs.files(swiftSources).withPropertyName("sources")
    workingDir(xcodeApp.projectDir)
    argumentProviders.add(
        CommandLineArgumentProvider {
            swiftSources
                .get()
                .files
                .map { it.absolutePath }
                .sorted()
        },
    )
    commandLine("xcrun", "swift-format", "lint", "--strict", "--parallel")
}

val resultBundle = output.map { it.dir("Test.xcresult") }

val test =
    tasks.register<Exec>("iosTest") {
        group = "ios"
        description = "Runs the unit and UI tests of the iOS app on a simulator."
        dependsOn(generate)
        outputs.dir(resultBundle).withPropertyName("results")
        // Xcode builds the shared module too: Gradle cannot see every input, so the tests always run.
        outputs.upToDateWhen { false }
        val results = resultBundle
        val project = xcodeProject
        val scheme = xcodeApp.scheme
        val target = destination
        val passed = settings
        // xcodebuild refuses to overwrite a result bundle.
        doFirst { results.get().asFile.deleteRecursively() }
        workingDir(xcodeApp.projectDir)
        argumentProviders.add(
            CommandLineArgumentProvider {
                listOf(
                    "-project",
                    project.get().asFile.name,
                    "-scheme",
                    scheme.get(),
                    "-destination",
                    target.get(),
                    "-derivedDataPath",
                    "build/DerivedData",
                    "-resultBundlePath",
                    results.get().asFile.absolutePath,
                ) + passed.get()
            },
        )
        commandLine("xcodebuild", "-quiet", "test")
    }

/** Fails when the app's line coverage, read from an xccov JSON report, is under the minimum. */
abstract class VerifyIosCoverage : DefaultTask() {
    @get:InputFile
    abstract val report: RegularFileProperty

    @get:Input
    abstract val target: Property<String>

    @get:Input
    abstract val minimum: Property<Int>

    @TaskAction
    fun verify() {
        val json = JsonSlurper().parse(report.get().asFile) as Map<*, *>
        val targets = json["targets"] as List<*>
        val app =
            targets.map { it as Map<*, *> }.firstOrNull { it["name"] == target.get() }
                ?: throw GradleException("No coverage for ${target.get()} in ${report.get().asFile}")
        val coverage = (app["lineCoverage"] as Number).toDouble() * 100
        logger.lifecycle("Line coverage of ${target.get()}: %.1f%%".format(Locale.ROOT, coverage))
        if (coverage < minimum.get()) {
            throw GradleException(
                "Line coverage of ${target.get()} is %.1f%%, under the minimum of ${minimum.get()}%%".format(Locale.ROOT, coverage),
            )
        }
    }
}

val coverageReport = output.map { it.file("coverage.json") }

/** Writes the coverage of a test run as JSON, read from its result bundle by xccov. */
abstract class XccovReport
    @Inject
    constructor(
        private val exec: ExecOperations,
    ) : DefaultTask() {
        @get:InputDirectory
        abstract val results: DirectoryProperty

        @get:OutputFile
        abstract val report: RegularFileProperty

        @TaskAction
        fun write() {
            report.get().asFile.outputStream().use { json ->
                exec.exec {
                    commandLine("xcrun", "xccov", "view", "--report", "--json", results.get().asFile.absolutePath)
                    standardOutput = json
                }
            }
        }
    }

val coverageReportTask =
    tasks.register<XccovReport>("iosCoverageReport") {
        group = "ios"
        description = "Writes the coverage of the last iOS test run as JSON."
        dependsOn(test)
        val available = xcodeAvailable
        onlyIf("Xcode is installed") { available.get() }
        results = resultBundle
        report = coverageReport
    }

val verifyCoverage =
    tasks.register<VerifyIosCoverage>("iosVerifyCoverage") {
        group = "ios"
        description = "Fails when the iOS app's line coverage is under the minimum."
        dependsOn(coverageReportTask)
        val available = xcodeAvailable
        onlyIf("Xcode is installed") { available.get() }
        this.report = coverageReport
        target = xcodeApp.scheme.map { "$it.app" }
        minimum = xcodeApp.minimumCoverage
    }

/** Builds the app, then installs and launches it on a simulator, booted first if needed. */
abstract class RunOnSimulator
    @Inject
    constructor(
        private val exec: ExecOperations,
    ) : DefaultTask() {
        @get:Internal
        abstract val projectDir: DirectoryProperty

        @get:Input
        abstract val buildArguments: ListProperty<String>

        @get:Input
        abstract val simulator: Property<String>

        @get:Input
        abstract val app: Property<String>

        @get:Input
        abstract val bundleId: Property<String>

        @TaskAction
        fun run() {
            exec.exec {
                workingDir(projectDir)
                commandLine(listOf("xcodebuild", "-quiet", "build") + buildArguments.get())
            }
            // Booting a simulator that already runs fails: that failure is ignored.
            exec.exec {
                commandLine("xcrun", "simctl", "boot", simulator.get())
                isIgnoreExitValue = true
            }
            exec.exec { commandLine("open", "-a", "Simulator") }
            exec.exec {
                commandLine(
                    "xcrun",
                    "simctl",
                    "install",
                    simulator.get(),
                    projectDir
                        .get()
                        .file(app.get())
                        .asFile.absolutePath,
                )
            }
            exec.exec { commandLine("xcrun", "simctl", "launch", simulator.get(), bundleId.get()) }
        }
    }

tasks.register<RunOnSimulator>("iosRun") {
    group = "ios"
    description = "Builds the app, then installs and launches it on the simulator."
    dependsOn(generate)
    val available = xcodeAvailable
    onlyIf("Xcode is installed") { available.get() }
    outputs.upToDateWhen { false }
    projectDir = xcodeApp.projectDir
    buildArguments =
        xcodeProject
            .zip(xcodeApp.scheme) { project, scheme ->
                listOf("-project", project.asFile.name, "-scheme", scheme, "-derivedDataPath", "build/DerivedData")
            }.zip(destination) { arguments, destination -> arguments + listOf("-destination", destination) }
            .zip(settings) { arguments, settings -> arguments + settings }
    simulator = xcodeApp.simulator
    app = xcodeApp.scheme.map { "build/DerivedData/Build/Products/Debug-iphonesimulator/$it.app" }
    bundleId = xcodeApp.bundleId
}

tasks.named("check") {
    dependsOn("iosLint", verifyCoverage)
}
