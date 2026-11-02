package dev.ylp.client.sync

import dev.ylp.client.notes.adapter.out.local.SqlNoteStore
import dev.ylp.client.notes.domain.LocalNote
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.sync.adapter.out.local.SqlDeviceData
import dev.ylp.client.sync.adapter.out.local.SqlPendingChanges
import dev.ylp.client.sync.domain.ChangeOperation
import dev.ylp.client.testStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** The queue's merge rules, and whose data the device keeps. */
class SyncStoreTest {
    private val database = testStore().database
    private val changes = SqlPendingChanges(database)
    private val notes = SqlNoteStore(database)
    private val deviceData = SqlDeviceData(database)

    @Test
    fun `a change merged into the queued one leaves what the API still needs`() {
        val c = ChangeOperation.CREATE
        val u = ChangeOperation.UPDATE
        val d = ChangeOperation.DELETE
        assertEquals(c, c.then(u))
        assertNull(c.then(d))
        assertEquals(u, u.then(u))
        assertEquals(d, u.then(d))
    }

    @Test
    fun `the same user signing in again keeps the data and the changes not sent`() =
        runTest {
            deviceData.claimFor("alice")
            keepSomething()

            deviceData.claimFor("alice")

            assertEquals(1, changes.count())
            assertEquals(1, notes.countVisible())
        }

    @Test
    fun `another user signing in starts from an empty device`() =
        runTest {
            deviceData.claimFor("alice")
            keepSomething()

            deviceData.claimFor("bob")

            assertEquals(0, changes.count())
            assertEquals(0, notes.countVisible())
        }

    @Test
    fun `a user the provider does not name starts from an empty device`() =
        runTest {
            deviceData.claimFor("alice")
            keepSomething()

            deviceData.claimFor(null)

            assertEquals(0, changes.count())
        }

    private suspend fun keepSomething() {
        val now = Instant.parse("2026-11-02T08:00:00Z")
        notes.save(LocalNote("n1", null, "Groceries", "", now, now, version = 0, SyncState.PENDING))
        changes.record("note", "n1", ChangeOperation.CREATE)
    }
}
