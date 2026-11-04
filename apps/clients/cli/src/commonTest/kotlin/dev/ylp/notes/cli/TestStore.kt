package dev.ylp.notes.cli

import dev.ylp.client.LocalStore

/** An empty local store, on the platform the tests run on. */
expect fun testLocalStore(): LocalStore
