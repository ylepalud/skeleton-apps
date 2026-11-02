package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.Resolution
import dev.ylp.client.notes.domain.ResolutionResult
import dev.ylp.client.notes.domain.Violation
import dev.ylp.client.sync.application.SyncEngine

/**
 * Settles a conflicting note the way the user picked, among the conflict's resolutions, then sends
 * what is left to the API.
 */
class ResolveConflict internal constructor(
    private val notes: LocalNotes,
    private val sync: SyncEngine,
) {
    suspend fun execute(
        id: String,
        resolution: Resolution,
    ): ResolutionResult {
        val conflict = notes.conflictOf(id) ?: return ResolutionResult.Failed(NoteFailure.NotFound)
        if (resolution !in conflict.resolutions) {
            return ResolutionResult.Failed(NoteFailure.Invalid(listOf(Violation(null, "$resolution does not resolve this conflict"))))
        }
        notes.resolve(id, resolution) ?: return ResolutionResult.Failed(NoteFailure.NotFound)
        sync.sync()
        return ResolutionResult.Resolved
    }
}
