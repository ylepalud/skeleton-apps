package dev.ylp.client.sync.application

import dev.ylp.client.sync.domain.ChangeReplayer
import dev.ylp.client.sync.domain.PendingChanges
import dev.ylp.client.sync.domain.ReplayOutcome
import dev.ylp.client.sync.domain.SyncResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Replays the queued changes in order, each through the replayer of its entity. [store] guards the
 * local store: a change is read, sent and its answer stored while no local write runs, so a write
 * made meanwhile is never lost; it waits for one call at most.
 */
internal class SyncEngine(
    private val changes: PendingChanges,
    replayers: List<ChangeReplayer>,
    private val store: Mutex,
) {
    private val replayers = replayers.associateBy { it.entity }
    private val running = Mutex()

    suspend fun sync(): SyncResult =
        running.withLock {
            for (queued in changes.queued()) {
                val outcome =
                    store.withLock {
                        // Read again: a local write may have merged it, or a resolution removed it.
                        val change = changes.find(queued.entity, queued.entityId)
                        if (change == null || change.conflicting) return@withLock ReplayOutcome.APPLIED
                        val replayer = replayers.getValue(change.entity)
                        replayer.replay(change).also { outcome ->
                            when (outcome) {
                                ReplayOutcome.APPLIED -> changes.remove(change.seq)
                                ReplayOutcome.CONFLICT -> changes.markConflicting(change.seq)
                                ReplayOutcome.OFFLINE, ReplayOutcome.SIGNED_OUT -> Unit
                            }
                        }
                    }
                when (outcome) {
                    ReplayOutcome.OFFLINE -> return@withLock SyncResult.OFFLINE
                    ReplayOutcome.SIGNED_OUT -> return@withLock SyncResult.SIGNED_OUT
                    ReplayOutcome.APPLIED, ReplayOutcome.CONFLICT -> Unit
                }
            }
            SyncResult.SYNCED
        }
}
