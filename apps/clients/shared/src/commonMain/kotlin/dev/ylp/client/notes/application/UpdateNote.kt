package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.sync.application.SyncEngine

/**
 * Replaces the title and content of one of the user's notes on this device, then sends the change
 * to the API with the version it is based on. A note changed elsewhere since is not overwritten:
 * the note comes back [CONFLICTING][dev.ylp.client.notes.domain.SyncState.CONFLICTING], and
 * [ResolveConflict] settles it.
 */
class UpdateNote internal constructor(
    private val notes: LocalNotes,
    private val sync: SyncEngine,
) {
    suspend fun execute(
        id: String,
        title: String,
        content: String,
    ): NoteResult {
        val draft = NoteDraft(title, content)
        val violations = draft.violations()
        if (violations.isNotEmpty()) return NoteResult.Failed(NoteFailure.Invalid(violations))
        val note = notes.update(id, draft) ?: return NoteResult.Failed(NoteFailure.NotFound)
        sync.sync()
        return NoteResult.Success(notes.find(id)?.toNote() ?: note)
    }
}
