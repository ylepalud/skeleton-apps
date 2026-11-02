package dev.ylp.client

import app.cash.sqldelight.db.SqlDriver
import dev.ylp.client.store.db.ClientDatabase

/**
 * The database where a client keeps the user's notes and the changes the API does not have yet,
 * so that it works offline. Each platform opens it with `openLocalStore`.
 */
class LocalStore internal constructor(
    driver: SqlDriver,
) {
    internal val database = ClientDatabase(driver)
}
