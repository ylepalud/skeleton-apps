package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NoteResult

/** Deletes one of the user's notes, as read at [version]: a note changed since is kept. */
class DeleteNote internal constructor(
    private val notes: NoteGateway,
) {
    suspend fun execute(
        id: String,
        version: Long,
    ): DeleteNoteResult = notes.delete(id, version)
}
