package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.sync.application.SyncEngine

/**
 * Deletes one of the user's notes on this device, then on the API, as read: a note changed
 * elsewhere since is kept, as a conflict to resolve.
 */
class DeleteNote internal constructor(
    private val notes: LocalNotes,
    private val sync: SyncEngine,
) {
    suspend fun execute(id: String): DeleteNoteResult {
        if (!notes.delete(id)) return DeleteNoteResult.Failed(NoteFailure.NotFound)
        sync.sync()
        return DeleteNoteResult.Deleted
    }
}
