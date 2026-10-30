package dev.ylp.client.notes.domain

/** Why an operation on notes did not succeed. */
sealed class NoteFailure {
    /** The note does not exist, or belongs to another user. */
    data object NotFound : NoteFailure()

    /** The input breaks a rule of the contract. */
    data class Invalid(
        val violations: List<Violation>,
    ) : NoteFailure()

    /** No valid session: the user has to sign in again. */
    data object SignedOut : NoteFailure()

    /** The API could not be reached, or answered with an unexpected error. */
    data class Unavailable(
        val message: String,
    ) : NoteFailure()
}

/** The outcome of reading or writing one note. */
sealed class NoteResult {
    data class Success(
        val note: Note,
    ) : NoteResult()

    data class Failed(
        val failure: NoteFailure,
    ) : NoteResult()
}

/** The outcome of listing notes. */
sealed class NotePageResult {
    data class Success(
        val page: NotePage,
    ) : NotePageResult()

    data class Failed(
        val failure: NoteFailure,
    ) : NotePageResult()
}

/** The outcome of deleting a note. */
sealed class DeleteNoteResult {
    data object Deleted : DeleteNoteResult()

    data class Failed(
        val failure: NoteFailure,
    ) : DeleteNoteResult()
}
