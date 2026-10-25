# 0005. Contract first, server interfaces generated

- Status: accepted
- Date: 2026-10-23

## Context

The API has several consumers: the web app, the Bruno collection, a future CLI. They must agree with the server on paths, payloads and errors.

## Options

- OpenAPI contract written first, server interfaces and clients generated from it
- Code first, contract generated from annotations
- Contract written by hand next to the code, not enforced

## Decision

Chosen: OpenAPI contract written first, server interfaces and clients generated from it.

## Consequences

- The build breaks when code and contract disagree.
- Every client is generated from the same file.
- A contract change shows up in review before any code.
- Writing OpenAPI by hand is more verbose than annotating code.

More: [Contract first](../contract-first.md).
