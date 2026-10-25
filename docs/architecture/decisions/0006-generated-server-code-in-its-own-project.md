# 0006. Generated server code in its own project

- Status: accepted
- Date: 2026-10-23

## Context

The code generated from the contract must compile under the same strict rules as the rest, but it must not count in coverage or mutation testing.

## Options

- A dedicated project, `:contracts:api`, producing a plain jar
- Generation into the API's own source set, with exclusions in every tool

## Decision

Chosen: A dedicated project, `:contracts:api`, producing a plain jar.

## Consequences

- A plain jar, compiled under the same `-Werror` rules.
- It stays out of coverage and mutation testing with no exclusion list to maintain.
- One more Gradle project.

More: [Contract first](../contract-first.md).
