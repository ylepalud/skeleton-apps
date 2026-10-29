package dev.ylp.client.notes.domain

/**
 * The title and content of a note to write or replace, checked against the contract's limits
 * before any call, so that a client can show the problem without a round trip.
 */
data class NoteDraft(
    val title: String,
    val content: String,
) {
    fun violations(): List<Violation> =
        buildList {
            if (title.isEmpty()) add(Violation("title", "must not be empty"))
            if (title.length > TITLE_MAX_LENGTH) add(Violation("title", "must be at most $TITLE_MAX_LENGTH characters"))
            if (content.length > CONTENT_MAX_LENGTH) add(Violation("content", "must be at most $CONTENT_MAX_LENGTH characters"))
        }

    companion object {
        const val TITLE_MAX_LENGTH = 200
        const val CONTENT_MAX_LENGTH = 20_000
    }
}

/** A rule an input breaks; [field] is null when the API reports the problem without one. */
data class Violation(
    val field: String?,
    val message: String,
)
