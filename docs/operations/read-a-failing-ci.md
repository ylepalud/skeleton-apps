# Read a failing CI

| Failing job | Where to look | Usual cause |
| --- | --- | --- |
| `check` | The `check-reports` artifact (`build/reports` and test results of every project) | A failing test, the coverage gate (JaCoCo report), Spotless (`./gradlew spotlessApply`), an ErrorProne or NullAway error, an architecture rule, the Bruno drift check (`./gradlew :bruno:updateCollection`) |
| `e2e` | The step "Show the stack logs" and the `playwright-report` artifact (traces of failed tests) | The stack did not start, a console error in the browser, a contract change not reflected in the web app, a fixable `HIGH` or `CRITICAL` vulnerability in an image (steps "Scan the API image" and "Scan the web image": usually a base image or OS package to update) |
| `trivy-fs` | The Security tab, code scanning alerts, category `trivy-fs` | A dependency with a fixable `HIGH` or `CRITICAL` vulnerability, a secret, a misconfiguration |
| An image job in `publish.yml` | The job log, step "Scan the image" | A vulnerable library inside the image: update the dependency that brings it, see [Update dependencies](update-dependencies.md) |
| `pitest` (nightly) | The `pitest-report` artifact | Surviving mutants in `domain` or `application`: add the assertion that kills them |

An image whose scan fails is pushed but never signed: the cluster only deploys signed images, so a failed scan blocks the deployment, not the build.

Reproduce locally before fixing: the same commands run in CI ([Commands](../contributor/reference/commands.md)).
