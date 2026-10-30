package dev.ylp.notes.ui

import dev.ylp.client.notes.domain.NoteFailure

/** What a user reads for a failure. [NoteFailure.SignedOut] never shows: it leads to sign-in. */
fun NoteFailure.message(): String = when (this) {
    NoteFailure.NotFound -> "This note does not exist, or it is not yours."
    is NoteFailure.Invalid -> violations.joinToString("\n") { listOfNotNull(it.field, it.message).joinToString(": ") }
    NoteFailure.SignedOut -> "Your session has ended."
    is NoteFailure.Unavailable -> "The service cannot be reached. Try again."
}
