package dev.ylp.notes.ui

import dev.ylp.client.notes.domain.ConflictKind
import dev.ylp.client.notes.domain.NoteConflict
import dev.ylp.client.notes.domain.Resolution
import dev.ylp.client.notes.domain.SyncState

/** What the user reads about a conflict: what happened to the note. */
fun NoteConflict.headline(): String = when (kind) {
    ConflictKind.CHANGED_ELSEWHERE ->
        if (mineDeleted) "You deleted this note, and it was changed elsewhere." else "This note was changed elsewhere."

    ConflictKind.DELETED_ELSEWHERE -> "This note was deleted elsewhere."

    ConflictKind.REJECTED -> "The service refused this change."
}

/** The button of a resolution, in the words of this conflict. */
fun NoteConflict.label(resolution: Resolution): String = when (resolution) {
    Resolution.KEEP_MINE -> when {
        mineDeleted -> "Delete anyway"
        kind == ConflictKind.DELETED_ELSEWHERE -> "Restore mine"
        else -> "Keep mine"
    }

    Resolution.KEEP_THEIRS -> if (theirs == null) "Discard mine" else "Keep theirs"

    Resolution.KEEP_BOTH -> "Keep both"
}

/** The label of a note in the list, when the API does not have it as shown. */
fun SyncState.label(): String? = when (this) {
    SyncState.SYNCED -> null
    SyncState.PENDING -> "Not synced"
    SyncState.CONFLICTING -> "Conflict: open to resolve"
}

/** What the list says when the API cannot be reached. */
const val OFFLINE = "Offline: these are the notes kept on this device."
