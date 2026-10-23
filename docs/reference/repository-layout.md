# Repository layout

```
skeleton-apps/
├── AGENTS.md                    instructions for coding agents (CLAUDE.md imports it)
├── build-logic/                 Gradle convention plugins, an included build
│   ├── src/main/kotlin/         one *.gradle.kts file per plugin
│   └── src/functionalTest/      TestKit tests of the plugins
├── gradle/
│   ├── libs.versions.toml       version catalog: every library and Gradle plugin
│   └── gradle-daemon-jvm.properties   JDK of the Gradle daemon (Temurin 25)
├── gradle.properties            build tool versions (JaCoCo, PIT, Node, base image...)
├── settings.gradle.kts          repositories and the list of projects
├── build.gradle.kts             local stack tasks (startLocalStack...)
├── contracts/api/               OpenAPI contract and the :contracts:api project
│   ├── openapi.yaml             entry point: info, servers, security, paths
│   ├── common.yaml              Problem schema and shared responses
│   └── notes.yaml               the notes module: paths and schemas
├── services/api/                Spring Boot API (:services:api)
│   └── src/
│       ├── main/java/dev/ylp/
│       │   ├── ApiApplication.java
│       │   ├── config/          cross-cutting configuration (security, clock, paths)
│       │   ├── shared/          open module: identifiers, error types
│       │   └── notes/           a business module
│       │       ├── domain/      model and ports
│       │       ├── application/ use cases
│       │       └── adapter/     in/rest, out/postgres
│       ├── main/resources/      application.yml, Liquibase changelogs
│       ├── test/                unit tests, architecture tests, port contract tests
│       └── integrationTest/     Testcontainers tests, end to end
├── services/api-app/            Angular web app (:services:api-app)
│   ├── src/app/                 one folder per feature (notes, auth)
│   ├── e2e/                     Playwright tests
│   ├── nginx/                   nginx configuration template of the image
│   └── Dockerfile
├── bruno/                       API collection (:bruno)
│   ├── opencollection.yml       hand-written: auth, shared test
│   ├── environments/            hand-written: Stack, BootRun
│   ├── notes/                   generated from the contract
│   └── scripts/collection.mjs   generation and drift check
├── compose.yaml                 local stack
├── docs/                        this documentation
├── renovate.json                dependency updates
├── release-please-config.json   release configuration
└── .github/
    ├── workflows/               ci, publish, nightly, release
    └── actions/secure-image/    scan, SBOM and signature of an image
```

<!-- template-only:start -->
Template only, left out of generated projects: `scripts/installer/` (the project installer), the root `package.json` and `package-lock.json` (its dependencies), `.github/workflows/installer.yml` and `docs/how-to/start-a-new-project.md`.

<!-- template-only:end -->
Generated and ignored: `services/api-app/src/app/api/generated` (web client), `contracts/api/build/generated` (server code), every `build/` and `node_modules/`.
