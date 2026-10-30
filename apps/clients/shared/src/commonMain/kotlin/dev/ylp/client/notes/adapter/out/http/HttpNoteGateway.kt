package dev.ylp.client.notes.adapter.out.http

import dev.ylp.client.generated.apis.NotesApi
import dev.ylp.client.generated.infrastructure.HttpResponse
import dev.ylp.client.generated.models.NoteRequest
import dev.ylp.client.generated.models.Problem
import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteGateway
import dev.ylp.client.notes.domain.NotePage
import dev.ylp.client.notes.domain.NotePageResult
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.Violation
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.CancellationException
import dev.ylp.client.generated.models.Note as NoteBody
import dev.ylp.client.generated.models.NotePage as NotePageBody

/** The notes of the API, through the client generated from the contract. */
internal class HttpNoteGateway(
    private val api: NotesApi,
) : NoteGateway {
    override suspend fun list(
        page: Int,
        size: Int,
    ): NotePageResult =
        call(
            request = { api.listNotes(page, size) },
            success = { NotePageResult.Success(it.body().toDomain()) },
            failure = { NotePageResult.Failed(it) },
        )

    override suspend fun get(id: String): NoteResult = noteCall { api.getNote(id) }

    override suspend fun create(draft: NoteDraft): NoteResult = noteCall { api.createNote(draft.toBody()) }

    override suspend fun update(
        id: String,
        draft: NoteDraft,
    ): NoteResult = noteCall { api.updateNote(id, draft.toBody()) }

    override suspend fun delete(id: String): DeleteNoteResult =
        call(
            request = { api.deleteNote(id) },
            success = { DeleteNoteResult.Deleted },
            failure = { DeleteNoteResult.Failed(it) },
        )

    private suspend fun noteCall(request: suspend () -> HttpResponse<NoteBody>): NoteResult =
        call(
            request = request,
            success = { NoteResult.Success(it.body().toDomain()) },
            failure = { NoteResult.Failed(it) },
        )

    /** Runs a request; an error status or a transport failure becomes a [NoteFailure]. */
    private suspend fun <T : Any, R> call(
        request: suspend () -> HttpResponse<T>,
        success: suspend (HttpResponse<T>) -> R,
        failure: (NoteFailure) -> R,
    ): R =
        try {
            val response = request()
            if (response.success) success(response) else failure(response.toFailure())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(NoteFailure.Unavailable(e.message ?: "The API cannot be reached"))
        }

    private suspend fun HttpResponse<*>.toFailure(): NoteFailure =
        when (status) {
            401 -> NoteFailure.SignedOut
            404 -> NoteFailure.NotFound
            400, 413 -> NoteFailure.Invalid(listOf(Violation(null, problem()?.let { it.detail ?: it.title } ?: "Invalid request")))
            else -> NoteFailure.Unavailable(problem()?.title ?: "Unexpected answer from the API: $status")
        }

    private suspend fun HttpResponse<*>.problem(): Problem? = runCatching { typedBody<Problem>(typeInfo<Problem>()) }.getOrNull()

    private fun NoteDraft.toBody() = NoteRequest(title = title, content = content)

    private fun NoteBody.toDomain() = Note(id = id, title = title, content = content, createdAt = createdAt, updatedAt = updatedAt)

    private fun NotePageBody.toDomain() =
        NotePage(
            notes = items.map { it.toDomain() },
            page = page,
            // The generator renames "size", a reserved word in its templates.
            size = propertySize,
            totalNotes = totalItems,
            hasNext = hasNext,
        )
}
