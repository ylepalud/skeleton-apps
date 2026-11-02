package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.ConflictKind
import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.LocalNote
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteConflict
import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.NoteStore
import dev.ylp.client.notes.domain.Resolution
import dev.ylp.client.notes.domain.StoredConflict
import dev.ylp.client.notes.domain.SyncState
import dev.ylp.client.sync.domain.ChangeOperation
import dev.ylp.client.sync.domain.ChangeReplayer
import dev.ylp.client.sync.domain.PendingChange
import dev.ylp.client.sync.domain.PendingChanges
import dev.ylp.client.sync.domain.ReplayOutcome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The notes of this device: the local store kept in step with the API. A change is written here
 * first and queued, then replayed by the sync with the version it is based on; a refused change
 * becomes a [NoteConflict] the user resolves.
 *
 * [lock] is the store's lock, shared with the sync: local writes and replays never interleave.
 */
internal class LocalNotes(
    private val store: NoteStore,
    private val changes: PendingChanges,
    private val remote: NoteGateway,
    private val lock: Mutex,
    private val clock: Clock,
    private val newId: () -> String,
) : ChangeReplayer {
    override val entity = "note"

    suspend fun find(id: String): LocalNote? = store.byId(id)?.takeUnless { it.deleted }

    suspend fun deletedHere(id: String): Boolean = store.byId(id)?.deleted == true

    suspend fun create(draft: NoteDraft): Note =
        lock.withLock {
            val now = clock.now()
            val note = LocalNote(newId(), null, draft.title, draft.content, now, now, version = 0, SyncState.PENDING)
            store.save(note)
            changes.record(entity, note.id, ChangeOperation.CREATE)
            note.toNote()
        }

    /** Null when the note does not exist on this device. */
    suspend fun update(
        id: String,
        draft: NoteDraft,
    ): Note? =
        lock.withLock {
            val note = find(id) ?: return@withLock null
            val changed = note.copy(title = draft.title, content = draft.content, updatedAt = clock.now(), sync = note.sync.afterChange())
            store.save(changed)
            changes.record(entity, id, ChangeOperation.UPDATE)
            changed.toNote()
        }

    /** False when the note does not exist on this device. */
    suspend fun delete(id: String): Boolean =
        lock.withLock {
            val note = find(id) ?: return@withLock false
            // A note the API never had leaves at once: nothing is left to send.
            when (changes.record(entity, id, ChangeOperation.DELETE)) {
                null -> store.remove(id)
                else -> store.save(note.copy(deleted = true, sync = note.sync.afterChange()))
            }
            true
        }

    /** The notes kept on this device, as a page, when the API cannot be reached. */
    suspend fun cachedPage(
        page: Int,
        size: Int,
    ): NotePage {
        val total = store.countVisible()
        val notes = store.page(page.toLong() * size, size.toLong()).map { it.toNote() }
        return NotePage(notes, page, size, total, hasNext = (page + 1L) * size < total, offline = true)
    }

    /**
     * Keeps a page the API returned, and shows it with the changes of this device on top: the
     * notes created here first, the ones changed here as changed, the ones deleted here hidden.
     */
    suspend fun keep(fetched: NotePage): NotePage =
        lock.withLock {
            fetched.notes.forEach { keepFetched(it) }
            forgetDeletedElsewhere(fetched)
            val shown = fetched.notes.mapNotNull { store.byServerId(it.id)?.takeUnless { note -> note.deleted }?.toNote() }
            val createdHere = if (fetched.page == 0) store.localOnly().map { it.toNote() } else emptyList()
            val total = fetched.totalNotes + store.localOnly().size - store.countDeletedHere()
            fetched.copy(notes = createdHere + shown, totalNotes = total.coerceAtLeast(0))
        }

    /** Keeps a note the API returned; returns it as this device shows it. */
    suspend fun keep(fetched: Note): Note? = lock.withLock { keepFetched(fetched) }

    /** Forgets a note the API no longer has, unless it changed here. */
    suspend fun forget(id: String) {
        lock.withLock {
            val note = store.byId(id)
            if (note != null && note.sync == SyncState.SYNCED) store.remove(id)
        }
    }

    private suspend fun keepFetched(fetched: Note): Note? {
        val local = store.byServerId(fetched.id)
        if (local == null || local.sync == SyncState.SYNCED) {
            store.save(fetched.toLocal(id = local?.id ?: fetched.id))
        }
        return store.byServerId(fetched.id)?.takeUnless { it.deleted }?.toNote()
    }

    // A synced note in the fetched page's range that the page does not hold was deleted elsewhere.
    // The first page reaches up to now, the last one back to the oldest note.
    private suspend fun forgetDeletedElsewhere(fetched: NotePage) {
        if (fetched.notes.isEmpty() && (fetched.page > 0 || fetched.hasNext)) return
        val newest = if (fetched.page == 0) Instant.DISTANT_FUTURE else fetched.notes.first().updatedAt
        val oldest = if (fetched.hasNext) fetched.notes.last().updatedAt else Instant.DISTANT_PAST
        val returned = fetched.notes.map { it.id }.toSet()
        store
            .syncedBetween(oldest, newest)
            .filter { it.serverId !in returned }
            .forEach { store.remove(it.id) }
    }

    suspend fun conflictOf(id: String): NoteConflict? {
        val note = store.byId(id) ?: return null
        val conflict = store.conflictOf(id) ?: return null
        return NoteConflict(note.toNote(), note.deleted, conflict.theirs?.copy(id = id), conflict.kind)
    }

    /** Null when the note has no conflict, or [resolution] does not apply to it. */
    suspend fun resolve(
        id: String,
        resolution: Resolution,
    ): Unit? =
        lock.withLock {
            val note = store.byId(id) ?: return@withLock null
            val conflict = conflictOf(id) ?: return@withLock null
            val change = changes.find(entity, id) ?: return@withLock null
            if (resolution !in conflict.resolutions) return@withLock null
            val theirs = conflict.theirs
            when (resolution) {
                Resolution.KEEP_MINE -> {
                    if (theirs == null) {
                        // Deleted elsewhere: the note is written again, as a new one.
                        store.save(note.copy(serverId = null, version = 0, sync = SyncState.PENDING))
                        changes.requeue(change.seq, ChangeOperation.CREATE)
                    } else {
                        store.save(note.copy(version = theirs.version, sync = SyncState.PENDING))
                        changes.requeue(change.seq, change.operation)
                    }
                }

                Resolution.KEEP_THEIRS -> {
                    takeTheirs(note, theirs, change)
                }

                Resolution.KEEP_BOTH -> {
                    val now = clock.now()
                    val copy =
                        note.copy(
                            id = newId(),
                            serverId = null,
                            createdAt = now,
                            updatedAt = now,
                            version = 0,
                            sync = SyncState.PENDING,
                        )
                    store.save(copy)
                    changes.record(entity, copy.id, ChangeOperation.CREATE)
                    takeTheirs(note, theirs, change)
                }
            }
            store.removeConflict(id)
        }

    private suspend fun takeTheirs(
        note: LocalNote,
        theirs: Note?,
        change: PendingChange,
    ) {
        changes.remove(change.seq)
        if (theirs == null) {
            store.remove(note.id)
        } else {
            store.save(
                note.copy(
                    title = theirs.title,
                    content = theirs.content,
                    updatedAt = theirs.updatedAt,
                    version = theirs.version,
                    sync = SyncState.SYNCED,
                    deleted = false,
                ),
            )
        }
    }

    /** Called by the sync, which holds [lock]. */
    override suspend fun replay(change: PendingChange): ReplayOutcome {
        val note = store.byId(change.entityId) ?: return ReplayOutcome.APPLIED
        val serverId = note.serverId
        return when {
            change.operation == ChangeOperation.CREATE || serverId == null -> {
                applied(note, remote.create(note.draft())) { stored -> store.save(note.stored(stored, serverId = stored.id)) }
            }

            change.operation == ChangeOperation.UPDATE -> {
                applied(note, remote.update(serverId, note.draft(), note.version)) { stored -> store.save(note.stored(stored, serverId)) }
            }

            else -> {
                when (val result = remote.delete(serverId, note.version)) {
                    DeleteNoteResult.Deleted -> {
                        ReplayOutcome.APPLIED.also { store.remove(note.id) }
                    }

                    // Deleted elsewhere too: the user's intent holds.
                    is DeleteNoteResult.Failed if result.failure == NoteFailure.NotFound -> {
                        ReplayOutcome.APPLIED.also {
                            store.remove(
                                note.id,
                            )
                        }
                    }

                    is DeleteNoteResult.Failed -> {
                        refused(note, result.failure)
                    }
                }
            }
        }
    }

    private suspend fun applied(
        note: LocalNote,
        result: NoteResult,
        keep: suspend (Note) -> Unit,
    ): ReplayOutcome =
        when (result) {
            is NoteResult.Success -> ReplayOutcome.APPLIED.also { keep(result.note) }
            is NoteResult.Failed -> refused(note, result.failure)
        }

    // The notes strategy: a refused change is kept, next to what the API holds, for the user.
    private suspend fun refused(
        note: LocalNote,
        failure: NoteFailure,
    ): ReplayOutcome =
        when (failure) {
            NoteFailure.SignedOut -> {
                ReplayOutcome.SIGNED_OUT
            }

            is NoteFailure.Unavailable -> {
                ReplayOutcome.OFFLINE
            }

            NoteFailure.NotFound -> {
                conflict(note, StoredConflict(ConflictKind.DELETED_ELSEWHERE, theirs = null))
            }

            is NoteFailure.Invalid -> {
                conflict(note, StoredConflict(ConflictKind.REJECTED, theirs = null))
            }

            NoteFailure.Changed -> {
                when (val latest = remote.get(note.serverId ?: note.id)) {
                    is NoteResult.Success -> conflict(note, StoredConflict(ConflictKind.CHANGED_ELSEWHERE, latest.note))
                    is NoteResult.Failed -> refused(note, latest.failure)
                }
            }
        }

    private suspend fun conflict(
        note: LocalNote,
        conflict: StoredConflict,
    ): ReplayOutcome {
        // A deletion meets a note deleted elsewhere: nothing to resolve.
        if (note.deleted && conflict.kind == ConflictKind.DELETED_ELSEWHERE) return ReplayOutcome.APPLIED.also { store.remove(note.id) }
        store.saveConflict(note.id, conflict)
        store.save(note.copy(sync = SyncState.CONFLICTING))
        return ReplayOutcome.CONFLICT
    }

    private fun LocalNote.stored(
        stored: Note,
        serverId: String,
    ) = copy(
        serverId = serverId,
        createdAt = stored.createdAt,
        updatedAt = stored.updatedAt,
        version = stored.version,
        sync = SyncState.SYNCED,
    )

    private fun Note.toLocal(id: String) =
        LocalNote(id, serverId = this.id, title, content, createdAt, updatedAt, version, SyncState.SYNCED)

    // A conflicting note stays so until the user resolves it, even when changed again.
    private fun SyncState.afterChange() = if (this == SyncState.CONFLICTING) SyncState.CONFLICTING else SyncState.PENDING
}
