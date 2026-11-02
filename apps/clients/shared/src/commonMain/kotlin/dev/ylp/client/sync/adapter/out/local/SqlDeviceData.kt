package dev.ylp.client.sync.adapter.out.local

import dev.ylp.client.auth.domain.DeviceData
import dev.ylp.client.store.db.ClientDatabase

/** Everything the local database holds for the signed-in user: notes, conflicts, queued changes. */
internal class SqlDeviceData(
    private val database: ClientDatabase,
) : DeviceData {
    override suspend fun claimFor(subject: String?) {
        io {
            database.transaction {
                val owner = database.ownerQueries.current().executeAsOneOrNull()
                if (subject == null || owner != subject) {
                    wipe()
                    if (subject != null) database.ownerQueries.set(subject)
                }
            }
        }
    }

    override suspend fun clear() {
        io { database.transaction { wipe() } }
    }

    private fun wipe() {
        database.noteQueries.clearConflicts()
        database.noteQueries.clear()
        database.pendingChangeQueries.clear()
        database.ownerQueries.clear()
    }
}
