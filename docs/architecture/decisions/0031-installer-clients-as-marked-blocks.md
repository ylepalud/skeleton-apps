# 0031. The installer keeps the chosen clients: whole folders, and marked passages of the template

- Status: accepted
- Date: 2026-11-03

## Context

Issue #73 asks the installer which clients a new project gets: web, CLI, mobile (Android and iOS). The API is always there, the shared module stays with the CLI or mobile, and CI, docs and the README describe only what the project keeps. A client is not one folder: it also has passages in files every project keeps, such as `settings.gradle.kts`, `compose.yaml`, `ci.yml`, the README and some forty documentation pages. The issue's bar is that every combination of clients builds and passes CI.

## Options

For removing a client:

- Whole folders and files listed in the installer, plus marked blocks and lines in the shared files, stripped by the installer
- The installer edits each shared file it knows

For proving every combination:

- All seven combinations built for real on every pull request
- Every combination checked in memory on every pull request, three built for real, all seven on a schedule

## Decision

Chosen: listed folders and marked passages, and the in-memory check with three real builds per pull request and seven on a schedule.

- **Markers next to what they mark**: `client:<name>:start` and `:end` around a block, in each file's own comment syntax, or `client:<name>` at the end of a single line, for a table row, a list item or a sentence where a block would break the Markdown. The rule lives beside the text it governs, so a change of a shared file carries its own marking; an installer that edited each file would break silently at every change of those files. The names are the three clients, `native` for the shared module, and `!web` for what only a project without the web app needs, such as the API answering on port 8088 itself.
- **Every combination in memory**: the installer's unit tests generate the seven projects from the template itself and check what a missed marker would break: paired markers, links, the site navigation, Gradle includes, called workflows, and no path of a client left out. They run in seconds.
- **Three real builds per pull request** cover each removal once: all the clients, the web app alone (no native code at all) and the native clients alone (no web app). All seven, too slow for every pull request, run weekly and on demand. A combination broken only by the interaction of two removals can wait up to a week; the in-memory checks make that unlikely.

## Consequences

- A contributor marks every new client-specific passage of a shared file, or the tests fail ([Mark a client's passage](../../template/mark-a-clients-passage.md)).
- Without the web app, the API container publishes port 8088: the native clients' URL does not change, and the end-to-end job keeps the Bruno smoke run and the API image scan.
- The required `installer` check is an aggregating job, as `mobile` is for the clients: the matrix jobs report under names that follow the combinations.
- Design tokens (#79) will mark their per-platform outputs the same way.
