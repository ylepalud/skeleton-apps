# 0011. Testcontainers everywhere, never H2

- Status: accepted
- Date: 2026-10-23

## Context

An in-memory database accepts SQL that PostgreSQL refuses, and refuses SQL that PostgreSQL accepts.

## Options

- Testcontainers PostgreSQL for every test that touches the database
- H2 in PostgreSQL compatibility mode

## Decision

Chosen: Testcontainers PostgreSQL for every test that touches the database.

## Consequences

- Tests run the real SQL against the real database, Liquibase included.
- Tests need Docker and start slower.

More: [Testing strategy](../testing-strategy.md).
