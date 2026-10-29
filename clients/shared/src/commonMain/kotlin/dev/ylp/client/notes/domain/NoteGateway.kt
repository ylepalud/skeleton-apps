package dev.ylp.client.notes.domain

/** Where the user's notes live: the API, behind the HTTP adapter. */
interface NoteGateway {
    suspend fun list(
        page: Int,
        size: Int,
    ): NotePageResult

    suspend fun get(id: String): NoteResult

    suspend fun create(draft: NoteDraft): NoteResult

    suspend fun update(
        id: String,
        draft: NoteDraft,
    ): NoteResult

    suspend fun delete(id: String): DeleteNoteResult
}
