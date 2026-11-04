package dev.ylp.client.sync.application

import dev.ylp.client.sync.domain.SyncResult

/**
 * Sends the changes made on this device to the API, in order. The use cases sync on their own;
 * an app also calls this when the network comes back or the app returns to the foreground.
 */
class SyncChanges internal constructor(
    private val engine: SyncEngine,
) {
    suspend fun execute(): SyncResult = engine.sync()
}
