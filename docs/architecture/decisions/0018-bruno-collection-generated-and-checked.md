# 0018. Bruno collection generated from the contract and checked in `check`

- Status: accepted
- Date: 2026-10-23

## Context

A hand-maintained API collection drifts from the API.

## Options

- The collection generated from the contract, `check` failing on drift
- A hand-maintained collection
- No collection

## Decision

Chosen: The collection generated from the contract, `check` failing on drift.

## Consequences

- The collection cannot drift from the API.
- Hand-written parts (auth, environments) live apart from the generated requests.

More: [Contract first](../contract-first.md).
