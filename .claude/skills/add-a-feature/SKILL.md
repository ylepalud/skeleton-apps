---
name: add-a-feature
description: Add a feature end to end in skeleton-apps, from the contract to the clients. Use when a change spans the API and its clients, such as a new field or behaviour users see.
---

Follow the sequence of [docs/contributor/tutorials/first-feature.md](../../../docs/contributor/tutorials/first-feature.md), which walks one real feature through every layer: contract, collection, domain and use cases, persistence (a new Liquibase changeset), REST adapter, API tests, then the clients and the local stack.
The web app changes with its fake client. <!-- client:web -->
The native clients' library changes too (`apps/clients/shared`: gateway, use case, tests; see docs/architecture/clients.md). <!-- client:native -->
So do the Android screens (`apps/clients/android`) and the iOS ones (`apps/clients/ios`). <!-- client:mobile -->
So does the command line (`apps/clients/cli`). <!-- client:cli -->

Done when:
- each layer changed in that order, and the compiler reports nothing left;
- the behaviour has tests at the level that proves it, including the port contract test for persistence changes;
- a write on a versioned resource takes the version it is based on (`ExpectedVersion`, `If-Match`), its use case states its conflict policy in its Javadoc, and a test proves a stale version changes nothing (docs/architecture/overview.md#optimistic-concurrency);
- in the native clients, a change of a note's fields reaches the local store too (`src/commonMain/sqldelight`, `LocalNote`, `SqlNoteStore`), and a new entity changed offline brings its `ChangeReplayer` with a stated conflict strategy (docs/architecture/clients.md#offline); <!-- client:native -->
- `./gradlew check` passes, and the end-to-end tests pass against `./gradlew startLocalStack`.
