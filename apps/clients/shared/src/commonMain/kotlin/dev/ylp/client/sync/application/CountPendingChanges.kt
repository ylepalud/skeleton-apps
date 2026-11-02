package dev.ylp.client.sync.application

import dev.ylp.client.sync.domain.PendingChanges

/** How many local changes the API does not have yet: signing out now would lose them. */
class CountPendingChanges internal constructor(
    private val changes: PendingChanges,
) {
    suspend fun execute(): Long = changes.count()
}
