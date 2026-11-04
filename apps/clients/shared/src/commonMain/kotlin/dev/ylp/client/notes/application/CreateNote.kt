package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.sync.application.SyncEngine

/**
 * Writes a new note on this device, then sends it to the API if it can be reached; otherwise it
 * waits in the queue. The note returned tells which, by its sync state.
 */
class CreateNote internal constructor(
    private val notes: LocalNotes,
    private val sync: SyncEngine,
) {
    suspend fun execute(
        title: String,
        content: String,
    ): NoteResult {
        val draft = NoteDraft(title, content)
        val violations = draft.violations()
        if (violations.isNotEmpty()) return NoteResult.Failed(NoteFailure.Invalid(violations))
        val note = notes.create(draft)
        sync.sync()
        return NoteResult.Success(notes.find(note.id)?.toNote() ?: note)
    }
}
