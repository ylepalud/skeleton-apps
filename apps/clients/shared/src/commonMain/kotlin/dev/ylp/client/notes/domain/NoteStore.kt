package dev.ylp.client.notes.domain

import kotlin.time.Instant

/**
 * A note in the local store. [id] is the id the screens use; [serverId] is null until the API has
 * stored a note created on this device. A note deleted here stays, [deleted], until the API
 * deletes it too.
 */
internal data class LocalNote(
    val id: String,
    val serverId: String?,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
    val sync: SyncState,
    val deleted: Boolean = false,
) {
    fun toNote() = Note(id, title, content, createdAt, updatedAt, version, sync)

    fun draft() = NoteDraft(title, content)
}

/** The API's side of a conflict, kept until the user resolves it. */
internal data class StoredConflict(
    val kind: ConflictKind,
    val theirs: Note?,
)

/** Where this device keeps the user's notes: what the API returned, and the changes made here. */
internal interface NoteStore {
    suspend fun byId(id: String): LocalNote?

    suspend fun byServerId(serverId: String): LocalNote?

    /** The notes not deleted here, most recently updated first. */
    suspend fun page(
        offset: Long,
        size: Long,
    ): List<LocalNote>

    suspend fun countVisible(): Long

    /** The notes created here that the API does not have yet. */
    suspend fun localOnly(): List<LocalNote>

    /** How many notes the API has that were deleted here and not on the API yet. */
    suspend fun countDeletedHere(): Long

    /** The ids of the synced notes updated between [oldest] and [newest]. */
    suspend fun syncedBetween(
        oldest: Instant,
        newest: Instant,
    ): List<LocalNote>

    suspend fun save(note: LocalNote)

    suspend fun remove(id: String)

    suspend fun conflictOf(id: String): StoredConflict?

    suspend fun saveConflict(
        id: String,
        conflict: StoredConflict,
    )

    suspend fun removeConflict(id: String)
}
