package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import kotlin.time.Instant

/** Answers every call with a fixed note and records what it received. */
class FakeNoteGateway : NoteGateway {
    val calls = mutableListOf<String>()

    override suspend fun list(
        page: Int,
        size: Int,
    ): NotePageResult {
        calls += "list $page $size"
        return NotePageResult.Success(NotePage(listOf(NOTE), page, size, 1, false))
    }

    override suspend fun get(id: String): NoteResult {
        calls += "get $id"
        return NoteResult.Success(NOTE)
    }

    override suspend fun create(draft: NoteDraft): NoteResult {
        calls += "create ${draft.title}"
        return NoteResult.Success(NOTE)
    }

    override suspend fun update(
        id: String,
        draft: NoteDraft,
    ): NoteResult {
        calls += "update $id ${draft.title}"
        return NoteResult.Success(NOTE)
    }

    override suspend fun delete(id: String): DeleteNoteResult {
        calls += "delete $id"
        return DeleteNoteResult.Deleted
    }

    companion object {
        val NOTE =
            Note(
                id = "3f0c1e6a-8b2d-4f7e-9a1c-5d6e7f8a9b0c",
                title = "Groceries",
                content = "Milk, eggs, bread",
                createdAt = Instant.parse("2026-10-22T08:00:00Z"),
                updatedAt = Instant.parse("2026-10-22T09:30:00Z"),
            )
    }
}
