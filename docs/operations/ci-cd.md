# CI/CD

All workflows live in `.github/workflows`. Actions are pinned by commit SHA (Renovate keeps them current), and each job gets the minimal permissions it needs.

## Workflows

| Workflow | Trigger | Jobs |
| --- | --- | --- |
| `ci.yml` | pull requests to `develop` or `main`, push to `main`, called by `publish.yml` | `check` (`./gradlew check`, the Android app aside), `android`, `ios` and `mobile` (see [Mobile apps](#mobile-apps)), `e2e` (local stack, Playwright, Bruno smoke run, Trivy scan of the two images the stack runs), `trivy-fs` (dependencies including development ones, secrets and configuration; findings in the Security tab), `docs` (no em dash, `./gradlew docsLint docsBuild`: Markdown lint and a strict build of the site, which fails on a broken link or anchor) |
| `docs.yml` | push to `main`, or manual; only when the variable `DOCS_PAGES` is `true` | builds the documentation site and deploys it to GitHub Pages (see [Documentation site](#documentation-site)) |
| `publish.yml` | push to `develop`, or manual with `all=true` | `ci.yml`, then one image job per image whose code changed: build and push, scan, SBOM, signature |
| `nightly.yml` | every day at 03:00 UTC, or manual | `./gradlew pitest` on `develop`, and the whole iOS run (`ios.yml` with the shared module's simulator tests; see [Mobile apps](#mobile-apps)) |
| `release.yml` | push to `main`, or manual with `tag=vX.Y.Z` to promote an existing release's images again | release-please, then the release tag added to the published images |

<!-- template-only:start -->
The template also runs `installer.yml` on pull requests to `develop` or `main`: its `installer` job runs the installer's tests, generates a project from the pull request and runs `./gradlew check` on it.

<!-- template-only:end -->

## Images

| Image | Built by | Name |
| --- | --- | --- |
| API | Jib | `ghcr.io/ylepalud/skeleton-apps/api:<commit sha>` |
| Web app | `docker buildx` | `ghcr.io/ylepalud/skeleton-apps/api-app:<commit sha>` |

Path filters decide which images a push to `develop` publishes: the API for `apps/services/api`, `apps/contracts`, `build-logic` and the Gradle files; the web app for `apps/services/api-app`, `apps/contracts` and its convention. Tags are immutable commit SHAs; a release adds `vX.Y.Z` to the same image. There is no `latest`.

The `.github/actions/secure-image` action runs for each image:

1. Trivy scans the pushed image and fails on fixable `HIGH` or `CRITICAL` vulnerabilities.
2. Syft produces an SPDX SBOM, uploaded as an artifact.
3. cosign signs the image and attests the SBOM, keyless, with the workflow's GitHub OIDC identity.

An image that fails its scan stays unsigned. To check a signature:

```bash
cosign verify ghcr.io/ylepalud/skeleton-apps/api@<digest> \
  --certificate-identity-regexp '^https://github.com/ylepalud/skeleton-apps/' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com
```

`cosign verify-attestation --type spdxjson` with the same options checks the SBOM.

## Mobile apps

`ci.yml` checks the native apps through one workflow per platform, `android.yml` and `ios.yml`, which a `changes` job starts only when a change can affect them; a skipped job costs nothing. The `mobile` job then reports for both, and is the required check: it passes when each app job passed or was skipped.

| Job | Runner | Starts when a change touches | Runs | Keeps |
| --- | --- | --- | --- | --- |
| `android` (`android.yml`) | `ubuntu-26.04` (Android SDK preinstalled), about 4 min | `apps/clients/android`, `apps/clients/shared`, the contract, `ci.yml`, `android.yml`, or the build (`build-logic`, `gradle/`, the root Gradle files) | `:apps:clients:android:check` (lint, Robolectric UI tests, Kover) and `assembleDebug` | `android-debug-apk`: the debug APK, 7 days; `android-reports` on failure |
| `ios` (`ios.yml`) | `macos-26` (Xcode, simulators), about 10 to 15 min with warm caches | `apps/clients/ios`, `apps/clients/shared`, the contract, `ci.yml`, `ios.yml`, or the conventions it builds with (`xcode-app`, `kotlin-multiplatform-convention`, `openapi-kotlin-client`) | `iosLint`, `iosTest` and `iosVerifyCoverage` on the `iPhone 17` simulator of the newest installed runtime, picked by UDID and booted while Gradle starts | `ios-simulator-app`: the simulator build, 7 days; `ios-test-results` (the `.xcresult`) on failure |

The `ios` job is the slow one, so it follows fewer paths: a dependency upgrade or another convention does not start it. `nightly.yml` runs it every night on `develop` with the shared module's tests on the simulator too, and catches within a day what a pull request let through, such as a Kotlin upgrade that changes Swift export. Those nightly runs also fill the caches pull requests start from: the Gradle cache that `setup-gradle` writes from `develop` only, and `~/.konan`, the Kotlin/Native toolchain, kept by Kotlin version even when a run fails.

Neither job signs anything: the APK is a debug build and the iOS build runs on the simulator only. Store releases are #74.

## Branches and rulesets

| Branch | Role | Merge method | Rules |
| --- | --- | --- | --- |
| `develop` | default branch, the only one deployed | squash | pull request required, `check`, `mobile`, `e2e`<!-- template-only:start -->, `installer`<!-- template-only:end --> and `trivy-fs` required, linear history, no force push or deletion |
| `main` | releases | merge commit | pull request required, the same checks, no force push or deletion; the branch need not be up to date, since release-please's commits only exist on `main` |

Repository administrators can bypass the rulesets. Without the release App, this is how release-please's pull requests get merged (see [Release](release.md)).

## Repository settings

Public template repository. Secret scanning with push protection, Dependabot alerts (updates come from Renovate), read-only `GITHUB_TOKEN` by default, workflows allowed to open pull requests (for release-please).

## Documentation site

`docs/` is published as a site built by Zensical (`mkdocs.yml`), with an API reference rendered from the contract by Redocly and a domain model page generated from the code by `ModularityTest`. Pull requests only build it, in the `docs` job of `ci.yml`; `docs.yml` publishes it from `main`, so the site describes the latest release. Locally: `./gradlew docsServe`, then <http://localhost:8000>.

Publishing is opt-in, because GitHub Pages must be enabled per repository and a private repository on a free plan has none. To turn it on:

```bash
gh api -X POST repos/<owner>/<name>/pages -f build_type=workflow
gh api -X PUT repos/<owner>/<name>/environments/github-pages \
  --input - <<< '{"deployment_branch_policy": {"protected_branches": false, "custom_branch_policies": true}}'
gh api -X POST repos/<owner>/<name>/environments/github-pages/deployment-branch-policies -f name=main -f type=branch
gh variable set DOCS_PAGES --body true
```

The first command enables Pages with GitHub Actions as its source; the next two let `main` deploy to the `github-pages` environment; the last one turns the workflow on. Enabling Pages may also have allowed the default branch: `gh api repos/<owner>/<name>/environments/github-pages/deployment-branch-policies` lists the rules, and `gh api -X DELETE` on a rule's URL removes it, so that only `main` deploys. The site is then served at `https://<owner>.github.io/<name>/`.

## Release App

A pull request opened with the workflow's own token starts no workflow, so release-please's pull requests would get no check. `release.yml` therefore opens them with a GitHub App token when the App is configured, and falls back to the workflow's token otherwise.

- One App serves every repository of its owner: create it once (no webhook, only on this account) with the repository permissions **Contents** and **Pull requests** in read and write, then install it on each repository.
- Repository variable `RELEASE_APP_CLIENT_ID`: the App's client id.
- Repository secret `RELEASE_APP_PRIVATE_KEY`: a private key of the App. Store it straight from the downloaded file, then delete the file:

  ```bash
  gh variable set RELEASE_APP_CLIENT_ID --body <client id>
  gh secret set RELEASE_APP_PRIVATE_KEY < <app>.private-key.pem
  ```

The token lasts one hour and only carries those two permissions.
