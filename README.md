# skeleton-apps

[![CI](https://github.com/ylepalud/skeleton-apps/actions/workflows/ci.yml/badge.svg)](https://github.com/ylepalud/skeleton-apps/actions/workflows/ci.yml)
[![Publish](https://github.com/ylepalud/skeleton-apps/actions/workflows/publish.yml/badge.svg?branch=develop)](https://github.com/ylepalud/skeleton-apps/actions/workflows/publish.yml)
[![Nightly](https://github.com/ylepalud/skeleton-apps/actions/workflows/nightly.yml/badge.svg)](https://github.com/ylepalud/skeleton-apps/actions/workflows/nightly.yml)

A starting point for new projects, built to production standards from the first commit: a Spring Boot API organised as a Spring Modulith, an Angular web app, an OpenAPI contract both are generated from, and a delivery pipeline that publishes scanned and signed container images.

It ships one small but complete feature, personal **notes**, so that every convention is visible in working code: contract, hexagonal module, persistence, security, web screen, tests, API collection, images. Infrastructure is out of scope: this repository builds images, a separate repository deploys them.

## Quick start

You need Docker with Compose v2 and the Android SDK (`ANDROID_HOME` set; the command-line tools are enough). Gradle downloads its own JDK (Temurin 25) and the Node version the web app needs; a local Node is only required for the web app's development server. Building and testing for iOS needs macOS with Xcode.

```bash
./gradlew check            # build and verify everything
./gradlew startLocalStack  # run the full stack locally
./gradlew docsServe        # preview the documentation site on http://localhost:8000
```

Then open <http://localhost:8088> and sign in with any username: locally, a mock identity provider accepts everyone. `./gradlew stopLocalStack` stops the stack.

<!-- template-only:start -->
## Start a new project

```bash
npx github:ylepalud/skeleton-apps my-idea
```

The installer renames the project, the package and the images, and writes `my-idea.zip`: a ready Git repository with one `Init` commit. See [Start a new project](docs/template/start-a-new-project.md).

<!-- template-only:end -->

## What is inside

| Path | Content |
| --- | --- |
| `services/api` | Spring Boot API, one Spring Modulith application with the `notes` module |
| `services/api-app` | Angular web app, served by nginx in its image |
| `contracts/api` | OpenAPI contract, and the server interfaces generated from it |
| `clients/android` | Android app on the shared library: Compose screens, Custom Tabs sign-in, Keystore session |
| `clients/shared` | Kotlin Multiplatform library of the native clients: generated API client, sign-in, use cases |
| `bruno` | API collection generated from the contract |
| `build-logic` | Gradle convention plugins shared by every project |
| `compose.yaml` | Local stack: Postgres, a mock identity provider, the API and the web app |
| `.github` | CI, image publication, nightly mutation testing, releases |

Details: [Repository layout](docs/contributor/reference/repository-layout.md).

## How it works

**Contract first.** The OpenAPI contract in `contracts/api` is the source of truth. The API implements interfaces generated from it, so the build breaks when code and contract disagree; the web app's client and the Bruno collection are generated from the same contract. [Why](docs/architecture/contract-first.md).

**Modules, hexagonal inside.** The API is a Spring Modulith: each business module is a top-level package with its own database schema. Inside a module, the domain and the use cases depend on no framework; adapters plug them into HTTP and PostgreSQL. Both rules are checked by tests. [Architecture](docs/architecture/overview.md).

**Secure by default.** Every API route requires a token from an OpenID Connect provider; each note belongs to the token's subject, and someone else's note does not exist for you. The web app signs in with Authorization Code and PKCE, and shares its origin with the API. [Security](docs/architecture/security.md).

**Tested at every level.** The domain is tested without Spring, adapters against a real PostgreSQL, the whole chain end to end with real tokens, the web app with Vitest and Playwright, and even the build conventions with Gradle TestKit. [Testing strategy](docs/architecture/testing-strategy.md).

**Delivered as signed images.** A push to `develop` publishes the changed images to GitHub Container Registry, scanned, with an attested SBOM, and signed with cosign. A release adds its version tag to those exact images. [Supply chain](docs/architecture/supply-chain.md).

## Documentation

One section per reader; [docs/README.md](docs/README.md) lists them. The same pages, with an API reference, can be published as a [site](docs/operations/ci-cd.md#documentation-site)<!-- template-only:start -->; this repository's is <https://ylepalud.github.io/skeleton-apps/><!-- template-only:end -->. Locally, `./gradlew docsServe` serves it on <http://localhost:8000>, rebuilt on every change to `docs/`.

| Section | For |
| --- | --- |
| [Product](docs/product/README.md) | using the web app and the API: sign-in, notes, errors |
| [Template](docs/template/README.md) | the template this code base comes from<!-- template-only:start -->, and how to start a new project from it<!-- template-only:end --> |
| [Contributor](docs/contributor/README.md) | changing the code: [Add your first feature](docs/contributor/tutorials/first-feature.md), how-to guides, reference |
| [Operations](docs/operations/README.md) | keeping it delivering: CI/CD, releases, dependencies, a red build |
| [Architecture](docs/architecture/README.md) | understanding the design, and the [decision records](docs/architecture/decisions.md) behind it |

Coding agents start with [AGENTS.md](AGENTS.md).

## Contributing

Pull requests target `develop` and need the `check`, `e2e`<!-- template-only:start -->, `installer`<!-- template-only:end --> and `trivy-fs` checks. Commits follow [Conventional Commits](https://www.conventionalcommits.org/), which drive the changelog and the version. Everything is written in English.
