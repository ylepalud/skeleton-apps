# 0030. Native CLI: Kotlin/Native and Clikt, the device flow with a loopback fallback, the system's secret store

- Status: accepted
- Date: 2026-11-03

## Context

Issue #72 asks for a command-line client of the notes on macOS and Linux, built on `apps/clients/shared` like the apps ([ADR 0024](0024-native-clients-on-a-hexagonal-kotlin-multiplatform-library.md)), with no JVM to install, signing in through the OIDC device flow and keeping its tokens securely. The local identity provider, mock-oauth2-server, has no device flow.

## Options

For signing in:

- The device flow (RFC 8628) when the provider offers it, Authorization Code with PKCE on a loopback redirect (RFC 8252) otherwise
- Replacing the local provider with one that has the device flow, such as Keycloak
- The loopback redirect only

For the tokens on Linux:

- `secret-tool` (libsecret) when installed, a file readable by the owner only otherwise
- libsecret linked into the binary

For the tokens on macOS:

- The `security` tool, with the secret on its standard input
- The Security framework, through Kotlin/Native's interop

## Decision

Chosen: the device flow with a loopback fallback, `secret-tool` or a private file on Linux, `security` on macOS, and Kotlin/Native binaries for `macosArm64` and `linuxX64` with Clikt.

- **Device flow first**: `SignInOnDevice` in `shared` reads the provider's discovery document; when it lists `device_authorization_endpoint` (Zitadel, Keycloak), the user types a code on any device. Otherwise `notes login` falls back to the existing `SignIn`, with a browser on the same computer and a one-shot server on `127.0.0.1`. The local stack keeps its light provider; replacing it would touch every client and the end-to-end tests for a local convenience.
- **The system's tools, not linked libraries**: a binary linked to libsecret would not start on a server without it, even where a private file is enough. Both `security` and `secret-tool` read the secret on their standard input, never in their arguments, which other users can read with `ps`; one class drives both, and tests replace the tool with a script.
- **A JVM target that ships nowhere**: the commands live in `commonMain` and run their tests on the JVM too, where Kover measures them at the same 80% as `shared`.

## Consequences

- `shared` gains the `macosArm64` and `linuxX64` targets, Ktor's Darwin and Curl engines, and `SignInOnDevice`; its common tests run on macOS and, in CI, on Linux.
- Binaries that use the local store link against the system's SQLite (`-lsqlite3`): the Linux ones link on a Linux host only, with `libsqlite3-dev`.
- The CLI's client of the identity provider must allow the device grant, or the loopback redirect `http://127.0.0.1:8770/callback` (`NOTES_OIDC_REDIRECT_PORT` changes the port).
- `cli.yml` builds and tests both binaries when a change can affect them; the required `mobile` check reports for the CLI too.

More: [Native clients](../clients.md#the-command-line), [Use the CLI](../../product/use-the-cli.md).
