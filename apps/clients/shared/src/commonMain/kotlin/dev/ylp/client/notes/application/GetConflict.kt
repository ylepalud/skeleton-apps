package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteConflictResult
import dev.ylp.client.notes.domain.NoteFailure

/** Reads both sides of a conflicting note, and the ways to resolve it. */
class GetConflict internal constructor(
    private val notes: LocalNotes,
) {
    suspend fun execute(id: String): NoteConflictResult =
        notes.conflictOf(id)?.let { NoteConflictResult.Found(it) } ?: NoteConflictResult.Failed(NoteFailure.NotFound)
}
