# Acceptance

The acceptance test book of each version: what to run, by hand, before calling a version accepted. One folder per version, the latest being the one to use:

| Version | Source |
| --- | --- |
| 0.2.0 | [0.2.0/content.py](0.2.0/content.py) |

Each folder holds the book's source, `content.py` (the tests as data: preconditions, steps with the exact commands, expected results) and `build.py`, which renders it as a PDF.
<!-- template-only:start -->
In this repository the folder also holds the rendered book, [skeleton-apps-acceptance-test-book.pdf](0.2.0/skeleton-apps-acceptance-test-book.pdf). A project generated from the template keeps only the clients it chose, so it renders its own.
<!-- template-only:end -->

## Render a book

`build.py` needs Python 3 and reportlab:

```bash
cd docs/acceptance/0.2.0
python3 -m venv .venv && .venv/bin/pip install reportlab
.venv/bin/python build.py skeleton-apps-acceptance-test-book.pdf "0.2.0 (develop @ $(git rev-parse --short HEAD))"
```

## Run a campaign

The tests run against the local stack only, in order: the stack, the API with Bruno and curl, then each client the project has. Open the PDF in Preview, Acrobat or any reader with forms; tick Pass, Fail or Blocked for each test, write what differs in Notes, fill the summary and the verdict, and save the file. Keep the filled copy with the release it accepts.

## A new version

Copy the latest folder under the new version's number, change `content.py` for what the version changes (a new screen, command or error is a new test), render the PDF, and add the version to the table above.
