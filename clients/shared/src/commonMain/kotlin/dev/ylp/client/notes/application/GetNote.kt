package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NoteResult

/** Reads one of the user's notes. */
class GetNote internal constructor(
    private val notes: NoteGateway,
) {
    suspend fun execute(id: String): NoteResult = notes.get(id)
}
