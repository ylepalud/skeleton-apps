package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.SyncState

/**
 * Reads one of the user's notes: from the API when it can be reached, from this device otherwise.
 * A note changed here and not sent yet is read as changed here.
 */
class GetNote internal constructor(
    private val notes: LocalNotes,
    private val remote: NoteGateway,
) {
    suspend fun execute(id: String): NoteResult {
        if (notes.deletedHere(id)) return NoteResult.Failed(NoteFailure.NotFound)
        val local = notes.find(id)
        if (local != null && local.sync != SyncState.SYNCED) return NoteResult.Success(local.toNote())
        val result = remote.get(local?.serverId ?: id)
        return when {
            result is NoteResult.Success -> {
                notes.keep(result.note)?.let { NoteResult.Success(it) }
                    ?: NoteResult.Failed(NoteFailure.NotFound)
            }

            result is NoteResult.Failed && result.failure == NoteFailure.NotFound -> {
                result.also { notes.forget(id) }
            }

            result is NoteResult.Failed && result.failure is NoteFailure.Unavailable && local != null -> {
                NoteResult.Success(local.toNote())
            }

            else -> {
                result
            }
        }
    }
}
