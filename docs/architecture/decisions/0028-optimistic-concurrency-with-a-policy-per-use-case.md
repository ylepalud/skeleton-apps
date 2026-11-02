# 0028. Optimistic concurrency, with a conflict policy per use case

- Status: accepted
- Date: 2026-11-02

## Context

A note could be changed on the web, on Android and on iOS; the last save silently overwrote the others, and offline use (#71) would make it common. Issue #70 asks for versions. Notes are the only resource today, but every future resource a user changes from several places has the same problem, and what to do about a conflict depends on the use case, not on the resource type.

## Options

For the mechanism:

- A version per resource, exposed as `version` and as a strong `ETag`; changes require `If-Match` (428 without it, 412 when stale)
- The same, with `If-Match` optional
- Comparing `updatedAt` timestamps

For the conflict policy:

- One policy for the whole API
- A policy per use case, stated in its Javadoc and tested

## Decision

Chosen: a version per resource, `If-Match` required, and a policy per use case. A required `If-Match` makes a blind overwrite impossible rather than merely discouraged; `/api/v1` keeps its path because every client lives in this repository and nothing is deployed yet. The version is also a field of the body, so a client editing from a list, or caching notes offline, has it without another request. Timestamps were rejected: two changes in the same millisecond, or clocks that differ, defeat them. The mechanism is shared (`dev.ylp.shared.concurrency`, `common.yaml`); the policy is a decision of each use case, because "refuse and show the latest version" suits a note's text but not, for instance, an append-only record. Notes refuse a stale update or deletion and show the latest version.

## Consequences

- Every write on a versioned resource takes an `ExpectedVersion`; the aggregate checks it before changing, and JPA's `@Version` catches a change committed between the read and the write.
- `ErrorCategory` gains `STALE_VERSION` (412) and `VERSION_REQUIRED` (428), with the shared codes `VERSION_MISMATCH` and `VERSION_REQUIRED`.
- The clients keep the version of what they show; the native clients receive `NoteFailure.Changed`, the web app the `VERSION_MISMATCH` code.
- The `add-a-feature` and `add-a-module` skills and their docs ask for the policy of each new write use case.

More: [Optimistic concurrency](../overview.md#optimistic-concurrency), [API errors](../../product/api-errors.md).
