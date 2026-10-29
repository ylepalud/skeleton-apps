package dev.ylp.client.notes.domain

import kotlin.time.Instant

/** A note of the signed-in user, as the API returns it. */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** One page of the user's notes, most recently updated first. */
data class NotePage(
    val notes: List<Note>,
    val page: Int,
    val size: Int,
    val totalNotes: Long,
    val hasNext: Boolean,
)
