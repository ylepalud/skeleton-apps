# Commands

Run from the repository root. The Gradle tasks are the source of truth: `./gradlew tasks --all` lists them.

## Build and verify

| Command | What it does |
| --- | --- |
| `./gradlew check` | Everything a pull request needs: compilation with ErrorProne and NullAway, Spotless, unit and integration tests, coverage gate, architecture tests, the web app's lint and tests, the Bruno drift check, and the TestKit tests of the conventions |
| `./gradlew :services:api:check` | The same for the API only |
| `./gradlew :build-logic:functionalTest` | The TestKit tests of the convention plugins only |
| `./gradlew pitest` | Mutation testing (80% threshold), outside `check` |
| `./gradlew spotlessApply` | Formats Java sources; `:build-logic:spotlessApply` formats the Kotlin build code |

## Run

| Command | What it does |
| --- | --- |
| `./gradlew startLocalStack` | Builds both images and starts the whole stack, then waits for the API to be ready |
| `./gradlew stopLocalStack` | Stops the stack, keeps the database |
| `./gradlew resetDatabase` | Stops the stack and deletes the database |
| `./gradlew :services:api:bootRun` | Runs the API from source on port 8080, with Postgres and the identity provider started by Docker Compose |
| `cd services/api-app && npm start` | Runs the web app's development server on port 4200, proxying `/api` to `localhost:8080` |

## Contract and collection

| Command | What it does |
| --- | --- |
| `./gradlew :bruno:updateCollection` | Regenerates the Bruno requests from the contract; run it after any contract change |
| `cd bruno && npm run smoke` | Replays every request of the collection against the local stack |

## Images and end-to-end tests

| Command | What it does |
| --- | --- |
| `./gradlew :services:api:jibDockerBuild` | Builds the API image `skeleton-apps/api:local` |
| `./gradlew :services:api-app:dockerBuild` | Builds the web app image `skeleton-apps/api-app:local` |
| `./gradlew :services:api-app:npmRunE2e -PrunE2e=true` | Runs the Playwright tests; set `E2E_BASE_URL=http://localhost:8088` to target the local stack |

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
| `gh pr merge <number> --merge --admin` | Merges release-please's release pull request when the release App is not configured (see [Release](../../operations/release.md)) |
