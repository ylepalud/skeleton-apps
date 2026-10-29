package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.Violation

/** Lists the user's notes, most recently updated first. */
class ListNotes internal constructor(
    private val notes: NoteGateway,
) {
    suspend fun execute(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
    ): NotePageResult {
        val violations =
            buildList {
                if (page < 0) add(Violation("page", "must be 0 or more"))
                if (size !in 1..MAX_PAGE_SIZE) add(Violation("size", "must be between 1 and $MAX_PAGE_SIZE"))
            }
        if (violations.isNotEmpty()) return NotePageResult.Failed(NoteFailure.Invalid(violations))
        return notes.list(page, size)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
    }
}
