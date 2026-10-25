# 0012. Jib for the API, nginx-unprivileged for the web app

- Status: accepted
- Date: 2026-10-23

## Context

The images must be small, run as non-root, and be rebuilt reproducibly from pinned bases.

## Options

- Jib on a distroless base for the API, `nginx-unprivileged` with a Dockerfile for the web app
- A hand-written Dockerfile for both
- Cloud Native Buildpacks

## Decision

Chosen: Jib on a distroless base for the API, `nginx-unprivileged` with a Dockerfile for the web app.

## Consequences

- No Dockerfile for the JVM, non-root images, minimal bases pinned by digest.
- The API image has no shell, which makes debugging inside the container harder.

More: [Supply chain](../supply-chain.md).
