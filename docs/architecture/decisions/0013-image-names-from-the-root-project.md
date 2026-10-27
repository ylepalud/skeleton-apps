# 0013. Image names derived from the root project

- Status: accepted
- Date: 2026-10-23

## Context

Projects generated from this template must publish their own images without renaming anything by hand.

## Options

- Image names computed from the root project and the repository
- Image names written in the build scripts and workflows

## Decision

Chosen: Image names computed from the root project and the repository.

## Consequences

- A generated project gets its own names with no renaming.

More: [Gradle conventions](../../contributor/reference/gradle-conventions.md).
