package dev.ylp.client.notes.domain

import kotlin.time.Instant

/**
 * A note of the signed-in user, as this device knows it. [id] is the API's id, or, for a note
 * written offline and not sent yet, a local one; on this device, both keep finding the note. [version] is the API version the note is based on: it grows at every
 * stored change, and a change based on an older one is refused. [sync] tells whether the API has
 * the note as shown.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
    val sync: SyncState = SyncState.SYNCED,
)

/** Whether the API has a note as this device shows it. */
enum class SyncState {
    /** The API has it as shown. */
    SYNCED,

    /** Changed on this device; the change is queued until the API can be reached. */
    PENDING,

    /** The API refused the change because of a change made elsewhere: the user resolves it. */
    CONFLICTING,
}

/**
 * One page of the user's notes, most recently updated first. [offline] tells that the API could
 * not be reached: the page shows the notes kept on this device.
 */
data class NotePage(
    val notes: List<Note>,
    val page: Int,
    val size: Int,
    val totalNotes: Long,
    val hasNext: Boolean,
    val offline: Boolean = false,
)
