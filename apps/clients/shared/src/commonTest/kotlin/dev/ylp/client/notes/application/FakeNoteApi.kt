package dev.ylp.client.notes.application

import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.Violation
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The notes API in memory, as the real one answers: versions checked on every change, 404 for an
 * unknown note. It can go [offline], end the session, and be changed by another device.
 */
class FakeNoteApi : NoteGateway {
    val notes = linkedMapOf<String, Note>()
    val calls = mutableListOf<String>()
    var offline = false
    var signedOut = false
    private var lastId = 0
    private var time = Instant.parse("2026-11-01T08:00:00Z")

    /** A note another device wrote. */
    fun add(
        title: String,
        content: String = "",
    ): Note = store(Note("s${++lastId}", title, content, tick(), time, version = 0))

    fun changeElsewhere(
        id: String,
        title: String,
    ): Note = notes.getValue(id).let { store(it.copy(title = title, updatedAt = tick(), version = it.version + 1)) }

    fun deleteElsewhere(id: String) {
        notes.remove(id)
    }

    override suspend fun list(
        page: Int,
        size: Int,
    ): NotePageResult {
        calls += "list $page"
        failure()?.let { return NotePageResult.Failed(it) }
        val sorted = notes.values.sortedByDescending { it.updatedAt }
        val items = sorted.drop(page * size).take(size)
        return NotePageResult.Success(NotePage(items, page, size, sorted.size.toLong(), hasNext = (page + 1) * size < sorted.size))
    }

    override suspend fun get(id: String): NoteResult {
        calls += "get $id"
        failure()?.let { return NoteResult.Failed(it) }
        return notes[id]?.let { NoteResult.Success(it) } ?: NoteResult.Failed(NoteFailure.NotFound)
    }

    override suspend fun create(draft: NoteDraft): NoteResult {
        calls += "create ${draft.title}"
        failure()?.let { return NoteResult.Failed(it) }
        rejection(draft)?.let { return NoteResult.Failed(it) }
        return NoteResult.Success(store(Note("s${++lastId}", draft.title, draft.content, tick(), time, version = 0)))
    }

    override suspend fun update(
        id: String,
        draft: NoteDraft,
        version: Long,
    ): NoteResult {
        calls += "update $id ${draft.title} at $version"
        failure()?.let { return NoteResult.Failed(it) }
        val note = notes[id] ?: return NoteResult.Failed(NoteFailure.NotFound)
        if (note.version != version) return NoteResult.Failed(NoteFailure.Changed)
        rejection(draft)?.let { return NoteResult.Failed(it) }
        return NoteResult.Success(store(note.copy(title = draft.title, content = draft.content, updatedAt = tick(), version = version + 1)))
    }

    override suspend fun delete(
        id: String,
        version: Long,
    ): DeleteNoteResult {
        calls += "delete $id at $version"
        failure()?.let { return DeleteNoteResult.Failed(it) }
        val note = notes[id] ?: return DeleteNoteResult.Failed(NoteFailure.NotFound)
        if (note.version != version) return DeleteNoteResult.Failed(NoteFailure.Changed)
        notes.remove(id)
        return DeleteNoteResult.Deleted
    }

    private fun failure(): NoteFailure? =
        when {
            offline -> NoteFailure.Unavailable("offline")
            signedOut -> NoteFailure.SignedOut
            else -> null
        }

    // A rule the API holds and this client does not know: a title it refuses.
    private fun rejection(draft: NoteDraft) =
        if (draft.title == REJECTED_TITLE) NoteFailure.Invalid(listOf(Violation("title", "is reserved"))) else null

    private fun store(note: Note) = note.also { notes[it.id] = it }

    private fun tick() = (time + 1.minutes).also { time = it }

    companion object {
        const val REJECTED_TITLE = "reserved"
    }
}
