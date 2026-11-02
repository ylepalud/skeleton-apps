// Unit tests of bin/agent-cli.mjs, then its selftest against this repository.
// Run by `./gradlew agentCliTest`, part of `check`; alone: node --test bin/agent-cli.test.mjs
import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  REGISTRY,
  Stale,
  bashCalls,
  checkTasks,
  commandSegments,
  coveringCommand,
  coveringCommands,
  filterLog,
  parseAdr,
  parseAgentsTable,
  parseArgs,
  parseDuration,
  parsePlugins,
  parseSettings,
  parseWorkflow,
  renderDocs,
  summarizeChecks,
  summarizeGradle,
} from './agent-cli.mjs';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

test('parseSettings reads the root name and every included project', () => {
  const settings = parseSettings('rootProject.name = "demo"\n\ninclude("apps:services:api")\ninclude(":apps:bruno")\n');
  assert.deepEqual(settings, { name: 'demo', projects: [':apps:services:api', ':apps:bruno'] });
});

test('parseSettings is stale without a project', () => {
  assert.throws(() => parseSettings('rootProject.name = "demo"\n'), Stale);
});

test('parsePlugins reads ids, backticked and bare plugins', () => {
  const script = 'plugins {\n    id("spring")\n    `java-library`\n    base\n}\n\ndependencies {}\n';
  assert.deepEqual(parsePlugins(script), ['spring', 'java-library', 'base']);
});

test('parseAgentsTable reads the rows of the Before you table only', () => {
  const agents = [
    '# AGENTS.md',
    '| not | [this](x.md) |',
    '## Before you…',
    '',
    '| …do this | read |',
    '| --- | --- |',
    '| build or test | [docs/commands.md](docs/commands.md) |',
    '| touch `apps/` | [docs/clients.md](docs/clients.md) |',
  ].join('\n');
  assert.deepEqual(parseAgentsTable(agents), [
    { when: 'build or test', path: 'docs/commands.md' },
    { when: 'touch `apps/`', path: 'docs/clients.md' },
  ]);
});

test('parseAgentsTable is stale when the section is gone', () => {
  assert.throws(() => parseAgentsTable('# AGENTS.md\n\n## Rules\n'), Stale);
});

test('parseAdr reads number, title and status', () => {
  assert.deepEqual(parseAdr('d/0003-x.md', '# 0003. Hexagonal modules\n\n- Status: accepted\n'), {
    number: '0003',
    title: 'Hexagonal modules',
    status: 'accepted',
    file: 'd/0003-x.md',
  });
  assert.throws(() => parseAdr('d/0004-x.md', '# Hexagonal modules\n'), Stale);
});

test('parseWorkflow reads name, triggers and jobs, block or inline', () => {
  const block = 'name: CI\n\non:\n  pull_request:\n    branches: [develop]\n  push:\n\njobs:\n  check:\n    runs-on: x\n  e2e:\n    needs: check\n';
  assert.deepEqual(parseWorkflow('ci.yml', block), { file: 'ci.yml', name: 'CI', on: ['pull_request', 'push'], jobs: ['check', 'e2e'] });
  assert.deepEqual(parseWorkflow('w.yml', 'name: "W"\non: [push, workflow_dispatch]\njobs:\n  a:\n').on, ['push', 'workflow_dispatch']);
  assert.throws(() => parseWorkflow('bad.yml', 'name: x\n'), Stale);
});

test('summarizeChecks counts states and keeps the run of a failure', () => {
  const summary = summarizeChecks([
    { name: 'check', status: 'COMPLETED', conclusion: 'SUCCESS' },
    { name: 'e2e', status: 'COMPLETED', conclusion: 'FAILURE', workflowName: 'CI', detailsUrl: 'https://github.com/o/r/actions/runs/123/job/9' },
    { name: 'ios', status: 'IN_PROGRESS', conclusion: '' },
    { name: 'android', status: 'COMPLETED', conclusion: 'SKIPPED' },
    { context: 'cla', state: 'PENDING' },
  ]);
  assert.deepEqual(
    { total: summary.total, pass: summary.pass, fail: summary.fail, pending: summary.pending, skip: summary.skip },
    { total: 5, pass: 1, fail: 1, pending: 2, skip: 1 },
  );
  assert.deepEqual(summary.notPassing[0], { name: 'e2e', workflow: 'CI', state: 'fail', runId: '123' });
});

test('filterLog keeps the error lines once, without the gh prefix or noise', () => {
  const log = [
    'check\tRun\t2026-11-01T10:00:00.1234567Z > Task :api:test FAILED',
    'check\tRun\t2026-11-01T10:00:00.2Z NotesTest > rejects() FAILED',
    'check\tRun\t2026-11-01T10:00:00.3Z NotesTest > rejects() FAILED',
    'check\tRun\t2026-11-01T10:00:00.4Z some progress line',
    'check\tRun\t2026-11-01T10:00:00.5Z Run with --stacktrace option to get the stack trace.',
    'check\tRun\t2026-11-01T10:00:00.6Z ##[error]Process completed with exit code 1.',
  ].join('\n');
  assert.deepEqual(filterLog(log), ['> Task :api:test FAILED', 'NotesTest > rejects() FAILED', '##[error]Process completed with exit code 1.']);
});

test('summarizeGradle reads the result, failed tasks and errors', () => {
  const log = [
    '> Task :apps:services:api:compileJava',
    'e: file.kt:3:1 Unresolved reference',
    '> Task :apps:services:api:test FAILED',
    'FAILURE: Build failed with an exception.',
    '* What went wrong:',
    "Execution failed for task ':apps:services:api:test'.",
    '> There were failing tests.',
    'BUILD FAILED in 1m 2s',
  ].join('\n');
  const summary = summarizeGradle(log);
  assert.equal(summary.result, 'fail');
  assert.equal(summary.duration, '1m');
  assert.deepEqual(summary.failedTasks, [':apps:services:api:test']);
  assert.ok(summary.errors.includes('e: file.kt:3:1 Unresolved reference'));
  assert.ok(summary.errors.includes('> There were failing tests.'));
  assert.equal(summarizeGradle('BUILD SUCCESSFUL in 4s').result, 'pass');
  const docs = summarizeGradle('\x1b[33mWarning: page does not exist\x1b[0m\n    ╭─[ overview.md:50:46 ]\n1 issue found\nBUILD FAILED in 9s');
  assert.deepEqual(docs.errors, ['Warning: page does not exist', 'at overview.md:50:46', '1 issue found']);
});

test('checkTasks maps docs, module short names and raw tasks', () => {
  const modules = [
    { path: ':apps:services:api', dir: 'apps/services/api', check: ':apps:services:api:check' },
    { path: ':apps:contracts:api', dir: 'apps/contracts/api', check: ':apps:contracts:api:check' },
    { path: ':apps:clients:shared', dir: 'apps/clients/shared', check: ':apps:clients:shared:check' },
  ];
  assert.deepEqual(checkTasks([], modules), ['check']);
  assert.deepEqual(checkTasks(['docs'], modules), ['docsLint', 'docsBuild']);
  assert.deepEqual(checkTasks(['shared'], modules), [':apps:clients:shared:check']);
  assert.deepEqual(checkTasks(['services/api'], modules), [':apps:services:api:check']);
  assert.deepEqual(checkTasks(['spotlessApply', ':x:test'], modules), ['spotlessApply', ':x:test']);
  assert.throws(() => checkTasks(['api'], modules), /ambiguous/);
});

test('parseArgs reads positionals, flags, values and the rest after --', () => {
  assert.deepEqual(parseArgs(['70', '--no-body', '--limit', '5', '--dir=a', '--dir', 'b', '--', '--info'], ['no-body']), {
    _: ['70'],
    'no-body': true,
    limit: '5',
    dir: ['a', 'b'],
    '--': ['--info'],
  });
});

test('parseDuration reads units', () => {
  assert.equal(parseDuration('20s'), 20_000);
  assert.equal(parseDuration('30m'), 1_800_000);
  assert.equal(parseDuration('15'), 15_000);
  assert.throws(() => parseDuration('soon'));
});

test('commandSegments strips cd, rtk and splits compound commands', () => {
  assert.deepEqual(commandSegments('cd /x && rtk git status --short; gh pr checks 92 || true'), ['git status --short', 'gh pr checks 92', 'true']);
});

test('coveringCommand maps the raw lookups seen in transcripts to their command', () => {
  const cases = {
    'git status --short': 'status',
    'git log --oneline develop..HEAD | head -20': 'status',
    'gh pr checks 92 2>&1 | tail -8': 'pr',
    "gh pr list --state open --json number,title": 'prs',
    'gh run view 123 --log-failed 2>&1 | grep -i error': 'ci',
    'until [ "$(gh pr view 92 --json state --jq .state)" = MERGED ]; do sleep 20; done': 'wait',
    'gh issue view 65 --json body --jq .body': 'issue',
    'gh issue list --state open --limit 50': 'issues',
    'gh api repos/{owner}/{repo}/issues/65/sub_issues': 'epic',
    'gh label list --json name': 'labels',
    'gh api repos/o/r/rulesets --jq .': 'repo',
    'cat settings.gradle.kts': 'modules',
    'find . -name "build.gradle.kts" -not -path "*/build/*"': 'modules',
    "grep -n '' README.md AGENTS.md CLAUDE.md mkdocs.yml": 'doc',
    'ls docs/architecture/decisions | tail -2': 'adr',
    'ls .github/workflows': 'workflows',
    "grep -rn --include='*.md' $'\u2014' README.md docs": 'lint',
    './gradlew check > /tmp/check.log 2>&1': 'check',
    './gradlew -q docsLint docsBuild 2>&1 | tail -20': 'check',
  };
  for (const [raw, expected] of Object.entries(cases)) assert.equal(coveringCommand(raw)?.command, expected, raw);
});

test('coveringCommands lists every lookup of a compound command, in order', () => {
  const raw = 'sleep 20; until [ "$(rtk proxy gh pr checks 29 --json state)" ]; do sleep 20; done; gh pr checks 29; rtk proxy gh label list';
  assert.deepEqual(coveringCommands(raw).map((c) => c.command), ['wait', 'pr', 'labels']);
});

test('coveringCommand leaves actions, other reads and the CLI itself alone', () => {
  for (const raw of [
    'git commit -m "feat: x"',
    'gh api -X POST repos/o/r/rulesets',
    'gh pr create --base develop',
    'ls -lt docs/architecture/decisions/0028-x.md',
    'cat apps/services/api/build.gradle.kts',
    'bin/agent-cli pr 92',
    './gradlew spotlessApply',
  ]) {
    assert.equal(coveringCommand(raw), null, raw);
  }
});

test('bashCalls reads Bash calls, their result size and the since filter', () => {
  const dir = mkdtempSync(join(tmpdir(), 'agent-cli-'));
  const file = join(dir, '0123abcd-0000-0000-0000-000000000000.jsonl');
  const use = (id, command, timestamp) => JSON.stringify({ timestamp, message: { content: [{ type: 'tool_use', id, name: 'Bash', input: { command } }] } });
  const result = (id, content) => JSON.stringify({ message: { content: [{ type: 'tool_result', tool_use_id: id, content }] } });
  writeFileSync(file, [use('a', 'git status', '2026-10-01T00:00:00Z'), result('a', 'x'.repeat(10)), use('b', 'gh pr checks 1', '2026-11-01T00:00:00Z'), 'not json'].join('\n'));
  const calls = bashCalls([file]);
  assert.equal(calls.length, 2);
  assert.deepEqual(calls[0], { session: '0123abcd', time: '2026-10-01T00:00:00Z', command: 'git status', bytes: 12 });
  assert.deepEqual(bashCalls([file], '2026-10-15').map((c) => c.command), ['gh pr checks 1']);
});

test('every command has a summary, a usage, a run and a text, and a unique name', () => {
  const names = REGISTRY.map((e) => e.name);
  assert.equal(new Set(names).size, names.length);
  for (const e of REGISTRY) {
    assert.ok(e.summary && e.usage.startsWith(e.name) && typeof e.run === 'function' && typeof e.text === 'function', e.name);
  }
});

test('renderDocs lists every command with its usage', () => {
  const page = renderDocs();
  for (const e of REGISTRY) assert.ok(page.includes(`### ${e.name}\n`) && page.includes(`bin/agent-cli ${e.usage}`), e.name);
});

test('every command answers --help', () => {
  const cli = fileURLToPath(new URL('./agent-cli.mjs', import.meta.url));
  for (const e of REGISTRY) {
    const r = spawnSync(process.execPath, [cli, e.name, '--help'], { encoding: 'utf8' });
    assert.equal(r.status, 0, e.name);
    assert.ok(r.stdout.startsWith(`usage: bin/agent-cli ${e.name}`), e.name);
  }
});

test('the hook says which command covers a raw lookup, and stays silent otherwise', () => {
  const cli = fileURLToPath(new URL('./agent-cli.mjs', import.meta.url));
  const hook = (input) => spawnSync(process.execPath, [cli, 'hook'], { input: JSON.stringify(input), encoding: 'utf8' });
  const covered = hook({ tool_name: 'Bash', tool_input: { command: 'gh pr checks 92' } });
  assert.equal(covered.status, 0);
  assert.match(JSON.parse(covered.stdout).hookSpecificOutput.additionalContext, /bin\/agent-cli pr covers/);
  assert.equal(hook({ tool_name: 'Bash', tool_input: { command: 'git commit -m x' } }).stdout, '');
  assert.equal(spawnSync(process.execPath, [cli, 'hook'], { input: 'not json', encoding: 'utf8' }).status, 0);
});

test('selftest passes on this repository', () => {
  const cli = fileURLToPath(new URL('./agent-cli.mjs', import.meta.url));
  const r = spawnSync(process.execPath, [cli, 'selftest'], { encoding: 'utf8' });
  assert.equal(r.status, 0, r.stdout + r.stderr);
});
