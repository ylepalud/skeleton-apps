# 0032. An agent CLI that reads every answer at run time, checked by the build

- Status: accepted
- Date: 2026-11-02

## Context

The Claude Code transcripts of this repository show the same lookups in session after session: the state of the branch, a pull request's checks, the log of a failed run, an epic's sub-issues, the list of modules, which page to read, the next ADR number, a `./gradlew check` piped through `tail`. Each is rebuilt by hand, with a different `gh` or `grep` invocation every time, and often costs several calls and a few thousand tokens of output. A single tool can answer them, but a tool that holds facts goes stale and then answers wrong, which is worse for an agent than no tool.

## Options

- A Node script with no dependency, `bin/agent-cli`, one command per repeated lookup, every answer read from its source at run time
- Shell aliases or a Makefile
- More pages in `docs/` listing the commands to type
- An MCP server

## Decision

Chosen: the Node script. Node is already required (the web app's toolchain), its standard library covers the parsing and the tests (`node:test`), and one registry describes each command once: its help, its reference page, the files it reads (`sources`) and the raw commands it replaces (`match`). Pages would be one more cache to keep current; an MCP server needs a running process and a client configuration in every generated project; shell aliases cannot produce JSON or fail cleanly.

Four guards keep it current:

- **Read at run time.** No command stores a fact; each parses the file or calls the tool that owns it.
- **Stale fails loudly.** A source that no longer parses gives exit 3 and names it, never an empty answer.
- **Checked with the build.** `./gradlew check` runs `agentCliTest`: unit tests, then `selftest`, which runs every offline command against the repository, checks every declared source exists, and checks the generated reference page matches the registry.
- **Usage feeds back.** A `PostToolUse` hook (`.claude/settings.json`) tells an agent when a raw command it ran has a command; `agent-cli stats` reads the transcripts and lists raw calls still made, calls through the CLI, and repeated lookups with no command yet.

## Consequences

- The CLI ships with the template: it holds no project name, and reads the repository and its GitHub remote from where it runs.
- The hook only adds context after a command, it never blocks one: a raw command with options the CLI does not cover keeps working.
- `agentCliTest` is never up to date, since the selftest reads files no task input declares; it takes a few seconds.
- The GitHub commands are tested through their parsers only: `check` runs without network access to GitHub.
- `stats` is the review ritual: a command nobody calls is deleted, a lookup that recurs gets a command.

More: [Agent CLI](../../contributor/reference/agent-cli.md).
