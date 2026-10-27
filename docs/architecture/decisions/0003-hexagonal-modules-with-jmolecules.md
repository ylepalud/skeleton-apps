# 0003. Hexagonal modules, roles declared with jMolecules

- Status: accepted
- Date: 2026-10-23

## Context

Inside a module, the domain and the use cases must not depend on frameworks. A convention known only from package names is broken silently.

## Options

- Hexagonal layers, roles declared with jMolecules annotations and checked by `jmolecules-archunit`
- Hexagonal layers inferred from package names, checked by hand-written ArchUnit rules
- Layered architecture, with the domain using Spring and JPA directly

## Decision

Chosen: Hexagonal layers, roles declared with jMolecules annotations and checked by `jmolecules-archunit`.

## Consequences

- The roles are declared in code and checked by `HexagonalArchitectureTest`, rather than guessed from package names.
- The domain is tested without Spring.
- Each adapter maps between its own model and the domain, which costs some code.

More: [Architecture overview](../overview.md#hexagonal-inside-a-module).
