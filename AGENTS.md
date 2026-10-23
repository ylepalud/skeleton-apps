# AGENTS.md

Gradle monorepo: a Spring Boot API organised as a Spring Modulith (`services/api`), an Angular web app (`services/api-app`), both generated from one OpenAPI contract (`contracts/api`), delivered as signed images. Overview: [README.md](README.md).

## Rules

- **English** for everything written: code, comments, docs, commits, pull requests.
- **Contract first.** An API change starts in `contracts/api`, then `./gradlew :bruno:updateCollection`, then code.
- **Tests with every change**, at the level that proves it; `./gradlew check` passes before any commit.
- **Module boundaries and hexagonal layers are enforced** by `ModularityTest` and `HexagonalArchitectureTest`: fix the design, not the test.
- **Ownership**: an owner reaches a use case only through `CurrentOwner`; someone else's resource answers 404.
- **Secrets** live in environment variables, never in files.
- **Conventional Commits**, pull requests to `develop`.

## Before you…

| …do this | read |
| --- | --- |
| build, test or run anything | [docs/reference/commands.md](docs/reference/commands.md) |
| change an endpoint, a payload or an error | [docs/how-to/change-the-api-contract.md](docs/how-to/change-the-api-contract.md) |
| add a feature across layers | [docs/tutorials/first-feature.md](docs/tutorials/first-feature.md) |
| add a module | [docs/how-to/add-a-module.md](docs/how-to/add-a-module.md) |
| touch domain, use cases or adapters | [docs/explanation/architecture.md](docs/explanation/architecture.md) |
| write or fix tests | [docs/explanation/testing-strategy.md](docs/explanation/testing-strategy.md) |
| touch security, tokens or limits | [docs/explanation/security.md](docs/explanation/security.md) |
| change a convention plugin or the build | [docs/reference/gradle-conventions.md](docs/reference/gradle-conventions.md) |
| change CI, images or releases | [docs/reference/ci-cd.md](docs/reference/ci-cd.md) |
| update a dependency | [docs/how-to/update-dependencies.md](docs/how-to/update-dependencies.md) |
| debug a red CI | [docs/how-to/read-a-failing-ci.md](docs/how-to/read-a-failing-ci.md) |
| debug the local build or stack | [docs/how-to/troubleshoot.md](docs/how-to/troubleshoot.md) |
