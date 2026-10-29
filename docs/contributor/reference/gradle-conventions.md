# Gradle conventions

The build is a Kotlin DSL monorepo. Every project applies one or more convention plugins from `build-logic/src/main/kotlin`, an included build, so build logic is written once and tested once: each plugin has TestKit tests in `build-logic/src/functionalTest` that build throwaway projects, including the cases where a gate must fail.

## Where versions live

| Kind | Where |
| --- | --- |
| Libraries and Gradle plugins | `gradle/libs.versions.toml` |
| Build tools (JaCoCo, google-java-format, PIT, Node, Jib base image) | `gradle.properties`, each with a `# renovate:` comment |
| Android SDK levels (`androidCompileSdk`, `androidMinSdk`, `androidTargetSdk`) | `gradle.properties` |
| Libraries managed by the Spring Boot BOM | declared without a version |
| JDK of the Gradle daemon | `gradle/gradle-daemon-jvm.properties` (Temurin 25) |

## Plugins

| Plugin | Applies to | What it sets up |
| --- | --- | --- |
| `java-convention` | every Java project | Java 25 toolchain (Adoptium), `group = dev.ylp`, `-Werror` with all lint warnings, ErrorProne and NullAway (JSpecify mode) as errors, JUnit Jupiter, Spotless (google-java-format for Java, ktlint for build scripts) |
| `integration-test` | the API | An `integrationTest` suite run after `test` and by `check`; it can reuse the unit test sources (port contract tests, test data builders) |
| `quality-gates` | the API | JaCoCo over both suites, at least 80% of instructions, branches and lines in `check`; PIT on `domain` and `application` at 80%, outside `check` |
| `service-isolation` | the API | Fails `check` when a `:services:*` project depends on another one: deployables talk through contracts only |
| `spring` | the API | Spring Boot with the Boot, Modulith and jMolecules BOMs as Gradle platforms; web, validation, JPA, Liquibase, OAuth2 resource server, Actuator, Prometheus; Modulith, jMolecules, Testcontainers and security test support; `bootRun` on the `local` profile |
| `container-image` | the API | Jib image on the distroless Java 25 `nonroot` base pinned by digest, production classpath only, named `<root project>/<project>`; CI sets the repository, the tag and the source label |
| `openapi-server` | `contracts/api` | Generates Spring MVC interfaces and `*Dto` models from the contract (JSpecify annotations, bean validation) into a library the API depends on |
| `frontend-convention` | the web app | Pinned Node downloaded by the build, `npm ci`, API client generation from the contract, `lint` and `test` in `check`, `build` in `assemble`, Playwright behind `-PrunE2e=true` |
| `kotlin-multiplatform-convention` | `clients/shared` | Kotlin Multiplatform with the `android`, `iosArm64`, `iosSimulatorArm64` and `jvm` targets, the common tests run on each (the JVM target ships nowhere: it carries the architecture tests and the coverage), warnings as errors, kotlinx.serialization, ktlint, Kover at 80% of lines and branches in `check` |
| `openapi-kotlin-client` | `clients/shared` | Generates an internal Ktor client and models from the contract into `commonMain` (`openApiClient { contract; packageName }`), with one HTTP engine per platform; the generated package is out of coverage |
| `android-app-convention` | `clients/android` | Android application with Compose (AGP 9, built-in Kotlin), SDK levels from `gradle.properties`, lint warnings as errors in `check`, JVM tests on the JUnit platform (Jupiter, and Vintage for the JUnit 4 API Robolectric needs) on the debug variant only, Kover at 80% |
| `bruno-collection` | `bruno` | Regenerates the requests from the contract (`updateCollection`) and fails `check` when they drift (`verifyCollection`) |

## Rules worth knowing

- **Configuration cache is on.** A task action must not capture the build script: copy what it needs into local values first. TestKit tests run with the configuration cache, so they catch this.
- **Docker is resolved from the caller's `PATH`** (`dev.ylp.buildlogic.dockerExecutable`): a long-lived Gradle daemon may have a minimal `PATH`.
- **The Android SDK is a prerequisite** of every build: the Android Gradle Plugin configures `clients/shared` even when only the API is built. Set `ANDROID_HOME`.
- **iOS needs macOS with Xcode.** Elsewhere, Kotlin skips the iOS targets.
- **A change to a convention plugin comes with a TestKit test.**
