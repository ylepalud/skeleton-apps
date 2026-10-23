# skeleton-apps

[![CI](https://github.com/ylepalud/skeleton-apps/actions/workflows/ci.yml/badge.svg)](https://github.com/ylepalud/skeleton-apps/actions/workflows/ci.yml)
[![Publish](https://github.com/ylepalud/skeleton-apps/actions/workflows/publish.yml/badge.svg?branch=develop)](https://github.com/ylepalud/skeleton-apps/actions/workflows/publish.yml)
[![Nightly](https://github.com/ylepalud/skeleton-apps/actions/workflows/nightly.yml/badge.svg)](https://github.com/ylepalud/skeleton-apps/actions/workflows/nightly.yml)

A starting point for new projects, built to production standards from the first commit: a Spring Boot API organised as a Spring Modulith, an Angular web app, an OpenAPI contract both are generated from, and a delivery pipeline that publishes scanned and signed container images.

It ships one small but complete feature, personal **notes**, so that every convention is visible in working code: contract, hexagonal module, persistence, security, web screen, tests, API collection, images. Infrastructure is out of scope: this repository builds images, a separate repository deploys them.

## Quick start

You need Docker with Compose v2. Gradle downloads its own JDK (Temurin 25) and the Node version the web app needs; a local Node is only required for the web app's development server.

```bash
./gradlew check            # build and verify everything
./gradlew startLocalStack  # run the full stack locally
```

Then open http://localhost:8088 and sign in with any username: locally, a mock identity provider accepts everyone. `./gradlew stopLocalStack` stops the stack.

<!-- template-only:start -->
## Start a new project

```bash
npx github:ylepalud/skeleton-apps#v0.1.0 my-idea
```

The installer renames the project, the package and the images, and writes `my-idea.zip`: a ready Git repository with one `Init` commit. See [Start a new project](docs/how-to/start-a-new-project.md).

<!-- template-only:end -->

## What is inside

| Path | Content |
| --- | --- |
| `services/api` | Spring Boot API, one Spring Modulith application with the `notes` module |
| `services/api-app` | Angular web app, served by nginx in its image |
| `contracts/api` | OpenAPI contract, and the server interfaces generated from it |
| `bruno` | API collection generated from the contract |
| `build-logic` | Gradle convention plugins shared by every project |
| `compose.yaml` | Local stack: Postgres, a mock identity provider, the API and the web app |
| `.github` | CI, image publication, nightly mutation testing, releases |

Details: [Repository layout](docs/reference/repository-layout.md).

## How it works

**Contract first.** The OpenAPI contract in `contracts/api` is the source of truth. The API implements interfaces generated from it, so the build breaks when code and contract disagree; the web app's client and the Bruno collection are generated from the same contract. [Why](docs/explanation/contract-first.md).

**Modules, hexagonal inside.** The API is a Spring Modulith: each business module is a top-level package with its own database schema. Inside a module, the domain and the use cases depend on no framework; adapters plug them into HTTP and PostgreSQL. Both rules are checked by tests. [Architecture](docs/explanation/architecture.md).

**Secure by default.** Every API route requires a token from an OpenID Connect provider; each note belongs to the token's subject, and someone else's note does not exist for you. The web app signs in with Authorization Code and PKCE, and shares its origin with the API. [Security](docs/explanation/security.md).

**Tested at every level.** The domain is tested without Spring, adapters against a real PostgreSQL, the whole chain end to end with real tokens, the web app with Vitest and Playwright, and even the build conventions with Gradle TestKit. [Testing strategy](docs/explanation/testing-strategy.md).

**Delivered as signed images.** A push to `develop` publishes the changed images to GitHub Container Registry, scanned, with an attested SBOM, and signed with cosign. A release adds its version tag to those exact images. [Supply chain](docs/explanation/supply-chain.md).

## Documentation

| Section | For when you want to |
| --- | --- |
| [Tutorials](docs/tutorials/) | learn by building: [Add your first feature](docs/tutorials/first-feature.md) |
| [How-to guides](docs/how-to/) | get a task done: run locally, change the contract, add a module, release, troubleshoot |
| [Reference](docs/reference/) | look something up: commands, conventions, CI/CD, local stack, configuration, errors |
| [Explanation](docs/explanation/) | understand the design and the decisions behind it |

Coding agents start with [AGENTS.md](AGENTS.md).

## Contributing

Pull requests target `develop` and need the `check`, `e2e`<!-- template-only:start -->, `installer`<!-- template-only:end --> and `trivy-fs` checks. Commits follow [Conventional Commits](https://www.conventionalcommits.org/), which drive the changelog and the version. Everything is written in English.
