# Run the iOS app

The app in `apps/clients/ios` talks to the local stack from the iOS simulator. It needs macOS with Xcode, XcodeGen and Docker. It is the only app folder that is not a Gradle project: the root build drives Xcode for it.

## Once: the tools

```bash
xcode-select -p                       # must point to Xcode.app, not to the Command Line Tools
brew install xcodegen
```

Open Xcode once to accept its license and install the iOS simulator it offers.

## Every time

```bash
./gradlew startLocalStack             # API, web app and identity provider
./gradlew iosRun                      # generates the project, builds, starts the simulator and the app
```

Sign in with any username: the mock identity provider accepts everyone. The simulator shares the machine's network, so `localhost` reaches the stack directly and the tokens carry the issuer the API checks.

`iosRun` uses the `iPhone 17` simulator; pick another with `-PiosSimulator="iPhone 16"`, or a device identifier when several runtimes have that name (`xcrun simctl list devices available` lists both).

## In Xcode

```bash
./gradlew iosGenerateProject
open apps/clients/ios/Notes.xcodeproj
```

Change `project.yml`, never the generated project: it is not committed, and the next generation replaces it. A build phase runs `./gradlew :apps:clients:shared:embedSwiftExportForXcode`, so Xcode builds the shared module too; a change in Kotlin shows on the next build.

## Point it at another environment

The same Gradle properties as the Android app set the build settings:

```bash
./gradlew iosRun -PmobileApiBaseUrl=https://example.org/api/v1 -PmobileOidcIssuer=https://id.example.org
```

The provider must accept the redirect URI `dev.ylp.notes://callback` for that client. Plain HTTP is allowed to `localhost` only.

## Tests

`./gradlew iosTest` runs the unit and UI tests on the simulator: a stub server in the test runner stands in for the API and the identity provider, and the sign-in page is answered without showing it. `./gradlew iosVerifyCoverage` then requires 80% of the app's lines, and `./gradlew iosLint` runs swift-format. `./gradlew check` includes all three on a Mac with Xcode; CI runs them in the `ios` job when a change can affect the app, and every night on `develop`.
