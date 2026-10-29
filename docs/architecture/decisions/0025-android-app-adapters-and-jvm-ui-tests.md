# 0025. Android app: Compose UI, Custom Tabs and Keystore adapters, UI tests on the JVM

- Status: accepted
- Date: 2026-10-29

## Context

The Android app implements the UI and the two ports of `clients/shared` (issue #67): a browser for the sign-in and a token store. Its tests must run in `check`, on CI's Linux runners, without an emulator. The repository's tests run on the JUnit platform (JUnit 6), but Robolectric and the Compose test rule only offer a JUnit 4 API.

## Options

For the sign-in browser:

- Custom Tabs, the redirect back to the activity through a private-use scheme
- A WebView inside the app

For the token store:

- AES-GCM with a key of the AndroidKeyStore, the ciphertext in private preferences, kept out of backups
- `EncryptedSharedPreferences` (androidx.security-crypto), now deprecated

For the UI tests:

- Robolectric and Compose UI tests on the JVM, run by JUnit Vintage on the JUnit platform
- Instrumented tests on an emulator
- A community JUnit 5 extension for Robolectric

## Decision

Chosen: Custom Tabs with the redirect `dev.ylp.notes://callback` received by `MainActivity` (`singleTop`), as RFC 8252 asks of native apps; a WebView would let the app read what the user types on the provider's page. The tokens are encrypted with a key of the AndroidKeyStore, behind a `TokenCipher` so the store is tested on the JVM, and excluded from cloud backups and device transfers. The UI tests run on the JVM with Robolectric, against a real `SharedClient` and MockWebServer standing in for the API and the provider; the JUnit platform runs them through Vintage, and every other test uses Jupiter.

## Consequences

- `check` covers the screens, the sign-in flow and the error paths in seconds, on any machine with the Android SDK.
- What only a device has (the AndroidKeyStore, a real browser) is checked by hand on an emulator; [Run the Android app](../../contributor/how-to/run-the-android-app.md) gives the steps.
- JUnit 4 stays as an API for Robolectric only; the tests run on the same platform as the rest of the repository.
- Local runs use `adb reverse`, so the emulator's `localhost` is the machine's and the token issuer matches the one the API checks.

More: [Native clients](../clients.md).
