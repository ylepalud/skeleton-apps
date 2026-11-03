package dev.ylp.notes.cli

import dev.ylp.client.LocalStore
import dev.ylp.client.openLocalStore
import kotlin.random.Random

actual fun testLocalStore(): LocalStore = openLocalStore(testDirectory(), name = "test-${Random.nextLong()}.db")

/** A fresh folder under the system's temporary one. */
fun testDirectory(): String =
    "${environment("TMPDIR")?.trimEnd('/') ?: "/tmp"}/notes-cli-${Random.nextLong().toULong()}".also(::makeDirectories)
