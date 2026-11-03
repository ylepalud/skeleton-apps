package dev.ylp.client

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.ylp.client.store.db.ClientDatabase

/** Opens the local store in [directory], which must exist: the command line keeps it in the user's data folder. */
fun openLocalStore(
    directory: String,
    name: String = DEFAULT_STORE_NAME,
): LocalStore =
    LocalStore(
        NativeSqliteDriver(ClientDatabase.Schema, name, onConfiguration = { configuration ->
            configuration.copy(extendedConfig = configuration.extendedConfig.copy(basePath = directory))
        }),
    )
