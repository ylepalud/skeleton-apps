# CI/CD

All workflows live in `.github/workflows`. Actions are pinned by commit SHA (Renovate keeps them current), and each job gets the minimal permissions it needs.

## Workflows

| Workflow | Trigger | Jobs |
| --- | --- | --- |
| `ci.yml` | pull requests to `develop` or `main`, push to `main`, called by `publish.yml` | `check` (`./gradlew check`), `e2e` (local stack, Playwright, Bruno smoke run), `trivy-fs` (dependencies including development ones, secrets and configuration; findings in the Security tab) |
| `publish.yml` | push to `develop`, or manual with `all=true` | `ci.yml`, then one image job per image whose code changed: build and push, scan, SBOM, signature |
| `nightly.yml` | every day at 03:00 UTC, or manual | `./gradlew pitest` on `develop` |
| `release.yml` | push to `main` | release-please, then the release tag added to the published images |

## Images

| Image | Built by | Name |
| --- | --- | --- |
| API | Jib | `ghcr.io/ylepalud/skeleton-apps/api:<commit sha>` |
| Web app | `docker buildx` | `ghcr.io/ylepalud/skeleton-apps/api-app:<commit sha>` |

Path filters decide which images a push to `develop` publishes: the API for `services/api`, `contracts`, `build-logic` and the Gradle files; the web app for `services/api-app`, `contracts` and its convention. Tags are immutable commit SHAs; a release adds `vX.Y.Z` to the same image. There is no `latest`.

The `.github/actions/secure-image` action runs for each image:

1. Trivy scans the pushed image and fails on fixable `HIGH` or `CRITICAL` vulnerabilities.
2. Syft produces an SPDX SBOM, uploaded as an artifact.
3. cosign signs the image and attests the SBOM, keyless, with the workflow's GitHub OIDC identity.

An image that fails its scan stays unsigned. To check a signature:

```bash
cosign verify ghcr.io/ylepalud/skeleton-apps/api@<digest> \
  --certificate-identity-regexp '^https://github.com/ylepalud/skeleton-apps/' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com
```

`cosign verify-attestation --type spdxjson` with the same options checks the SBOM.

## Branches and rulesets

| Branch | Role | Merge method | Rules |
| --- | --- | --- | --- |
| `develop` | default branch, the only one deployed | squash | pull request required, `check`, `e2e` and `trivy-fs` required, linear history, no force push or deletion |
| `main` | releases | merge commit | pull request required, the same checks, no force push or deletion |

Repository administrators can bypass the rulesets; this is how release-please's pull requests get merged (see [Release](../how-to/release.md)).

## Repository settings

Public template repository. Secret scanning with push protection, Dependabot alerts (updates come from Renovate), read-only `GITHUB_TOKEN` by default, workflows allowed to open pull requests (for release-please).
