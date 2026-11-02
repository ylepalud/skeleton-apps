package dev.ylp.client.notes.domain

/**
 * A change of a note the API refused, and what the API holds instead: the notes strategy for
 * concurrent changes. Nothing is lost: the user picks one of [resolutions].
 */
data class NoteConflict(
    /** The note as changed on this device. */
    val mine: Note,
    /** Whether the change of this device is a deletion. */
    val mineDeleted: Boolean,
    /** The note as the API holds it; null when it was deleted elsewhere or the change was rejected. */
    val theirs: Note?,
    val kind: ConflictKind,
) {
    val resolutions: List<Resolution> =
        when (kind) {
            ConflictKind.CHANGED_ELSEWHERE -> {
                if (mineDeleted) {
                    listOf(Resolution.KEEP_MINE, Resolution.KEEP_THEIRS)
                } else {
                    listOf(Resolution.KEEP_MINE, Resolution.KEEP_THEIRS, Resolution.KEEP_BOTH)
                }
            }

            ConflictKind.DELETED_ELSEWHERE -> {
                listOf(Resolution.KEEP_MINE, Resolution.KEEP_THEIRS)
            }

            ConflictKind.REJECTED -> {
                listOf(Resolution.KEEP_THEIRS)
            }
        }
}

/** Why the API refused a change. */
enum class ConflictKind {
    /** The note changed elsewhere since the version the change is based on. */
    CHANGED_ELSEWHERE,

    /** The note was deleted elsewhere while it was changed here. */
    DELETED_ELSEWHERE,

    /** The API rejects the change itself, as breaking one of its rules. */
    REJECTED,
}

/** How the user resolves a [NoteConflict]. */
enum class Resolution {
    /**
     * The change of this device wins: sent again on top of the latest version, or, for a note
     * deleted elsewhere, written again as a note.
     */
    KEEP_MINE,

    /** The API's state wins: the change of this device is dropped. */
    KEEP_THEIRS,

    /** Both stay: the API's state, and the change of this device as a new note. */
    KEEP_BOTH,
}
