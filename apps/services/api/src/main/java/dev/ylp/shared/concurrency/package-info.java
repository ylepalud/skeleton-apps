/**
 * Optimistic concurrency for any versioned resource: the expected version of a change, the errors
 * of a stale or missing one, and their HTTP form (ETag and If-Match). Each use case states what it
 * does with a conflict; the mechanism is the same everywhere.
 */
package dev.ylp.shared.concurrency;
