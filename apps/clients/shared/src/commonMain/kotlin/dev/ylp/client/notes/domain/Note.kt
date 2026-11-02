package dev.ylp.client.notes.domain

import kotlin.time.Instant

/**
 * A note of the signed-in user, as the API returns it. [version] grows at every stored change: a
 * change or deletion names the version it is based on, and fails with [NoteFailure.Changed] if the
 * note changed since.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)

/** One page of the user's notes, most recently updated first. */
data class NotePage(
    val notes: List<Note>,
    val page: Int,
    val size: Int,
    val totalNotes: Long,
    val hasNext: Boolean,
)
