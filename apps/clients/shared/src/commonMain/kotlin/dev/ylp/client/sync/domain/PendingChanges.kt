package dev.ylp.client.sync.domain

/** What a queued change does to its entity on the API. */
internal enum class ChangeOperation {
    CREATE,
    UPDATE,
    DELETE,
    ;

    /**
     * The one change left when [next] follows this one, before either reached the API: the queue
     * replays the latest local state once. Null when nothing is left to send: an entity created
     * and deleted here.
     */
    fun then(next: ChangeOperation): ChangeOperation? =
        when {
            this == CREATE && next == DELETE -> null
            this == CREATE -> CREATE
            else -> next
        }
}

/** A change of [entity] made on this device and not stored by the API yet. */
internal data class PendingChange(
    val seq: Long,
    val entity: String,
    val entityId: String,
    val operation: ChangeOperation,
    val conflicting: Boolean,
)

/**
 * The queue of local changes, whatever the entity, in the order of their first change. One change
 * per entity: a new one is merged into the queued one with [ChangeOperation.then].
 */
internal interface PendingChanges {
    /** Queues [operation] on the entity; returns the change left once merged, null if none. */
    suspend fun record(
        entity: String,
        entityId: String,
        operation: ChangeOperation,
    ): ChangeOperation?

    suspend fun find(
        entity: String,
        entityId: String,
    ): PendingChange?

    /** The changes to replay, oldest first; a conflicting change waits for its resolution. */
    suspend fun queued(): List<PendingChange>

    suspend fun markConflicting(seq: Long)

    /** Puts a resolved change back in the queue, at its place, as [operation]. */
    suspend fun requeue(
        seq: Long,
        operation: ChangeOperation,
    )

    suspend fun remove(seq: Long)

    suspend fun count(): Long
}

/** What replaying one change gave. */
internal enum class ReplayOutcome {
    /** The API stored the change: it leaves the queue. */
    APPLIED,

    /** The API refused it because of a change made elsewhere: it waits for the user. */
    CONFLICT,

    /** The API cannot be reached: the change and the ones after it wait for the next sync. */
    OFFLINE,

    /** The session is over: nothing replays until the user signs in again. */
    SIGNED_OUT,
}

/**
 * Sends the queued changes of one entity to the API, and stores what the API answers. Each entity
 * brings its own, with its conflict strategy: what a refused change becomes and how the user
 * resolves it.
 */
internal interface ChangeReplayer {
    val entity: String

    suspend fun replay(change: PendingChange): ReplayOutcome
}
