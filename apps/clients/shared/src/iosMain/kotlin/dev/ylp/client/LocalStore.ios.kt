package dev.ylp.client

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseFileContext
import dev.ylp.client.store.db.ClientDatabase

/** Opens the local store in the app's Application Support folder. */
fun openLocalStore(name: String = DEFAULT_STORE_NAME): LocalStore = LocalStore(NativeSqliteDriver(ClientDatabase.Schema, name))

/** Deletes the local store, as a reinstall would: tests start from a fresh device with it. */
fun deleteLocalStore(name: String = DEFAULT_STORE_NAME) {
    DatabaseFileContext.deleteDatabase(name)
}

const val DEFAULT_STORE_NAME = "client.db"
