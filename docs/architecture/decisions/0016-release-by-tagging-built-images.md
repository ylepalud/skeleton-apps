# 0016. Release by tagging the images built on `develop`

- Status: accepted
- Date: 2026-10-23

## Context

Rebuilding an image for a release produces different bytes from the ones that were tested and signed.

## Options

- A release adds its version tag to the images already published from `develop`
- A release rebuilds the images from `main`

## Decision

Chosen: A release adds its version tag to the images already published from `develop`.

## Consequences

- The verified bytes are the shipped bytes.
- Images are tagged by commit SHA, never `latest`.

More: [Supply chain](../supply-chain.md).
