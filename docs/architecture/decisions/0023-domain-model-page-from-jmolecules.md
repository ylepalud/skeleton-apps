# 0023. Domain model page generated from the module model and the jMolecules annotations

- Status: accepted
- Date: 2026-10-27

## Context

The documentation site shows no picture of the domain: which modules exist, what each one models, and how a request travels through its hexagon. jMolecules annotations already declare these roles ([0003](0003-hexagonal-modules-with-jmolecules.md)) and Spring Modulith reads them, but its `Documenter` writes PlantUML and AsciiDoc, which Zensical does not render, and its canvases list a module's types without showing how they relate.

## Options

- The Spring Modulith `Documenter` as is, with PlantUML converted to SVG and AsciiDoc to Markdown by two more pinned containers
- Diagrams drawn by hand in the Markdown
- A page generated in Mermaid by `ModularityTest`, from `ApplicationModules` and the jMolecules annotations

## Decision

Chosen: a page generated in Mermaid by `ModularityTest`. Spring Modulith gives the modules, their dependencies, aggregate roots and value objects; the hexagonal roles (`@PrimaryAdapter`, `@PrimaryPort`, `@SecondaryPort`, `@SecondaryAdapter`) are read from the classes and their packages, since Modulith does not model them. A module holding an aggregate root, a value object or a port is a business module and gets a class diagram and a request flow; any other module is cross-cutting and lists its Spring beans, its public types and the modules using it. Each module's description is the first sentence of its package Javadoc, read from the sources, and the test fails when one is missing. The page is written to `docs/architecture/domain-model.md` by `:services:api:docsDomainModel`, on which `docsBuild` and `docsServe` depend, and is not committed. The `Documenter` and `spring-modulith-docs` are removed.

## Consequences

- The page cannot drift from the code: it is generated on every site build, and the test that writes it also checks its content in `check`.
- Value objects need `@ValueObject` and use cases a `@PrimaryPort` package: a role the code does not declare is not drawn. A JPA entity Modulith counts as an aggregate root is left out because it is an adapter type.
- Mermaid renders in the site and on GitHub, follows the site theme, and needs no extra container.
- The generator is our code, in the test sources: a Spring Modulith upgrade that changes its model can break it, and the test shows where.

More: [Domain model](../domain-model.md).
