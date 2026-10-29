# Decisions

The structuring choices, one line each. Each links to its record: the context, the options weighed, and the consequences.

The first records were written on 2026-10-25 from the original one-line table, so their options are a reconstruction rather than minutes of a discussion.

## Add a decision

Copy the last record, number it after it, and add its line below. A decision that replaces another one sets the old record's status to `superseded by NNNN`; the old record stays.

| Record | Decision | Reason |
| --- | --- | --- |
| [0001](decisions/0001-one-gradle-monorepo.md) | One Gradle monorepo, Kotlin DSL, conventions in `build-logic` | Build logic written once, typed, and tested with TestKit |
| [0002](decisions/0002-one-deployable-spring-modulith.md) | One deployable, Spring Modulith | One JVM fits a small node; module boundaries are verified and keep extraction possible |
| [0003](decisions/0003-hexagonal-modules-with-jmolecules.md) | Hexagonal modules with jMolecules annotations | The roles are declared in code and checked by tests rather than guessed from package names |
| [0004](decisions/0004-one-database-schema-per-module.md) | One database schema per module | Data isolation follows module isolation |
| [0005](decisions/0005-contract-first-generated-interfaces.md) | Contract first, server interfaces generated | The build breaks when code and contract disagree; every client is generated from the same file |
| [0006](decisions/0006-generated-server-code-in-its-own-project.md) | Generated server code in its own project (`:contracts:api`) | A plain jar, compiled under the same `-Werror` rules, kept out of coverage and mutation testing |
| [0007](decisions/0007-problem-details-with-error-categories.md) | `ProblemDetail` errors with a category on business reasons | The domain says what an error is; only the REST adapter knows HTTP |
| [0008](decisions/0008-jwt-resource-server-from-the-first-commit.md) | JWT resource server from the first commit, mock provider locally | Production security is exercised everywhere; switching providers is configuration only |
| [0009](decisions/0009-owner-scoped-reads.md) | Owner-scoped reads, 404 for someone else's resource | Nobody can learn which resources exist |
| [0010](decisions/0010-pkce-and-refresh-tokens-same-origin.md) | PKCE and refresh tokens in the web app, same origin as the API | No CORS, no iframe in the content security policy |
| [0011](decisions/0011-testcontainers-never-h2.md) | Testcontainers everywhere, never H2 | Tests run the real SQL against the real database |
| [0012](decisions/0012-jib-and-nginx-unprivileged-images.md) | Jib for the API, nginx-unprivileged for the web app | No Dockerfile for the JVM, non-root images, minimal bases pinned by digest |
| [0013](decisions/0013-image-names-from-the-root-project.md) | Image names derived from the root project | A generated project gets its own names with no renaming |
| [0014](decisions/0014-keyless-signatures-after-the-scan.md) | Keyless cosign signatures, SBOM attestations, sign only after the scan | Nothing to leak, and only verified images carry a signature |
| [0015](decisions/0015-develop-deployed-main-released.md) | `develop` deployed, `main` for releases, release-please | Continuous delivery on `develop`, versioned releases from Conventional Commits |
| [0016](decisions/0016-release-by-tagging-built-images.md) | Release by tagging the images built on `develop` | The verified bytes are the shipped bytes |
| [0017](decisions/0017-renovate-with-delay-and-automerge.md) | Renovate with a seven-day delay, automerge for minor updates | Current dependencies without reviewing every patch |
| [0018](decisions/0018-bruno-collection-generated-and-checked.md) | Bruno collection generated from the contract and checked in `check` | The collection cannot drift from the API |
| [0019](decisions/0019-everything-in-english.md) | Everything written in English | The repository is public and a template |
| [0020](decisions/0020-cli-location-follows-its-toolchain.md) | A future CLI in Java or Kotlin lives here; in another language, in its own repository | It is generated from the same contract either way; only the toolchain decides where it lives |
| [0021](decisions/0021-documentation-by-audience.md) | Documentation organised by audience | Each reader finds the pages written for them |
| [0022](decisions/0022-documentation-site-with-zensical.md) | Documentation site built by Zensical, published to GitHub Pages from `main` | The Markdown stays the source; a strict build catches broken links in the pull request |
| [0023](decisions/0023-domain-model-page-from-jmolecules.md) | Domain model page generated in Mermaid by `ModularityTest` from the module model and the jMolecules annotations | The diagrams cannot drift from the code, and the site renders them without another tool |
| [0024](decisions/0024-native-clients-on-a-hexagonal-kotlin-multiplatform-library.md) | Native clients on a hexagonal Kotlin Multiplatform library, sign-in written in Kotlin | API calls, sign-in and error mapping written and tested once for Android, iOS and the CLI; the apps implement their UI only |
| [0025](decisions/0025-android-app-adapters-and-jvm-ui-tests.md) | Android app: Custom Tabs sign-in, tokens encrypted with an AndroidKeyStore key, UI tests on the JVM with Robolectric | RFC 8252 for native sign-in; the screens and the sign-in flow are tested in `check`, without an emulator |
