package dev.ylp.client.sync.domain

/** How far a sync went. */
enum class SyncResult {
    /** Every queued change reached the API, or waits for the user to resolve a conflict. */
    SYNCED,

    /** The API cannot be reached: the changes left wait for the next sync. */
    OFFLINE,

    /** The session is over: the changes wait until the user signs in again. */
    SIGNED_OUT,
}
