# AGENTS.md

Gradle monorepo: a Spring Boot API organised as a Spring Modulith (`apps/services/api`), an Angular web app (`apps/services/api-app`) and a Kotlin Multiplatform library for the native clients (`apps/clients/shared`), all generated from one OpenAPI contract (`apps/contracts/api`), delivered as signed images. Overview: [README.md](README.md).

## Rules

- **English** for everything written: code, comments, docs, commits, pull requests.
- **Product code lives in `apps/`**, one Gradle project per app or library; Gradle builds everything, and the iOS app (`apps/clients/ios`) is the only folder that is not a Gradle project, driven by the root build.
- **Contract first.** An API change starts in `apps/contracts/api`, then `./gradlew :apps:bruno:updateCollection`, then code.
- **Tests with every change**, at the level that proves it; `./gradlew check` passes before any commit.
- **Module boundaries and hexagonal layers are enforced** by `ModularityTest` and `HexagonalArchitectureTest`: fix the design, not the test.
- **Ownership**: an owner reaches a use case only through `CurrentOwner`; someone else's resource answers 404.
- **Secrets** live in environment variables, never in files.
- **Documentation with every change**, in the same pull request: the site in `docs/` and its nav, an ADR for a structuring choice, the README, and how to start locally anything new that runs.
- **Conventional Commits**, pull requests to `develop`.

## Before you…

| …do this | read |
| --- | --- |
| build, test or run anything | [docs/contributor/reference/commands.md](docs/contributor/reference/commands.md) |
| change an endpoint, a payload or an error | [docs/contributor/how-to/change-the-api-contract.md](docs/contributor/how-to/change-the-api-contract.md) |
| add a feature across layers | [docs/contributor/tutorials/first-feature.md](docs/contributor/tutorials/first-feature.md) |
| add a module | [docs/contributor/how-to/add-a-module.md](docs/contributor/how-to/add-a-module.md) |
| touch domain, use cases or adapters | [docs/architecture/overview.md](docs/architecture/overview.md) |
| write or fix tests | [docs/architecture/testing-strategy.md](docs/architecture/testing-strategy.md) |
| touch the native clients (`apps/clients/`) | [docs/architecture/clients.md](docs/architecture/clients.md) |
| run the Android app | [docs/contributor/how-to/run-the-android-app.md](docs/contributor/how-to/run-the-android-app.md) |
| run the iOS app | [docs/contributor/how-to/run-the-ios-app.md](docs/contributor/how-to/run-the-ios-app.md) |
| touch security, tokens or limits | [docs/architecture/security.md](docs/architecture/security.md) |
| change a convention plugin or the build | [docs/contributor/reference/gradle-conventions.md](docs/contributor/reference/gradle-conventions.md) |
| change CI, images or releases | [docs/operations/ci-cd.md](docs/operations/ci-cd.md) |
| update a dependency | [docs/operations/update-dependencies.md](docs/operations/update-dependencies.md) |
| debug a red CI | [docs/operations/read-a-failing-ci.md](docs/operations/read-a-failing-ci.md) |
| work on a sub-issue of an epic | [docs/contributor/how-to/track-an-epic.md](docs/contributor/how-to/track-an-epic.md) |
| debug the local build or stack | [docs/operations/troubleshoot.md](docs/operations/troubleshoot.md) |
