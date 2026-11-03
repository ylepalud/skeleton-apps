# Start a new project

A new project is generated from a released version of this template by its installer. You need Node 22 or later and Git; Java 17+ and Docker Compose v2 are only needed to verify the build before archiving.

```bash
npx github:ylepalud/skeleton-apps my-idea
```

The installer generates from the latest release. To pin a version, add it to the command: `npx github:ylepalud/skeleton-apps#vX.Y.Z my-idea`. The installer asks for the project name, the GitHub owner, the base Java package, the clients to keep, the web app's OIDC client id when the web app is kept, and where to write the archive, shows a summary, and writes `<name>.zip` in the current folder. Inside: the generated project, already a Git repository on `develop` with a single `Init` commit.

To change a version (Java, Spring, Angular, a tool), change it here first, release, then generate from the new tag. The installer never asks for versions.

## Options

`npx github:ylepalud/skeleton-apps#<tag> --help` lists them. With `--yes`, nothing is asked: the given options and the defaults apply.

| Option | Default |
| --- | --- |
| `--name` (or the first argument) | none, required |
| `--owner` | `git config github.user`, else `user.name` |
| `--package` | `dev.<owner>.<name without dashes>` |
| `--clients` | `web,cli,mobile`: the clients to keep, at least one |
| `--client-id` | `<name>-web`, used only with the web app |
| `--dest` | `.` |
| `--ref` | the latest `vX.Y.Z` tag, else `develop` |
| `--source` | this repository; a local path generates from its last commit |
| `--check` | off: runs `./gradlew check` on the generated project before archiving |

## What changes

- `ylepalud/skeleton-apps` becomes `<owner>/<name>` (badges, image names, signature identity), then `skeleton-apps` becomes `<name>` (root project, compose project, local images, release-please, docs).
- The base package `dev.ylp` becomes yours, in sources and build scripts, and the source folders move with it.
- Only the chosen clients are kept: see [Choose the clients](#choose-the-clients).
- The web app's OIDC client id changes in `apps/services/api-app/public/config.json` and `compose.yaml`. The web module keeps its name, `api-app`.
- The release history restarts: no `CHANGELOG.md`, manifest at `0.0.0`.
- Template-only material stays out: the installer, its workflow, references to this repository's issues, and this section of the docs, replaced by a page naming the template version the project came from.
- The `notes` module stays as the living example. Delete it, with its contract file, changelog, Bruno folder and web feature, once your first module exists.

## Choose the clients

The API is always there. Each client is optional, and at least one is kept:

| Client | Kept when chosen | Left out otherwise |
| --- | --- | --- |
| `web` | the Angular app (`apps/services/api-app`), its convention, its image, its end-to-end tests and pages | the web service of `compose.yaml`: the API itself answers on `http://localhost:8088`, where the clients expect it |
| `cli` | the command-line client (`apps/clients/cli`), its convention, `cli.yml`, its pages and ADR | the shared module's macOS and Linux targets |
| `mobile` | the Android and iOS apps, their conventions, `android.yml`, `ios.yml`, the nightly iOS run, their pages and ADRs | the shared module's Android and iOS targets, and with them the Android SDK and Xcode prerequisites |

The shared module (`apps/clients/shared`) stays with the CLI or mobile; without either, the native clients' jobs, conventions and pages go too. The installer's own tests check every combination; see [Mark a client's passage](mark-a-clients-passage.md) to keep that true when the template changes.

## After unzipping

1. Create the empty repository `<owner>/<name>` on GitHub and push `develop`.
2. Apply the branches, rulesets and repository settings described in [CI/CD](../operations/ci-cd.md), and install the Renovate app.
3. Install the [release App](../operations/ci-cd.md#release-app) on the new repository, then set its variable and secret there, so that release pull requests run CI.
4. Optionally, publish the documentation site: [Documentation site](../operations/ci-cd.md#documentation-site).
5. `./gradlew check`, then `./gradlew startLocalStack`.

## Work on the installer

It lives in `scripts/installer` (plain JavaScript checked by TypeScript) with the root `package.json`, and is never published to npm. `npm run installer:test` and `npm run installer:typecheck` run its checks, every combination of clients included, in memory. The `installer` workflow also generates three projects from each pull request and builds them: all the clients, the web app alone and the native clients alone; every week, and on demand, it builds all seven combinations.
