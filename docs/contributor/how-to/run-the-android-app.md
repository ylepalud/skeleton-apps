# Run the Android app

The app in `clients/android` talks to the local stack from an emulator. It needs the Android SDK (`ANDROID_HOME`, see [Troubleshoot](../../operations/troubleshoot.md#sdk-location-not-found)) and Docker.

## Once: an emulator

With the command-line tools, install the emulator and a system image, then create a device:

```bash
sdkmanager --sdk_root="$ANDROID_HOME" "emulator" "system-images;android-36;google_apis_playstore;arm64-v8a"
avdmanager create avd -n notes -k "system-images;android-36;google_apis_playstore;arm64-v8a" -d pixel_7
```

On an Intel machine, take the `x86_64` image. Android Studio's device manager does the same.

## Every time

```bash
emulator -avd notes &                 # add -no-window to run it headless
./gradlew startLocalStack             # API, web app and identity provider
adb reverse tcp:8088 tcp:8088         # the API, through the web app's proxy
adb reverse tcp:8090 tcp:8090         # the identity provider
./gradlew :clients:android:installDebug
```

Open **Notes** and sign in with any username: the mock identity provider accepts everyone. The first sign-in may show Chrome's welcome screen; choose to use it without an account.

`adb reverse` makes the emulator's `localhost` the machine's. The tokens then carry the issuer `http://localhost:8090/default`, the one the API checks; `10.0.2.2`, the emulator's usual alias for the machine, would give another issuer and every call would answer 401.

## Point it at another environment

The addresses are build settings, read from Gradle properties:

| Property | Default (local stack) |
| --- | --- |
| `mobileApiBaseUrl` | `http://localhost:8088/api/v1` |
| `mobileOidcIssuer` | `http://localhost:8090/default` |
| `mobileOidcClientId` | `api-mobile` |

```bash
./gradlew :clients:android:installDebug -PmobileApiBaseUrl=https://example.org/api/v1 -PmobileOidcIssuer=https://id.example.org
```

The provider must accept the redirect URI `dev.ylp.notes://callback` for that client. Only the debug build may use plain HTTP, and only to `localhost`.

## Tests

`./gradlew :clients:android:check` runs lint, the tests and the coverage gate on the JVM: Robolectric renders the real screens, and a mock server stands in for the API and the identity provider. No emulator is needed.
