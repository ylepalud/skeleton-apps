package dev.ylp.notes.cli

import dev.ylp.client.LocalStore
import dev.ylp.client.openLocalStore

actual fun testLocalStore(): LocalStore = openLocalStore()
