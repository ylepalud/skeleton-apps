/*
 * The agent CLI (bin/agent-cli): its unit tests, then its selftest against the
 * repository, so that a change moving a file the CLI reads fails in its own
 * pull request. Never up to date: the selftest reads files no input declares.
 *
 * Applied by the root build, on the same pinned Node toolchain as the web app,
 * whichever clients a project keeps.
 */
import com.github.gradle.node.task.NodeTask

plugins {
    base
    id("com.github.node-gradle.node")
}

node {
    version = providers.gradleProperty("nodeToolchainVersion")
    download = true
    workDir = layout.buildDirectory.dir("node")
}

val agentCliTest =
    tasks.register<NodeTask>("agentCliTest") {
        group = "verification"
        description = "Runs the agent CLI's unit tests and its selftest (bin/agent-cli.test.mjs)."
        options = listOf("--test")
        script = layout.projectDirectory.file("bin/agent-cli.test.mjs")
        outputs.upToDateWhen { false }
    }

tasks.named("check") { dependsOn(agentCliTest) }
