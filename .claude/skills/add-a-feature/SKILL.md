---
name: add-a-feature
description: Add a feature end to end in skeleton-apps, from the contract to the web app. Use when a change spans the API and the web app, such as a new field or behaviour users see.
---

Follow the sequence of [docs/contributor/tutorials/first-feature.md](../../../docs/contributor/tutorials/first-feature.md), which walks one real feature through every layer: contract, collection, domain and use cases, persistence (a new Liquibase changeset), REST adapter, API tests, web app and its fake client, then the local stack.

Done when:
- each layer changed in that order, and the compiler reports nothing left;
- the behaviour has tests at the level that proves it, including the port contract test for persistence changes;
- `./gradlew check` passes, and the end-to-end tests pass against `./gradlew startLocalStack`.
