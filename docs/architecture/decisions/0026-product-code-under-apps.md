# 0026. All product code under `apps/`

- Status: accepted
- Date: 2026-10-30

## Context

The repository root mixed the product code (`services/`, `clients/`, `contracts/`, `bruno/`) with the build tooling, the documentation and the repository configuration, and the iOS app would have added one more folder (issue #89).

## Options

- One `apps/` folder, an implicit Gradle parent project in the same build, keeping the `services`, `clients` and `contracts` levels
- The same folder as a separate build, included from a thin root build
- Flat project paths under one parent (`:apps:api`, `:apps:shared`...)
- No move: an aggregating project that depends on every module's tasks

## Decision

Chosen: one `apps/` folder, an implicit Gradle parent project in the same build, keeping the `services`, `clients` and `contracts` levels. `:services:api` becomes `:apps:services:api`.

`apps/` holds all product code, and Gradle builds everything it can. The iOS app (`apps/clients/ios`) is the only folder that is not a Gradle project; a Gradle task still drives its build. `build-logic`, the root `build.gradle.kts` tasks, `compose.yaml`, `docs/` and `scripts/` stay at the root.

## Consequences

- The root holds the build tooling, the documentation and the repository configuration only; a new module or app goes under `apps/`.
- Every Gradle path is one level longer, in commands, CI and docs.
- One build stays one build: no included-build boundary for tasks, the configuration cache or the IDE.

More: [Repository layout](../../contributor/reference/repository-layout.md).
