# Update dependencies

## Renovate does most of it

`renovate.json` configures the Renovate GitHub App (it must be installed on the repository, issue #11):

- pull requests target `develop`, seven days after a version is released;
- patch, minor and digest updates merge automatically once CI passes;
- Spring, Angular, `angular-auth-oidc-client`, PostgreSQL and every major update wait for review;
- updates are grouped: Spring, Angular, GitHub Actions, Bruno tooling.

Renovate also updates what Gradle and npm do not declare: versions in `gradle.properties` that carry a `# renovate:` comment, the Jib base image digest, the images in `compose.yaml`, and the images started by Testcontainers.

## By hand

| What | Where |
| --- | --- |
| A library or a Gradle plugin | `gradle/libs.versions.toml` |
| Spring Boot, and with it every library of its BOM | `spring-boot` in `gradle/libs.versions.toml` |
| A build tool | `gradle.properties` (keep the `# renovate:` comment above the line) |
| The Gradle wrapper | `./gradlew wrapper --gradle-version <version> --gradle-distribution-sha256-sum <sum>` |
| A pinned image | its tag and its `sha256` digest together |
| A GitHub Action | its commit SHA, with the version in a trailing comment |

Then run `./gradlew check`; for an image or a base image, also `./gradlew startLocalStack` and the end-to-end tests.

## npm overrides

`apps/services/api-app/package.json` and `apps/bruno/package.json` force patched versions of a few transitive dependencies through `overrides`, because their direct dependencies pin vulnerable versions. Remove each override once the direct dependency ships the fix (issue #15), and check that `npm audit` stays clean.

## When the scan blocks

If `trivy-fs` or an image scan fails, the fix is almost always an update: find which direct dependency brings the vulnerable version (`./gradlew :apps:services:api:dependencyInsight --dependency <name>` or `npm ls <name>`), and update it, or override the transitive version. See [Read a failing CI](read-a-failing-ci.md).
