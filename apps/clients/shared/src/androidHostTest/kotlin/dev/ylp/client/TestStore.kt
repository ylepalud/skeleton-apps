package dev.ylp.client

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ylp.client.store.db.ClientDatabase

actual fun testStore(): LocalStore = LocalStore(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { ClientDatabase.Schema.create(it) })
