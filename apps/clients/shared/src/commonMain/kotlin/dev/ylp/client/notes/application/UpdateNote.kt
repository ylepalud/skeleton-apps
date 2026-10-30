package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NoteResult

/** Replaces the title and content of one of the user's notes. */
class UpdateNote internal constructor(
    private val notes: NoteGateway,
) {
    suspend fun execute(
        id: String,
        title: String,
        content: String,
    ): NoteResult {
        val draft = NoteDraft(title, content)
        val violations = draft.violations()
        if (violations.isNotEmpty()) return NoteResult.Failed(NoteFailure.Invalid(violations))
        return notes.update(id, draft)
    }
}
