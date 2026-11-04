package dev.ylp.client

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.ylp.client.store.db.ClientDatabase

/** Opens the local store in the app's databases folder; a null [name] keeps it in memory. */
fun openLocalStore(
    context: Context,
    name: String? = DEFAULT_STORE_NAME,
): LocalStore = LocalStore(AndroidSqliteDriver(ClientDatabase.Schema, context, name))
