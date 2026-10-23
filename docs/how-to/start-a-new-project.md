# Start a new project

This repository is a GitHub template. Until the installer exists (issue #10), starting a new project is manual:

1. On GitHub, **Use this template** and create the new repository, or copy the files without the `.git` folder.
2. **Rename the project**: `rootProject.name` in `settings.gradle.kts`, the compose project (`name:` in `compose.yaml`) and its two image names, `package-name` in `release-please-config.json`, the title of `README.md` and `AGENTS.md`. Image names follow `rootProject.name` automatically.
3. **Rename the base package** `dev.ylp`: the `group` in `build-logic/src/main/kotlin/java-convention.gradle.kts`, the source folders and `package` declarations under `services/api` and `build-logic`, the packages in `contracts/api/build.gradle.kts`, `NullAway:AnnotatedPackages` and the PIT targets.
4. **Rename the web app's OIDC client id** (`api-app`) in `services/api-app/public/config.json` and in `compose.yaml`.
5. **Reset the release history**: delete `CHANGELOG.md` if present and set `.release-please-manifest.json` to `{ ".": "0.0.0" }`.
6. **Keep the `notes` module** as the living example until your first module exists, then delete it with its contract file, changelog, Bruno folder and web feature.
7. **Verify**: `./gradlew check`, then `./gradlew startLocalStack`.
8. **On GitHub**: default branch `develop`, the rulesets and settings described in [CI/CD](../reference/ci-cd.md), and the Renovate app.
