package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NoteResult

/** Deletes one of the user's notes. */
class DeleteNote internal constructor(
    private val notes: NoteGateway,
) {
    suspend fun execute(id: String): DeleteNoteResult = notes.delete(id)
}
