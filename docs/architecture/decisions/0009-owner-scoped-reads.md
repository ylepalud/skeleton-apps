# 0009. Owner-scoped reads, 404 for someone else's resource

- Status: accepted
- Date: 2026-10-23

## Context

Every resource belongs to the subject of a token. Answering 403 for someone else's resource tells the caller that it exists.

## Options

- Every read scoped to the owner; someone else's resource answers 404, like a missing one
- A permission check after loading, answering 403

## Decision

Chosen: Every read scoped to the owner; someone else's resource answers 404, like a missing one.

## Consequences

- Nobody can learn which resources exist.
- `CurrentOwner` is the only way an owner enters a use case.
- Sharing a resource with someone later needs an explicit model.

More: [Security](../security.md#authorization).
