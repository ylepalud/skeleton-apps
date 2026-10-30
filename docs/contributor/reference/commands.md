# Commands

Run from the repository root. The Gradle tasks are the source of truth: `./gradlew tasks --all` lists them.

## Build and verify

| Command | What it does |
| --- | --- |
| `./gradlew check` | Everything a pull request needs: compilation with ErrorProne and NullAway, Spotless, unit and integration tests, coverage gate, architecture tests, the web app's lint and tests, the Bruno drift check, the TestKit tests of the conventions, and the iOS app's lint, tests and coverage on a Mac with Xcode |
| `./gradlew :apps:services:api:check` | The same for the API only |
| `./gradlew :apps:clients:shared:check` | The native clients' library: JVM and Android host tests, iOS simulator tests on macOS with Xcode, Konsist, Kover, ktlint |
| `./gradlew :apps:clients:android:check` | The Android app: lint, Robolectric tests, coverage, ktlint |
| `./gradlew iosLint iosTest iosVerifyCoverage` | The iOS app, on macOS with Xcode: swift-format, unit and UI tests on the simulator, coverage; skipped elsewhere |
| `./gradlew :build-logic:functionalTest` | The TestKit tests of the convention plugins only |
| `./gradlew pitest` | Mutation testing (80% threshold), outside `check` |
| `./gradlew spotlessApply` | Formats Java and Kotlin sources; `:build-logic:spotlessApply` formats the Kotlin build code |

## Run

| Command | What it does |
| --- | --- |
| `./gradlew startLocalStack` | Builds both images and starts the whole stack, then waits for the API to be ready |
| `./gradlew stopLocalStack` | Stops the stack, keeps the database |
| `./gradlew resetDatabase` | Stops the stack and deletes the database |
| `./gradlew :apps:services:api:bootRun` | Runs the API from source on port 8080, with Postgres and the identity provider started by Docker Compose |
| `./gradlew :apps:clients:android:installDebug` | Installs the Android app on the running emulator; see [Run the Android app](../how-to/run-the-android-app.md) |
| `./gradlew iosRun` | Builds the iOS app and launches it on the simulator; see [Run the iOS app](../how-to/run-the-ios-app.md) |
| `./gradlew iosGenerateProject` | Generates `apps/clients/ios/Notes.xcodeproj` from `project.yml`, to open in Xcode |
| `cd apps/services/api-app && npm start` | Runs the web app's development server on port 4200, proxying `/api` to `localhost:8080` |

## Contract and collection

| Command | What it does |
| --- | --- |
| `./gradlew :apps:bruno:updateCollection` | Regenerates the Bruno requests from the contract; run it after any contract change |
| `cd apps/bruno && npm run smoke` | Replays every request of the collection against the local stack |

## Images and end-to-end tests

| Command | What it does |
| --- | --- |
| `./gradlew :apps:services:api:jibDockerBuild` | Builds the API image `skeleton-apps/api:local` |
| `./gradlew :apps:services:api-app:dockerBuild` | Builds the web app image `skeleton-apps/api-app:local` |
| `./gradlew :apps:services:api-app:npmRunE2e -PrunE2e=true` | Runs the Playwright tests; set `E2E_BASE_URL=http://localhost:8088` to target the local stack |

## Documentation

The tools run in containers pinned in `gradle.properties`: Docker is the only prerequisite besides the JDK, which generates the domain model page. None of these tasks is part of `check`; the `docs` job of CI runs them.

| Command | What it does |
| --- | --- |
| `./gradlew docsServe` | Serves the site on <http://localhost:8000>, rebuilt on every change |
| `./gradlew docsBuild` | Builds the site into `build/docs/site`; a broken link or anchor fails it |
| `./gradlew docsLint` | Lints the Markdown of the README and `docs/` (`.markdownlint-cli2.jsonc`) |
| `./gradlew :apps:services:api:docsDomainModel` | Generates the domain model page from the modules and their jMolecules annotations (`docs/architecture/domain-model.md`, not committed) |
| `./gradlew docsApiReference` | Renders the API reference from the contract (`docs/product/api-reference.html`, not committed) |

<!-- template-only:start -->
## Installer

| Command | What it does |
| --- | --- |
| `npm ci` | Installs the installer's dependencies (root `package.json`) |
| `npm run installer:test` | Its unit tests (Vitest) |
| `npm run installer:typecheck` | Type checks its JavaScript |
| `node scripts/installer/src/cli.js --source . my-idea` | Generates a project from the last local commit |

<!-- template-only:end -->
## GitHub

| Command | What it does |
| --- | --- |
| `gh workflow run publish.yml --ref develop -f all=true` | Republishes every image, whatever changed |
| `gh workflow run release.yml --ref main -f tag=vX.Y.Z` | Gives the images of an existing release its tag again |
| `gh workflow run nightly.yml --ref develop` | Runs mutation testing now |
| `gh workflow run docs.yml --ref main` | Publishes the documentation site now |
| `gh pr merge <number> --merge --admin` | Merges release-please's release pull request when the release App is not configured (see [Release](../../operations/release.md)) |
