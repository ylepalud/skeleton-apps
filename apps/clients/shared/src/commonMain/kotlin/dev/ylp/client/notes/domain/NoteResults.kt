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

    /**
     * The note changed since the version the change was based on: nothing was overwritten. What to
     * do next is the policy of each screen; for notes, show the latest version.
     */
    data object Changed : NoteFailure()

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

/** The outcome of reading the conflict of a note. */
sealed class NoteConflictResult {
    data class Found(
        val conflict: NoteConflict,
    ) : NoteConflictResult()

    data class Failed(
        val failure: NoteFailure,
    ) : NoteConflictResult()
}

/** The outcome of resolving a conflict. */
sealed class ResolutionResult {
    data object Resolved : ResolutionResult()

    data class Failed(
        val failure: NoteFailure,
    ) : ResolutionResult()
}
