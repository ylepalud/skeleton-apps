package dev.ylp.client

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ylp.client.store.db.ClientDatabase

/**
 * Opens an empty local store in memory. The JVM target ships nowhere: this serves the tests of
 * the clients built on the library.
 */
fun openLocalStore(): LocalStore = LocalStore(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { ClientDatabase.Schema.create(it) })
