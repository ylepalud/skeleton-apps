# 0029. Offline native clients: a local store, a queue of changes, a conflict strategy per entity

- Status: accepted
- Date: 2026-11-02

## Context

The native clients stopped working without network: every screen called the API. Issue #71 asks them to read cached notes offline, queue changes, replay them in order with their `If-Match` ([ADR 0028](0028-optimistic-concurrency-with-a-policy-per-use-case.md)) when the network returns, and never lose a change the API refuses. How to resolve such a conflict depends on the entity.

## Options

For the writes:

- Every write goes to the local store and the queue, then the queue replays at once when it can
- Direct calls online, the queue only offline

For a note created offline:

- A local id, replaced by the API's id on replay
- An id chosen by the client, with a create that is idempotent on the API (`PUT /notes/{id}` with `If-None-Match: *`)

For the replay trigger:

- Opportunistic in `shared` (before a read, after a write), plus the apps calling `SyncChanges` when the network returns or the app comes back
- A connectivity port in `shared`, watched by a background coroutine

## Decision

Chosen: every write through the local store and the queue, a local id remapped on replay, and opportunistic replay called by the apps on network changes.

- **One path**: the same code runs online and offline, and is tested once. On mobile, an online 412 is a conflict like an offline one; the web app keeps ADR 0028's "refuse and show the latest version".
- **A coalesced queue**: one pending change per entity, merged with the next (create then update is one create, create then delete is nothing), replayed in the order of each entity's first change. The API never receives a state the user already replaced.
- **A strategy per entity**: the queue and its replay (`dev.ylp.client.sync`) know no entity; each entity plugs in a `ChangeReplayer` that sends its changes and turns a refusal into its own conflict. Notes keep the refused change next to the API's state: the user keeps theirs, keeps the API's, or keeps both (the change becomes a new note); a note deleted elsewhere can be written again or let go.
- **A local id**, so the contract does not change. The trade-off: if a create reaches the API and its answer is lost, the replay creates the note twice. An idempotent create removes that risk at the cost of a contract change; it is the path if duplicates show up.
- **No connectivity port**: the apps already know when the network returns, and a background coroutine in `shared` would be harder to bridge to Swift.
- **The data belongs to one user**: sign-out wipes the store, and a sign-in by another subject (the ID token's `sub`) starts from an empty one, so nobody reads or replays someone else's notes.

## Consequences

- `shared` depends on SQLDelight; its generated code is made `internal`, out of the API Swift export publishes. Each app opens the store (`openLocalStore`) and passes it to `SharedClient`.
- `UpdateNote` and `DeleteNote` no longer take a version: the store knows what each change is based on. A note carries a `SyncState`; `GetConflict`, `ResolveConflict`, `SyncChanges` and `CountPendingChanges` join the use cases.
- A local write waits for at most one call of a replay in progress: both hold the store's lock.
- A new entity that works offline adds its tables, its `ChangeReplayer` and its conflict strategy, and states that strategy like a use case states its policy.

More: [Native clients, offline](../clients.md#offline).
