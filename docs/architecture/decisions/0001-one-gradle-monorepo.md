# 0001. One Gradle monorepo with conventions in build-logic

- Status: accepted
- Date: 2026-10-23

## Context

The API, the web app, the contract and the API collection change together. Their build logic must be written once, kept consistent and checked.

## Options

- One Gradle monorepo, Kotlin DSL, convention plugins in the included build `build-logic`
- One repository per service, each with its own build
- A monorepo with build logic in `buildSrc` or copied between scripts

## Decision

Chosen: One Gradle monorepo, Kotlin DSL, convention plugins in the included build `build-logic`.

## Consequences

- Build logic is written once, typed, and tested with TestKit, including the cases where a gate must fail.
- Every project shares one version catalog and one CI.
- Every contributor needs Gradle, even to work on the web app alone; the build downloads Node for them.

More: [Gradle conventions](../../contributor/reference/gradle-conventions.md).
