package dev.ylp.client.sync.adapter.out.local

import dev.ylp.client.store.db.ClientDatabase
import dev.ylp.client.store.db.Pending_change
import dev.ylp.client.sync.domain.ChangeOperation
import dev.ylp.client.sync.domain.PendingChange
import dev.ylp.client.sync.domain.PendingChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** The queue of changes in the local database. */
internal class SqlPendingChanges(
    database: ClientDatabase,
) : PendingChanges {
    private val queries = database.pendingChangeQueries
    private val transacter = database

    override suspend fun record(
        entity: String,
        entityId: String,
        operation: ChangeOperation,
    ): ChangeOperation? =
        io {
            transacter.transactionWithResult {
                val queued = queries.find(entity, entityId).executeAsOneOrNull()
                if (queued == null) {
                    queries.insert(entity, entityId, operation.name, QUEUED)
                    operation
                } else {
                    val merged = ChangeOperation.valueOf(queued.operation).then(operation)
                    if (merged == null) queries.remove(queued.seq) else queries.setOperation(merged.name, queued.seq)
                    merged
                }
            }
        }

    override suspend fun find(
        entity: String,
        entityId: String,
    ): PendingChange? = io { queries.find(entity, entityId).executeAsOneOrNull()?.toDomain() }

    override suspend fun queued(): List<PendingChange> = io { queries.queued().executeAsList().map { it.toDomain() } }

    override suspend fun markConflicting(seq: Long) {
        io { queries.setState(CONFLICTING, seq) }
    }

    override suspend fun requeue(
        seq: Long,
        operation: ChangeOperation,
    ) {
        io {
            transacter.transaction {
                queries.setOperation(operation.name, seq)
                queries.setState(QUEUED, seq)
            }
        }
    }

    override suspend fun remove(seq: Long) {
        io { queries.remove(seq) }
    }

    override suspend fun count(): Long = io { queries.countAll().executeAsOne() }

    private fun Pending_change.toDomain() = PendingChange(seq, entity, entity_id, ChangeOperation.valueOf(operation), state == CONFLICTING)

    private companion object {
        const val QUEUED = "QUEUED"
        const val CONFLICTING = "CONFLICTING"
    }
}

/** SQLite calls block: they run off the caller's thread, the UI's on Android and iOS. */
internal suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
