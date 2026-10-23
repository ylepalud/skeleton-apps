---
name: add-a-module
description: Add a business module to the skeleton-apps API. Use when a new capability needs its own package, database schema and contract tag rather than a change to an existing module.
---

Follow [docs/how-to/add-a-module.md](../../../docs/how-to/add-a-module.md), copying the shape of the `notes` module. Background on boundaries and layers: [docs/explanation/architecture.md](../../../docs/explanation/architecture.md).

Done when:
- the module has its package with `@ApplicationModule`, its own schema and changelog, its contract file and tag;
- each port has an abstract contract test, extended by its adapter's Testcontainers test;
- `ModularityTest` and `HexagonalArchitectureTest` pass without changes to them;
- `./gradlew check` passes.
