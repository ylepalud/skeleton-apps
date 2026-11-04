package dev.ylp.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AgentCliCheckTest {
    @TempDir
    lateinit var dir: File
    private lateinit var fixture: Fixture

    @BeforeEach
    fun setUp() {
        fixture = Fixture(dir)
        fixture.file("build.gradle.kts", "plugins {\n    id(\"agent-cli-check\")\n}\n")
        fixture.file("bin/agent-cli.test.mjs", "import test from 'node:test';\ntest('passes', () => {});\n")
    }

    @Test
    fun `check runs the agent CLI's tests, with the configuration cache`() {
        val result = fixture.build("check", "--dry-run")

        assertThat(result.output).contains(":agentCliTest SKIPPED").contains("Configuration cache entry stored")
    }
}
