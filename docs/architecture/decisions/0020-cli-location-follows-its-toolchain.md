# 0020. A future CLI lives here in Java or Kotlin, in its own repository otherwise

- Status: accepted
- Date: 2026-10-23

## Context

A command-line client will call the API. It is generated from the same contract whatever its language.

## Options

- In this monorepo when written in Java or Kotlin, in its own repository when written in another language
- Always in this monorepo
- Always in its own repository

## Decision

Chosen: In this monorepo when written in Java or Kotlin, in its own repository when written in another language.

## Consequences

- It is generated from the same contract either way; only the toolchain decides where it lives.

More: [Contract first](../contract-first.md).
