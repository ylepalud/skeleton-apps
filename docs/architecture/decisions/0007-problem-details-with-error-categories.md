# 0007. ProblemDetail errors, with a category on business reasons

- Status: accepted
- Date: 2026-10-23

## Context

Errors must have one shape for every client, and the domain must say what an error is without knowing HTTP.

## Options

- RFC 9457 `ProblemDetail`, business reasons declaring a category that only the REST adapter maps to a status
- Domain exceptions carrying HTTP statuses
- One exception class per error, each mapped by hand

## Decision

Chosen: RFC 9457 `ProblemDetail`, business reasons declaring a category that only the REST adapter maps to a status.

## Consequences

- The domain says what an error is; only the REST adapter knows HTTP.
- Clients rely on a stable `errorCode` rather than on messages.
- A new category needs a change in the shared handler.

More: [API errors](../../product/api-errors.md).
