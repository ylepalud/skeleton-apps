# 0004. One database schema per module

- Status: accepted
- Date: 2026-10-23

## Context

Modules share one PostgreSQL database. Without a rule, a query can join any table, and the module boundaries leak through the data.

## Options

- One schema per module, with its own Liquibase changelog
- One shared schema, with table name prefixes
- One database per module

## Decision

Chosen: One schema per module, with its own Liquibase changelog.

## Consequences

- Data isolation follows module isolation: no query joins two schemas, a module reads another one's data through its Java API or its events.
- Extracting a module later moves one schema.
- Reports that span modules need an explicit read model.

More: [Architecture overview](../overview.md#one-deployable-several-modules).
