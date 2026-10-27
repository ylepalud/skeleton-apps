# 0017. Renovate with a seven-day delay, automerge for minor updates

- Status: accepted
- Date: 2026-10-23

## Context

Dependencies must stay current without a review for every patch, and without taking a compromised release on its first day.

## Options

- Renovate, updates proposed after seven days, patch and minor merged once CI passes, majors and core frameworks reviewed
- Dependabot
- Manual updates

## Decision

Chosen: Renovate, updates proposed after seven days, patch and minor merged once CI passes, majors and core frameworks reviewed.

## Consequences

- Current dependencies without reviewing every patch.
- The CI must be trusted to catch a bad update.

More: [Update dependencies](../../operations/update-dependencies.md).
