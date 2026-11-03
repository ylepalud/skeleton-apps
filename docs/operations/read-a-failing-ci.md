# Read a failing CI

| Failing job | Where to look | Usual cause |
| --- | --- | --- |
| `check` | The `check-reports` artifact (`build/reports` and test results of every project) | A failing test, the coverage gate (JaCoCo report), Spotless (`./gradlew spotlessApply`), an ErrorProne or NullAway error, an architecture rule, the Bruno drift check (`./gradlew :apps:bruno:updateCollection`) |
| `android` | The `android-reports` artifact (lint, tests, Kover) | A failing Robolectric test, a lint warning (warnings fail), coverage under 80% <!-- client:mobile --> |
| `ios` | The `ios-test-results` artifact: open `Test.xcresult` in Xcode for each failure's screenshots and log | A failing UI test, swift-format (`./gradlew iosLint` names the rule), coverage under 80%, a Swift export change after a Kotlin upgrade (see [ADR 0027](../architecture/decisions/0027-ios-app-swift-export-and-xcodegen.md)) <!-- client:mobile --> |
| `e2e` | The step "Show the stack logs" | The stack did not start, a failing Bruno smoke request, a fixable `HIGH` or `CRITICAL` vulnerability in an image (the "Scan" steps: usually a base image or OS package to update) |
| `e2e`, web app | The `playwright-report` artifact (traces of failed tests) | A console error in the browser, a contract change not reflected in the web app <!-- client:web --> |
| `trivy-fs` | The Security tab, code scanning alerts, category `trivy-fs` | A dependency with a fixable `HIGH` or `CRITICAL` vulnerability, a secret, a misconfiguration |
| An image job in `publish.yml` | The job log, step "Scan the image" | A vulnerable library inside the image: update the dependency that brings it, see [Update dependencies](update-dependencies.md) |
| `cli` | The `cli-reports-<target>` artifact of the failing platform | A failing command test, coverage under 80%, a link error on Linux (see [Troubleshoot](troubleshoot.md)) <!-- client:cli --> |
| `mobile` | The client job it names in its log | It only reports their results: fix the failing one <!-- client:native --> |
| `ios` (nightly) | The same as `ios`, plus the shared module's test report in `ios-test-results` | Often a dependency upgrade merged during the day: a Kotlin or Ktor version that changes Swift export or breaks a test on iOS only <!-- client:mobile --> |
| `pitest` (nightly) | The `pitest-report` artifact | Surviving mutants in `domain` or `application`: add the assertion that kills them |

An image whose scan fails is pushed but never signed: the cluster only deploys signed images, so a failed scan blocks the deployment, not the build.

Reproduce locally before fixing: the same commands run in CI ([Commands](../contributor/reference/commands.md)).
