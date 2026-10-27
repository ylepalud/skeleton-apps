# 0015. `develop` deployed, `main` for releases, release-please

- Status: accepted
- Date: 2026-10-23

## Context

The project wants continuous delivery and versioned releases, without writing changelogs or version numbers by hand.

## Options

- `develop` deployed continuously, `main` for releases, release-please driven by Conventional Commits
- Trunk-based on one branch, releases from tags
- Versions and changelog written by hand

## Decision

Chosen: `develop` deployed continuously, `main` for releases, release-please driven by Conventional Commits.

## Consequences

- Continuous delivery on `develop`, versioned releases from Conventional Commits.
- Two long-lived branches to keep in sync, through merge commits into `main`.

More: [Release](../../operations/release.md).
