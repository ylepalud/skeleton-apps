# Decisions

The structuring choices, one line each, with the reason. The explanation pages give the details.

| Decision | Reason |
| --- | --- |
| One Gradle monorepo, Kotlin DSL, conventions in `build-logic` | Build logic written once, typed, and tested with TestKit |
| One deployable, Spring Modulith | One JVM fits a small node; module boundaries are verified and keep extraction possible |
| Hexagonal modules with jMolecules annotations | The roles are declared in code and checked by tests rather than guessed from package names |
| One database schema per module | Data isolation follows module isolation |
| Contract first, server interfaces generated | The build breaks when code and contract disagree; every client is generated from the same file |
| Generated server code in its own project (`:contracts:api`) | A plain jar, compiled under the same `-Werror` rules, kept out of coverage and mutation testing |
| `ProblemDetail` errors with a category on business reasons | The domain says what an error is; only the REST adapter knows HTTP |
| JWT resource server from the first commit, mock provider locally | Production security is exercised everywhere; switching providers is configuration only |
| Owner-scoped reads, 404 for someone else's resource | Nobody can learn which resources exist |
| PKCE and refresh tokens in the web app, same origin as the API | No CORS, no iframe in the content security policy |
| Testcontainers everywhere, never H2 | Tests run the real SQL against the real database |
| Jib for the API, nginx-unprivileged for the web app | No Dockerfile for the JVM, non-root images, minimal bases pinned by digest |
| Image names derived from the root project | A generated project gets its own names with no renaming |
| Keyless cosign signatures, SBOM attestations, sign only after the scan | Nothing to leak, and only verified images carry a signature |
| `develop` deployed, `main` for releases, release-please | Continuous delivery on `develop`, versioned releases from Conventional Commits |
| Release by tagging the images built on `develop` | The verified bytes are the shipped bytes |
| Renovate with a seven-day delay, automerge for minor updates | Current dependencies without reviewing every patch |
| Bruno collection generated from the contract and checked in `check` | The collection cannot drift from the API |
| Everything written in English | The repository is public and a template |
| A future CLI in Java or Kotlin lives here; in another language, in its own repository | It is generated from the same contract either way; only the toolchain decides where it lives |
