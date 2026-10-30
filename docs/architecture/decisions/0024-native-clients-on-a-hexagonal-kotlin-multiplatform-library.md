# 0024. Native clients on a hexagonal Kotlin Multiplatform library, sign-in written in Kotlin

- Status: accepted
- Date: 2026-10-29

## Context

The template gets an Android app, an iOS app and a command-line client (issue #65). Each needs the same API calls, the same sign-in and refresh, and later the same offline queue and conflict handling. Gradle must drive every build, iOS included. A backend-for-frontend was set aside: the REST API stays the single entry point (issue #20).

## Options

- A Kotlin Multiplatform library holding the logic, with a native UI per platform (Compose, SwiftUI)
- Two native code bases (Kotlin, Swift), each with its own client generated from the contract
- Compose Multiplatform, one UI for both platforms

For the sign-in, inside the library:

- Authorization Code with PKCE written in Kotlin, the platform opening the browser only
- AppAuth, a separate library on each platform

## Decision

Chosen: a Kotlin Multiplatform library, `apps/clients/shared`, organised as a hexagon. It defines the ports (`AuthorizationBrowser`, `TokenStore`) that each app implements, and exposes use cases; the apps implement their UI only. The sign-in is written in Kotlin in the library. The client is generated from the contract by openapi-generator's multiplatform library and kept internal. The public API stays within Kotlin's Swift export (Alpha): no generic type or function, one sealed result per operation. SKIE is the fallback if Swift export blocks.

## Consequences

- The API calls, the sign-in and the error mapping are written and tested once, for three clients.
- The security-sensitive code (PKCE, state check, refresh) is the template's own, so it carries its own tests rather than a library's track record.
- Every build needs the Android SDK, and iOS needs macOS with Xcode.
- Swift export can break with a Kotlin release; Renovate proposes each release, and the iOS build shows what broke.
- `ArchitectureTest` (Konsist) checks the hexagon and the Swift export limits, as `HexagonalArchitectureTest` does for the API.

More: [Native clients](../clients.md).
