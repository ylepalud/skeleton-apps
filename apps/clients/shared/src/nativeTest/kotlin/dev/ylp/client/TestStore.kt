package dev.ylp.client

import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.ylp.client.store.db.ClientDatabase

actual fun testStore(): LocalStore = LocalStore(inMemoryDriver(ClientDatabase.Schema))
