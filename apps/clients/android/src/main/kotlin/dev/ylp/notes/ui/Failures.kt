package dev.ylp.notes.ui

import dev.ylp.client.notes.domain.NoteFailure

/** What a user reads for a failure. [NoteFailure.SignedOut] never shows: it leads to sign-in. */
fun NoteFailure.message(): String = when (this) {
    NoteFailure.NotFound -> "This note does not exist, or it is not yours."
    is NoteFailure.Invalid -> violations.joinToString("\n") { listOfNotNull(it.field, it.message).joinToString(": ") }
    NoteFailure.SignedOut -> "Your session has ended."
    NoteFailure.Changed -> CHANGED_ELSEWHERE
    is NoteFailure.Unavailable -> "The service cannot be reached. Try again."
}

/** What the editor says when the note changed elsewhere between reading and saving it. */
const val CHANGED_ELSEWHERE = "This note was changed elsewhere. It now shows the latest version."
