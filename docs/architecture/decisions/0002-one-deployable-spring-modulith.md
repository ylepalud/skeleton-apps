# 0002. One deployable, organised as a Spring Modulith

- Status: accepted
- Date: 2026-10-23

## Context

The application runs on a single small node. It still needs clear business boundaries, so that one module can become a service later.

## Options

- One Spring Boot application, with modules verified by Spring Modulith
- One microservice per business capability
- One application with packages and no enforced boundary

## Decision

Chosen: One Spring Boot application, with modules verified by Spring Modulith.

## Consequences

- One JVM keeps the memory footprint low on a small node.
- `ModularityTest` fails the build when a module reaches into another module's internals, which keeps extraction possible.
- Modules share one release cycle and one failure domain.

More: [Architecture overview](../overview.md#one-deployable-several-modules).
