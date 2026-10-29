# Testing strategy

Each kind of test answers one question, as cheaply as possible.

| Level | Where | Answers | Tools |
| --- | --- | --- | --- |
| Domain and use cases | `services/api/src/test` | Do the rules hold? | JUnit 5, AssertJ, Mockito; no Spring |
| Architecture | `ModularityTest`, `HexagonalArchitectureTest` | Do module and layer boundaries hold? | Spring Modulith, jMolecules ArchUnit |
| Port contracts | `NoteRepositoryContractTest` (abstract, in `src/test`) | Does every adapter of a port behave the same? | Extended by each adapter test |
| Adapters | `services/api/src/integrationTest` | Does the adapter work against the real technology? | Testcontainers PostgreSQL, Liquibase enabled, never H2 |
| Web layer | `NotesControllerTest` | Are routes, statuses, validation and security right? | `@WebMvcTest`, mocked use cases, `jwt()` |
| End to end, API | `NotesEndToEndTest`, `ActuatorTest` | Does the whole chain work with real tokens? | Spring Boot on a random port, Testcontainers for PostgreSQL and the identity provider |
| Web app | `services/api-app/src/**/*.spec.ts` | Do the store and the screens behave? | Vitest, Testing Library, an in-memory fake client |
| End to end, browser | `services/api-app/e2e` | Can a user sign in and manage notes? Does any console error appear? | Playwright against the local stack |
| Native clients | `clients/shared/src/commonTest` | Do the use cases, the HTTP mapping and the sign-in behave? | `kotlin.test`, Ktor `MockEngine`; run on the JVM, against the Android variant on the host, and on the iOS simulator on macOS |
| Native clients architecture | `ArchitectureTest` in `clients/shared/src/jvmTest` | Do the hexagon's boundaries hold, and does the public API stay within Swift export's limits? | Konsist |
| Android app | `clients/android/src/test` | Do the screens drive the notes flow, sign-in and errors included? Do the adapters keep the session? | Robolectric, Compose UI tests, a real `SharedClient` against MockWebServer |
| API collection | `bruno` | Does every operation answer without an auth or server failure? | `bru run` against the local stack |
| Build conventions | `build-logic/src/functionalTest` | Do the plugins configure what they claim, and do the gates fail when they should? | Gradle TestKit |

## Conventions

- Test names state the behaviour (`readingNoteMissingOrNotOwnedFailsAsNotFound`), with a `@DisplayName` sentence and Given / When / Then comments.
- Test data comes from builders that are valid by default (`NoteTestDataBuilder.aNote()`); a test overrides only what it is about.
- Time and identifiers are fixed through `Clock` and `UUIDGenerator`.
- Mockito verifies exact arguments; mocks are built explicitly, without `@InjectMocks`.

## Gates

| Gate | Threshold | When |
| --- | --- | --- |
| JaCoCo, over unit and integration tests | 80% of instructions, branches and lines | `check` |
| PIT, on `domain` and `application` | 80% mutation score | nightly |
| Kover, on the native clients' JVM target and the Android app | 80% of lines and branches | `check` |
| Generated code | excluded from all of them: `:contracts:api` for the server, the `generated` package of `:clients:shared`, and `@Serializable` transport classes | |
