package dev.ylp.client.notes.adapter.out.local

import dev.ylp.client.notes.domain.ConflictKind
import dev.ylp.client.notes.domain.LocalNote
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteStore
import dev.ylp.client.notes.domain.StoredConflict
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.store.db.ClientDatabase
import dev.ylp.client.store.db.Note_conflict
import dev.ylp.client.sync.adapter.out.local.io
import kotlin.time.Instant
import dev.ylp.client.store.db.Note as NoteRow

/** The notes in the local database. */
internal class SqlNoteStore(
    database: ClientDatabase,
) : NoteStore {
    private val queries = database.noteQueries

    override suspend fun byId(id: String): LocalNote? = io { queries.byId(id).executeAsOneOrNull()?.toDomain() }

    override suspend fun byServerId(serverId: String): LocalNote? = io { queries.byServerId(serverId).executeAsOneOrNull()?.toDomain() }

    override suspend fun page(
        offset: Long,
        size: Long,
    ): List<LocalNote> = io { queries.page(size, offset).executeAsList().map { it.toDomain() } }

    override suspend fun countVisible(): Long = io { queries.countVisible().executeAsOne() }

    override suspend fun localOnly(): List<LocalNote> = io { queries.localOnly().executeAsList().map { it.toDomain() } }

    override suspend fun countDeletedHere(): Long = io { queries.countDeletedHere().executeAsOne() }

    override suspend fun syncedBetween(
        oldest: Instant,
        newest: Instant,
    ): List<LocalNote> =
        io {
            queries
                .syncedInRange(
                    newest.toEpochMilliseconds(),
                    oldest.toEpochMilliseconds(),
                    ::NoteRow,
                ).executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun save(note: LocalNote) {
        io { queries.upsert(note.toRow()) }
    }

    override suspend fun remove(id: String) {
        io {
            queries.transaction {
                queries.removeConflict(id)
                queries.remove(id)
            }
        }
    }

    override suspend fun conflictOf(id: String): StoredConflict? = io { queries.conflictOf(id).executeAsOneOrNull()?.toDomain() }

    override suspend fun saveConflict(
        id: String,
        conflict: StoredConflict,
    ) {
        val theirs = conflict.theirs
        io {
            queries.putConflict(
                Note_conflict(
                    note_id = id,
                    kind = conflict.kind.name,
                    title = theirs?.title,
                    content = theirs?.content,
                    created_at = theirs?.createdAt?.toEpochMilliseconds(),
                    updated_at = theirs?.updatedAt?.toEpochMilliseconds(),
                    version = theirs?.version,
                ),
            )
        }
    }

    override suspend fun removeConflict(id: String) {
        io { queries.removeConflict(id) }
    }

    private fun NoteRow.toDomain() =
        LocalNote(
            id = id,
            serverId = server_id,
            title = title,
            content = content,
            createdAt = Instant.fromEpochMilliseconds(created_at),
            updatedAt = Instant.fromEpochMilliseconds(updated_at),
            version = version,
            sync = SyncState.valueOf(sync),
            deleted = deleted != 0L,
        )

    private fun LocalNote.toRow() =
        NoteRow(
            id = id,
            server_id = serverId,
            title = title,
            content = content,
            created_at = createdAt.toEpochMilliseconds(),
            updated_at = updatedAt.toEpochMilliseconds(),
            version = version,
            sync = sync.name,
            deleted = if (deleted) 1 else 0,
        )

    private fun Note_conflict.toDomain(): StoredConflict {
        // The other side is stored whole or not at all.
        val theirs =
            if (title != null && content != null && created_at != null && updated_at != null && version != null) {
                Note(
                    id = note_id,
                    title = title,
                    content = content,
                    createdAt = Instant.fromEpochMilliseconds(created_at),
                    updatedAt = Instant.fromEpochMilliseconds(updated_at),
                    version = version,
                )
            } else {
                null
            }
        return StoredConflict(ConflictKind.valueOf(kind), theirs)
    }
}
