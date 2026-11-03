# Run the CLI

The command-line client in `apps/clients/cli` talks to the local stack from the terminal. It is a Kotlin/Native binary, `notes`, for macOS on Apple Silicon and Linux on x86_64: no JVM is needed to run it, only to build it.

## Build it

```bash
./gradlew :apps:clients:cli:linkReleaseExecutableMacosArm64   # on a Mac
./gradlew :apps:clients:cli:linkReleaseExecutableLinuxX64     # on Linux
```

The binary is `apps/clients/cli/build/bin/<target>/releaseExecutable/notes.kexe`; copy it as `notes` somewhere on your `PATH`. A Linux binary links against the system's SQLite, so it builds on a Linux host only; CI keeps both binaries of each pull request as artifacts.

## Every time

```bash
./gradlew startLocalStack             # API, web app and identity provider
notes login                           # opens the sign-in page in your browser
notes add --title Groceries --content "Milk, eggs"
notes list
```

The local provider has no device flow, so `login` opens its page in your browser and waits for its redirect on `http://127.0.0.1:8770/callback`; sign in with any username. Against a provider with the device flow, `login` prints a code to type on any device instead. [Use the CLI](../../product/use-the-cli.md) lists the commands.

## Point it at another environment

| Variable | Default |
| --- | --- |
| `NOTES_API_BASE_URL` | `http://localhost:8088/api/v1` |
| `NOTES_OIDC_ISSUER` | `http://localhost:8090/default` |
| `NOTES_OIDC_CLIENT_ID` | `api-cli` |
| `NOTES_OIDC_REDIRECT_PORT` | `8770`, the loopback port of the browser sign-in |

The provider's client must allow the device grant, or the redirect `http://127.0.0.1:<port>/callback`.

## Where it keeps things

| What | macOS | Linux |
| --- | --- | --- |
| Tokens | the login Keychain, item `dev.ylp.notes` | libsecret through `secret-tool` in a desktop session; otherwise `$XDG_CONFIG_HOME/notes/tokens` (`~/.config/notes/tokens`), mode `0600` |
| Notes and changes not synced | `$XDG_DATA_HOME/notes/client.db` (`~/.local/share/notes`) | the same |

`notes logout` forgets both. To start from scratch by hand, delete `~/.local/share/notes` and the tokens.

## Tests

`./gradlew :apps:clients:cli:check` runs the commands against a fake API and identity provider on a local server, on the JVM (with the Kover gate at 80%) and on macOS, plus the token stores with stand-in tools. On Linux, the same command also runs them as a Linux binary.
