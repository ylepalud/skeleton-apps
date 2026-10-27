# 0014. Keyless cosign signatures, SBOM attestations, sign only after the scan

- Status: accepted
- Date: 2026-10-23

## Context

Consumers of the images must be able to check where they come from and what they contain. A signing key is a secret that can leak.

## Options

- Keyless cosign signatures and SBOM attestations through the workflow's OIDC identity, only after a passing scan
- Signatures with a long-lived key stored as a secret
- No signature

## Decision

Chosen: Keyless cosign signatures and SBOM attestations through the workflow's OIDC identity, only after a passing scan.

## Consequences

- Nothing to leak, and only verified images carry a signature.
- Verification depends on Sigstore's public infrastructure.

More: [Supply chain](../supply-chain.md).
