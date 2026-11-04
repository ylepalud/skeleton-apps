package dev.ylp.client.auth.domain

/**
 * What the client keeps on the device for the signed-in user. It is forgotten when the user signs
 * out, and when someone else signs in: nobody reads, or sends as their own, another user's notes.
 */
internal interface DeviceData {
    /** Keeps the data of [subject], the user signing in; wipes it when it is someone else's or unknown. */
    suspend fun claimFor(subject: String?)

    suspend fun clear()
}
