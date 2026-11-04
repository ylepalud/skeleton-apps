// The lookups agents repeat on this repository, answered from one place.
// Every answer is read from its source at run time; a source that no longer
// parses fails with exit 3 ("stale") rather than answering wrong.
// Reference: docs/contributor/reference/agent-cli.md, generated from REGISTRY.
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync, mkdirSync, openSync, closeSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export const DOC_PAGE = 'docs/contributor/reference/agent-cli.md';

/** A source the CLI reads changed shape: fix bin/agent-cli, never trust its answer. */
export class Stale extends Error {
  constructor(source, what) {
    super(`stale: ${source}: ${what}; fix bin/agent-cli.mjs`);
  }
}

/** A tool, a remote or a resource the command needs is not there. */
export class Unavailable extends Error {}

/** The command line is wrong. */
export class UsageError extends Error {}

const EXIT = { ok: 0, failed: 1, usage: 2, unavailable: 4, stale: 3 };

// ---------------------------------------------------------------- helpers

function read(rel) {
  const path = join(ROOT, rel);
  if (!existsSync(path)) throw new Stale(rel, 'missing');
  return readFileSync(path, 'utf8');
}

function run(cmd, args, { allowFail = false } = {}) {
  const result = spawnSync(cmd, args, { cwd: ROOT, encoding: 'utf8', maxBuffer: 256 * 1024 * 1024 });
  if (result.error?.code === 'ENOENT') throw new Unavailable(`${cmd} is not installed`);
  if (result.status !== 0 && !allowFail) {
    const reason = (result.stderr || result.stdout || '').trim().split('\n')[0];
    throw new Unavailable(`${cmd} ${args.slice(0, 3).join(' ')}: ${reason}`);
  }
  return result.status === 0 ? result.stdout : '';
}

const gh = (...args) => {
  const out = run('gh', args);
  return out.trim() ? JSON.parse(out) : null;
};

const ghOptional = (...args) => {
  const out = run('gh', args, { allowFail: true });
  return out.trim() ? JSON.parse(out) : null;
};

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

function number(value, what) {
  const n = Number(String(value ?? '').replace(/^#/, ''));
  if (!Number.isInteger(n) || n <= 0) throw new UsageError(`${what}: expected a number, got ${value}`);
  return n;
}

const list = (values) => (values.length ? values.join(',') : '-');

/** Markdown files under a folder, relative to ROOT, sorted. */
function markdownFiles(dir) {
  const out = [];
  const walk = (rel) => {
    for (const entry of readdirSync(join(ROOT, rel), { withFileTypes: true })) {
      const child = `${rel}/${entry.name}`;
      if (entry.isDirectory()) walk(child);
      else if (entry.name.endsWith('.md')) out.push(child);
    }
  };
  if (!existsSync(join(ROOT, dir))) throw new Stale(dir, 'missing');
  walk(dir);
  return out.sort();
}

const firstHeading = (text) => text.match(/^#\s+(.+)$/m)?.[1].trim() ?? '';

// ---------------------------------------------------------------- parsers (pure, unit-tested)

/** Gradle projects and the root project name from settings.gradle.kts. */
export function parseSettings(text) {
  const name = text.match(/rootProject\.name\s*=\s*"([^"]+)"/)?.[1];
  const projects = [...text.matchAll(/^\s*include\(\s*"([^"]+)"\s*\)/gm)].map((m) => `:${m[1].replace(/^:/, '')}`);
  if (!name) throw new Stale('settings.gradle.kts', 'no rootProject.name');
  if (!projects.length) throw new Stale('settings.gradle.kts', 'no include("...")');
  return { name, projects };
}

/** Plugin ids of a build script's plugins { } block. */
export function parsePlugins(text) {
  const block = text.match(/^plugins\s*\{([\s\S]*?)^\}/m)?.[1] ?? '';
  return [...block.matchAll(/id\("([^"]+)"\)|`([^`]+)`|^\s*(\w[\w-]*)\s*$/gm)].map((m) => m[1] ?? m[2] ?? m[3]);
}

/** Rows of the "Before you…" table of AGENTS.md. */
export function parseAgentsTable(text) {
  const section = text.split(/^## /m).find((s) => s.startsWith('Before you'));
  if (!section) throw new Stale('AGENTS.md', 'no "## Before you" section');
  const rows = [...section.matchAll(/^\|\s*(?!…|-)([^|]+?)\s*\|\s*\[[^\]]*\]\(([^)]+)\)\s*\|$/gm)].map((m) => ({
    when: m[1],
    path: m[2],
  }));
  if (!rows.length) throw new Stale('AGENTS.md', 'no row in the "Before you" table');
  return rows;
}

/** Number, title and status of an ADR. */
export function parseAdr(file, text) {
  const heading = text.match(/^#\s+(\d{4})\.\s+(.+)$/m);
  if (!heading) throw new Stale(file, 'no "# NNNN. Title" heading');
  return { number: heading[1], title: heading[2].trim(), status: text.match(/^- Status:\s*(.+)$/m)?.[1].trim() ?? '-', file };
}

/** Name, triggers and job ids of a GitHub Actions workflow, without a YAML parser. */
export function parseWorkflow(file, text) {
  const lines = text.split('\n');
  const name = lines.find((l) => /^name:/.test(l))?.replace(/^name:\s*/, '').replace(/^["']|["']$/g, '') ?? '';
  const childKeys = (key) => {
    const start = lines.findIndex((l) => new RegExp(`^["']?${key}["']?:`).test(l));
    if (start < 0) return null;
    const inline = lines[start].replace(/^[^:]+:\s*/, '').trim();
    if (inline) return inline.replace(/[[\]]/g, '').split(',').map((s) => s.trim()).filter(Boolean);
    const keys = [];
    for (const line of lines.slice(start + 1)) {
      if (/^\S/.test(line)) break;
      const m = line.match(/^ {2}([\w-]+):/);
      if (m) keys.push(m[1]);
    }
    return keys;
  };
  const jobs = childKeys('jobs');
  if (!jobs) throw new Stale(file, 'no jobs:');
  return { file, name, on: childKeys('on') ?? [], jobs };
}

/** Counts and non-passing entries of a PR's statusCheckRollup. */
export function summarizeChecks(rollup) {
  const checks = (rollup ?? []).map((c) => {
    const name = c.name ?? c.context ?? '?';
    const raw = (c.conclusion || c.state || c.status || '').toUpperCase();
    const state =
      c.status && c.status !== 'COMPLETED'
        ? 'pending'
        : ['SUCCESS', 'NEUTRAL', 'SKIPPED'].includes(raw)
          ? raw === 'SUCCESS' ? 'pass' : 'skip'
          : ['PENDING', 'EXPECTED', 'QUEUED', 'IN_PROGRESS', ''].includes(raw)
            ? 'pending'
            : 'fail';
    const runId = (c.detailsUrl ?? c.targetUrl ?? '').match(/\/actions\/runs\/(\d+)/)?.[1] ?? null;
    return { name, workflow: c.workflowName ?? '', state, runId };
  });
  const count = (s) => checks.filter((c) => c.state === s).length;
  return {
    total: checks.length,
    pass: count('pass'),
    fail: count('fail'),
    pending: count('pending'),
    skip: count('skip'),
    notPassing: checks.filter((c) => c.state === 'fail' || c.state === 'pending'),
  };
}

const LOG_SIGNAL = /(##\[error\]|\berror\b|\bErrors?:|FAILED|FAILURE|Exception|^\s*fail\s|✖|✗|✘|\bfailed\b|AssertionError|expected:|What went wrong)/i;
const LOG_NOISE = /(Gradle Enterprise|Develocity|--scan|Run with --|Get more help|BUILD FAILED in|deprecated Gradle features|continue-on-failure|[Cc]ache[- ]cleanup|^\s*$)/;

/** The lines of a failed log that say why, deduplicated and capped. */
export function filterLog(text, max = 40) {
  const seen = new Set();
  const out = [];
  for (const raw of text.split('\n')) {
    // gh run view --log-failed prefixes: "<job>\t<step>\t<timestamp> "
    const line = raw.replace(/^[^\t]*\t[^\t]*\t/, '').replace(/^\d{4}-\d\d-\d\dT[\d:.]+Z\s?/, '').trimEnd();
    if (!LOG_SIGNAL.test(line) || LOG_NOISE.test(line)) continue;
    const key = line.replace(/\d+/g, '#');
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(line.length > 240 ? `${line.slice(0, 240)}…` : line);
    if (out.length >= max) break;
  }
  return out;
}

/** Result, failed tasks and the first error lines of a Gradle console log. */
export function summarizeGradle(log) {
  const text = log.replace(/\x1b\[[0-9;]*m/g, '');
  const result = text.match(/BUILD (SUCCESSFUL|FAILED) in (\S+)/);
  const failedTasks = [
    ...new Set([
      ...[...text.matchAll(/^> Task (\S+) FAILED/gm)].map((m) => m[1]),
      ...[...text.matchAll(/Execution failed for task '([^']+)'/g)].map((m) => m[1]),
    ]),
  ];
  const errors = [];
  const lines = text.split('\n');
  lines.forEach((line, i) => {
    if (/^(e: |error: |Warning: |Error: |\[ERROR\]|\d+ issues? found|.* > .* FAILED$|\s+at .*Test\.|.*AssertionFailedError|.*expected:)/.test(line)) errors.push(line.trim());
    // A docs build's issue location: "╭─[ page.md:27:22 ]"
    const at = line.match(/╭─\[ (.+) \]/)?.[1];
    if (at) errors.push(`at ${at}`);
    if (/^\* What went wrong:/.test(line)) errors.push(...lines.slice(i + 1, i + 4).map((l) => l.trim()).filter(Boolean));
  });
  return {
    result: result ? (result[1] === 'SUCCESSFUL' ? 'pass' : 'fail') : 'unknown',
    duration: result?.[2] ?? '-',
    failedTasks,
    errors: [...new Set(errors)].slice(0, 30),
  };
}

/** A shell command as an agent typed it, stripped of what varies between sessions. */
export function normalizeCommand(command) {
  return command
    .replace(/^\s*cd\s+[^&;|]+(&&|;)\s*/, '')
    .replace(/\brtk\s+(proxy\s+)?/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/** The segments of a compound command, each normalized. */
export function commandSegments(command) {
  return String(command)
    .split(/\s*(?:&&|\|\||;)\s*/)
    .map(normalizeCommand)
    .filter((segment) => segment && !/^cd\b/.test(segment));
}

/**
 * The subcommands whose `match` covers a raw command, in the order its parts
 * run: the whole command first, then each segment.
 */
export function coveringCommands(command, registry = REGISTRY) {
  if (/\bagent-cli\b/.test(command)) return [];
  const found = new Map();
  for (const candidate of [normalizeCommand(command), ...commandSegments(command)]) {
    const entry = registry.find((e) => e.match?.test(candidate));
    if (entry && !found.has(entry.name)) found.set(entry.name, { command: entry.name, segment: candidate });
  }
  return [...found.values()];
}

/** The first subcommand that covers a raw command, or null. */
export const coveringCommand = (command, registry = REGISTRY) => coveringCommands(command, registry)[0] ?? null;

// ---------------------------------------------------------------- git and GitHub context

/** The branch pull requests target, or null in a checkout without it (CI's shallow clone). */
function baseBranch() {
  const head = run('git', ['symbolic-ref', '--quiet', '--short', 'refs/remotes/origin/HEAD'], { allowFail: true }).trim();
  if (head) return head;
  for (const candidate of ['origin/develop', 'origin/main', 'develop', 'main']) {
    if (run('git', ['rev-parse', '--verify', '--quiet', candidate], { allowFail: true }).trim()) return candidate;
  }
  return null;
}

const PR_FIELDS = 'number,title,state,isDraft,mergeable,mergeStateStatus,reviewDecision,baseRefName,headRefName,url,statusCheckRollup';

function pullRequest(ref) {
  const args = ['pr', 'view', ...(ref ? [String(number(ref, 'pr'))] : []), '--json', PR_FIELDS];
  const result = spawnSync('gh', args, { cwd: ROOT, encoding: 'utf8' });
  if (result.error?.code === 'ENOENT') throw new Unavailable('gh is not installed');
  if (result.status !== 0) throw new Unavailable(`no pull request${ref ? ` #${ref}` : ' for this branch'}: ${result.stderr.trim().split('\n')[0]}`);
  return JSON.parse(result.stdout);
}

// ---------------------------------------------------------------- commands

function statusCommand() {
  const base = baseBranch();
  const branch = run('git', ['rev-parse', '--abbrev-ref', 'HEAD']).trim();
  const upstream = run('git', ['rev-parse', '--abbrev-ref', '--symbolic-full-name', '@{u}'], { allowFail: true }).trim() || null;
  const dirty = run('git', ['status', '--porcelain']).split('\n').filter(Boolean).map((l) => l.trimStart());
  if (!base) return { branch, upstream, base, ahead: null, behind: null, dirty, commits: [], diff: 'no base branch' };
  const [behind, ahead] = run('git', ['rev-list', '--left-right', '--count', `${base}...HEAD`]).trim().split(/\s+/).map(Number);
  const commits = run('git', ['log', '--format=%h %s', '-20', `${base}..HEAD`]).split('\n').filter(Boolean);
  const diff = run('git', ['diff', '--shortstat', `${base}...HEAD`]).trim() || 'no change';
  return { branch, upstream, base, ahead, behind, dirty, commits, diff };
}

function statusText(d) {
  return [
    `branch ${d.branch} upstream=${d.upstream ?? '-'} base=${d.base ?? '-'} ahead=${d.ahead ?? '-'} behind=${d.behind ?? '-'}`,
    `diff ${d.diff}`,
    `dirty ${d.dirty.length}${d.dirty.length ? `: ${d.dirty.join(' | ')}` : ''}`,
    ...d.commits.map((c) => `commit ${c}`),
  ];
}

function prCommand(args) {
  const pr = pullRequest(args._[0]);
  const { statusCheckRollup, ...rest } = pr;
  return { ...rest, checks: summarizeChecks(statusCheckRollup) };
}

function prText(d) {
  const c = d.checks;
  return [
    `#${d.number} ${d.state}${d.isDraft ? ' draft' : ''} ${d.headRefName}->${d.baseRefName} mergeable=${d.mergeable} merge=${d.mergeStateStatus} review=${d.reviewDecision || '-'}`,
    d.title,
    `checks pass=${c.pass} fail=${c.fail} pending=${c.pending} skip=${c.skip}`,
    ...c.notPassing.map((x) => `${x.state} ${x.workflow ? `${x.workflow}/` : ''}${x.name}${x.runId ? ` run=${x.runId}` : ''}`),
    d.url,
  ];
}

function prsCommand(args) {
  return gh('pr', 'list', '--state', args.all ? 'all' : 'open', '--limit', String(args.limit ?? 30), '--json', 'number,title,isDraft,headRefName,baseRefName,mergeable,statusCheckRollup').map(
    ({ statusCheckRollup, ...pr }) => {
      const c = summarizeChecks(statusCheckRollup);
      return { ...pr, checks: { pass: c.pass, fail: c.fail, pending: c.pending } };
    },
  );
}

const prsText = (d) =>
  d.map((p) => `#${p.number}${p.isDraft ? ' draft' : ''} ${p.headRefName}->${p.baseRefName} mergeable=${p.mergeable} checks=${p.checks.pass}/${p.checks.fail}/${p.checks.pending} ${p.title}`);

function ciCommand(args) {
  const ref = args._[0];
  let runIds;
  let source;
  if (ref && /^\d{8,}$/.test(ref)) {
    runIds = [ref];
    source = `run ${ref}`;
  } else {
    let pr = null;
    try {
      pr = pullRequest(ref);
    } catch (e) {
      if (ref || !(e instanceof Unavailable) || /not installed/.test(e.message)) throw e;
    }
    if (pr) {
      const checks = summarizeChecks(pr.statusCheckRollup);
      runIds = [...new Set(checks.notPassing.filter((c) => c.state === 'fail' && c.runId).map((c) => c.runId))];
      source = `pr #${pr.number}`;
      if (!runIds.length) return { source, checks: { pass: checks.pass, fail: checks.fail, pending: checks.pending }, runs: [] };
    } else {
      const branch = run('git', ['rev-parse', '--abbrev-ref', 'HEAD']).trim();
      const runs = gh('run', 'list', '--branch', branch, '--limit', String(args.limit ?? 8), '--json', 'databaseId,workflowName,status,conclusion,headSha,event,createdAt');
      runIds = runs.filter((r) => r.conclusion === 'failure').slice(0, 2).map((r) => String(r.databaseId));
      source = `branch ${branch}`;
      if (!runIds.length) return { source, recent: runs.map((r) => ({ id: r.databaseId, workflow: r.workflowName, status: r.status, conclusion: r.conclusion, sha: r.headSha.slice(0, 7), event: r.event })), runs: [] };
    }
  }
  const runs = runIds.map((id) => {
    const view = gh('run', 'view', id, '--json', 'workflowName,conclusion,headSha,url,jobs');
    const failedJobs = view.jobs.filter((j) => j.conclusion === 'failure').map((j) => ({
      name: j.name,
      steps: j.steps.filter((s) => s.conclusion === 'failure').map((s) => s.name),
    }));
    const log = run('gh', ['run', 'view', id, '--log-failed'], { allowFail: true });
    return { id, workflow: view.workflowName, conclusion: view.conclusion, sha: view.headSha.slice(0, 7), url: view.url, failedJobs, errors: filterLog(log, args.lines ?? 40) };
  });
  return { source, runs };
}

function ciText(d) {
  const out = [`source ${d.source}`];
  if (d.checks) out.push(`no failed run: pass=${d.checks.pass} fail=${d.checks.fail} pending=${d.checks.pending}`);
  for (const r of d.recent ?? []) out.push(`run ${r.id} ${r.workflow} ${r.status}/${r.conclusion || '-'} ${r.sha} ${r.event}`);
  for (const r of d.runs) {
    out.push(`run ${r.id} ${r.workflow} ${r.conclusion} ${r.sha} ${r.url}`);
    for (const j of r.failedJobs) out.push(`  job ${j.name}: ${list(j.steps)}`);
    for (const e of r.errors) out.push(`  | ${e}`);
  }
  return out;
}

function waitCommand(args) {
  const deadline = Date.now() + parseDuration(args.timeout ?? '30m');
  const interval = parseDuration(args.interval ?? '20s');
  for (;;) {
    const pr = pullRequest(args._[0]);
    const checks = summarizeChecks(pr.statusCheckRollup);
    const done = args.merged ? pr.state !== 'OPEN' : checks.pending === 0 && checks.total > 0;
    if (done || Date.now() > deadline) {
      const data = { number: pr.number, state: pr.state, mergeable: pr.mergeable, timedOut: !done, checks };
      data.failed = data.timedOut || checks.fail > 0 || (args.merged && pr.state !== 'MERGED');
      return data;
    }
    sleep(interval);
  }
}

export function parseDuration(value) {
  const m = String(value).match(/^(\d+)(ms|s|m|h)?$/);
  if (!m) throw new UsageError(`duration: expected 30s, 5m or 1h, got ${value}`);
  return Number(m[1]) * { ms: 1, s: 1000, m: 60_000, h: 3_600_000 }[m[2] ?? 's'];
}

function waitText(d) {
  const c = d.checks;
  return [
    `#${d.number} ${d.state} ${d.timedOut ? 'timed out' : 'settled'} mergeable=${d.mergeable} pass=${c.pass} fail=${c.fail} pending=${c.pending}`,
    ...c.notPassing.map((x) => `${x.state} ${x.name}${x.runId ? ` run=${x.runId}` : ''}`),
  ];
}

function relatedIssues(n) {
  const subs = ghOptional('api', `repos/{owner}/{repo}/issues/${n}/sub_issues?per_page=100`) ?? [];
  const parent = ghOptional('api', `repos/{owner}/{repo}/issues/${n}/parent`);
  return {
    parent: parent?.number ? { number: parent.number, title: parent.title, state: parent.state } : null,
    subIssues: subs.map((s) => ({ number: s.number, title: s.title, state: s.state })),
  };
}

function issueCommand(args) {
  const n = number(args._[0], 'issue');
  const issue = gh('issue', 'view', String(n), '--json', 'number,title,state,labels,milestone,url,body');
  return {
    number: issue.number,
    title: issue.title,
    state: issue.state,
    labels: issue.labels.map((l) => l.name),
    milestone: issue.milestone?.title ?? null,
    url: issue.url,
    ...relatedIssues(n),
    body: args['no-body'] ? undefined : issue.body,
  };
}

function issueText(d) {
  return [
    `#${d.number} ${d.state} [${list(d.labels)}] milestone=${d.milestone ?? '-'} ${d.title}`,
    ...(d.parent ? [`parent #${d.parent.number} ${d.parent.state} ${d.parent.title}`] : []),
    ...d.subIssues.map((s) => `sub #${s.number} ${s.state} ${s.title}`),
    ...(d.body !== undefined ? ['---', d.body.trim()] : []),
  ];
}

function issuesCommand(args) {
  const filters = ['--state', args.all ? 'all' : 'open', '--limit', String(args.limit ?? 30)];
  if (args.label) filters.push('--label', args.label);
  return gh('issue', 'list', ...filters, '--json', 'number,title,state,labels,milestone').map((i) => ({
    number: i.number,
    state: i.state,
    title: i.title,
    labels: i.labels.map((l) => l.name),
    milestone: i.milestone?.title ?? null,
  }));
}

const issuesText = (d) => d.map((i) => `#${i.number} ${i.state} [${list(i.labels)}]${i.milestone ? ` (${i.milestone})` : ''} ${i.title}`);

function epicCommand(args) {
  const n = number(args._[0], 'epic');
  const issue = gh('issue', 'view', String(n), '--json', 'number,title,state,url');
  const { subIssues } = relatedIssues(n);
  if (!subIssues.length) throw new Unavailable(`#${n} has no sub-issue: not an epic`);
  const prs = gh('pr', 'list', '--state', 'open', '--limit', '100', '--json', 'number,title,body,isDraft,headRefName');
  const linked = (sub) =>
    prs.filter((p) => new RegExp(`(close[sd]?|fix(e[sd])?|resolve[sd]?|part of|refs?)\\s+#${sub}\\b`, 'i').test(`${p.title}\n${p.body}`)).map((p) => p.number);
  const subs = subIssues.map((s) => ({ ...s, openPrs: s.state === 'open' ? linked(s.number) : [] }));
  return {
    number: issue.number,
    title: issue.title,
    state: issue.state,
    url: issue.url,
    done: subs.filter((s) => s.state === 'closed').length,
    total: subs.length,
    next: subs.find((s) => s.state === 'open')?.number ?? null,
    subIssues: subs,
  };
}

function epicText(d) {
  return [
    `#${d.number} ${d.state} ${d.done}/${d.total} done next=${d.next ? `#${d.next}` : '-'} ${d.title}`,
    ...d.subIssues.map((s) => `${s.state === 'closed' ? 'x' : ' '} #${s.number} ${s.title}${s.openPrs.length ? ` pr=${s.openPrs.map((p) => `#${p}`).join(',')}` : ''}`),
  ];
}

function labelsCommand() {
  return gh('label', 'list', '--limit', '200', '--json', 'name,description').map((l) => ({ name: l.name, description: l.description }));
}

const labelsText = (d) => d.map((l) => `${l.name}${l.description ? `: ${l.description}` : ''}`);

function repoCommand() {
  const repo = gh('repo', 'view', '--json', 'nameWithOwner,defaultBranchRef,visibility,homepageUrl,squashMergeAllowed,mergeCommitAllowed,rebaseMergeAllowed,deleteBranchOnMerge');
  const rulesets = (ghOptional('api', 'repos/{owner}/{repo}/rulesets') ?? []).map((r) => {
    const detail = ghOptional('api', `repos/{owner}/{repo}/rulesets/${r.id}`) ?? {};
    const rules = detail.rules ?? [];
    return {
      name: r.name,
      enforcement: r.enforcement,
      branches: detail.conditions?.ref_name?.include ?? [],
      rules: rules.map((x) => x.type),
      requiredChecks: rules.filter((x) => x.type === 'required_status_checks').flatMap((x) => x.parameters.required_status_checks.map((c) => c.context)),
      bypass: (detail.bypass_actors ?? []).map((b) => `${b.actor_type}:${b.actor_id ?? '-'}`),
    };
  });
  const names = (kind) => (ghOptional(kind, 'list', '--json', 'name') ?? []).map((v) => v.name);
  const merge = [
    ['squash', 'squashMergeAllowed'],
    ['merge', 'mergeCommitAllowed'],
    ['rebase', 'rebaseMergeAllowed'],
  ]
    .filter(([, field]) => repo[field])
    .map(([method]) => method);
  return {
    repo: repo.nameWithOwner,
    defaultBranch: repo.defaultBranchRef?.name,
    visibility: repo.visibility,
    homepage: repo.homepageUrl || null,
    merge,
    deleteBranchOnMerge: repo.deleteBranchOnMerge,
    pages: ghOptional('api', 'repos/{owner}/{repo}/pages')?.html_url ?? null,
    rulesets,
    variables: names('variable'),
    secrets: names('secret'),
  };
}

function repoText(d) {
  return [
    `${d.repo} ${d.visibility} default=${d.defaultBranch} merge=${list(d.merge)} deleteBranchOnMerge=${d.deleteBranchOnMerge}`,
    `pages=${d.pages ?? '-'} homepage=${d.homepage ?? '-'}`,
    ...d.rulesets.map((r) => `ruleset ${r.name} ${r.enforcement} branches=${list(r.branches)} rules=${list(r.rules)} checks=${list(r.requiredChecks)} bypass=${list(r.bypass)}`),
    `variables ${list(d.variables)}`,
    `secrets ${list(d.secrets)}`,
  ];
}

function modulesCommand() {
  const settings = parseSettings(read('settings.gradle.kts'));
  const modules = settings.projects.map((path) => {
    const dir = path.slice(1).replaceAll(':', '/');
    const script = ['build.gradle.kts', 'build.gradle'].map((f) => `${dir}/${f}`).find((f) => existsSync(join(ROOT, f)));
    if (!script) throw new Stale(dir, `no build script for ${path}`);
    return { path, dir, plugins: parsePlugins(read(script)), check: `${path}:check` };
  });
  // Folders under apps/ that hold a product but no Gradle project, driven by the root build.
  const gradleDirs = new Set(modules.map((m) => m.dir));
  const others = [];
  const scan = (rel, depth) => {
    if (!existsSync(join(ROOT, rel)) || depth > 2) return;
    for (const entry of readdirSync(join(ROOT, rel), { withFileTypes: true })) {
      const child = `${rel}/${entry.name}`;
      if (!entry.isDirectory() || entry.name.startsWith('.') || ['build', 'node_modules'].includes(entry.name)) continue;
      if (gradleDirs.has(child)) continue;
      if ([...gradleDirs].some((d) => d.startsWith(`${child}/`))) scan(child, depth + 1);
      else others.push(child);
    }
  };
  scan('apps', 0);
  return { root: settings.name, modules, notGradle: others };
}

function modulesText(d) {
  return [
    `root ${d.root}`,
    ...d.modules.map((m) => `${m.path} ${m.dir} plugins=${list(m.plugins)}`),
    ...d.notGradle.map((dir) => `- ${dir} not a Gradle project (driven by the root build)`),
  ];
}

function words(text) {
  return new Set(text.toLowerCase().split(/[^a-z0-9]+/).filter((w) => w.length > 2 && !['the', 'and', 'any', 'this', 'how', 'for'].includes(w)));
}

function docCommand(args) {
  const table = parseAgentsTable(read('AGENTS.md'));
  for (const row of table) if (!existsSync(join(ROOT, row.path))) throw new Stale('AGENTS.md', `${row.path} does not exist`);
  if (args.all) {
    return markdownFiles('docs').map((file) => {
      const text = read(file);
      return { path: file, title: firstHeading(text), lines: text.split('\n').length };
    });
  }
  const query = args._.join(' ');
  if (!query) return table;
  const wanted = words(query);
  const score = (text) => [...words(text)].filter((w) => [...wanted].some((q) => w.startsWith(q) || q.startsWith(w))).length;
  const pages = markdownFiles('docs').map((file) => ({ path: file, title: firstHeading(read(file)) }));
  const rows = [
    ...table.map((r) => ({ path: r.path, title: r.when, score: score(r.when) * 2 + score(r.path) })),
    ...pages.map((p) => ({ path: p.path, title: p.title, score: score(p.title) + score(p.path) })),
  ].filter((r) => r.score > 0);
  const best = new Map();
  for (const r of rows.sort((a, b) => b.score - a.score)) if (!best.has(r.path)) best.set(r.path, r);
  return [...best.values()].slice(0, Number(args.limit ?? 5));
}

const docText = (d) => d.map((r) => (r.when ? `${r.path} <- ${r.when}` : `${r.path}${r.lines ? ` ${r.lines}l` : ''} ${r.title}`));

const ADR_DIR = 'docs/architecture/decisions';

function adrCommand(args) {
  const adrs = markdownFiles(ADR_DIR)
    .filter((f) => /\/\d{4}-[^/]+\.md$/.test(f))
    .map((f) => parseAdr(f, read(f)));
  if (!adrs.length) throw new Stale(ADR_DIR, 'no NNNN-*.md file');
  const next = String(Math.max(...adrs.map((a) => Number(a.number))) + 1).padStart(4, '0');
  const query = args._.join(' ');
  const wanted = words(query);
  const shown = query ? adrs.filter((a) => [...words(a.title + a.file)].some((w) => [...wanted].some((q) => w.startsWith(q)))) : adrs;
  return { next, index: 'docs/architecture/decisions.md', adrs: shown };
}

const adrText = (d) => [`next ${d.next} (add a row to ${d.index})`, ...d.adrs.map((a) => `${a.number} ${a.status} ${a.title}`)];

function workflowsCommand() {
  const dir = '.github/workflows';
  if (!existsSync(join(ROOT, dir))) throw new Stale(dir, 'missing');
  return readdirSync(join(ROOT, dir))
    .filter((f) => /\.ya?ml$/.test(f))
    .sort()
    .map((f) => parseWorkflow(`${dir}/${f}`, read(`${dir}/${f}`)));
}

const workflowsText = (d) => d.map((w) => `${w.file} "${w.name}" on=${list(w.on)} jobs=${list(w.jobs)}`);

const EM_DASH = /\u2014/;
// The CI step that greps the Markdown for em dashes holds one.
const LINT_IGNORED = ['.github/workflows/ci.yml'];

function lintCommand() {
  const files = run('git', ['ls-files', '-z'])
    .split('\0')
    .filter((f) => f && !LINT_IGNORED.includes(f));
  const hits = [];
  for (const file of files) {
    const path = join(ROOT, file);
    if (!existsSync(path) || statSync(path).size > 2_000_000) continue;
    const buffer = readFileSync(path);
    if (buffer.includes(0)) continue;
    buffer
      .toString('utf8')
      .split('\n')
      .forEach((line, i) => {
        const col = line.search(EM_DASH);
        if (col >= 0) hits.push({ file, line: i + 1, col: col + 1, text: line.trim().slice(0, 120) });
      });
  }
  return { rule: 'no em dash (U+2014) in tracked text files', hits, failed: hits.length > 0 };
}

const lintText = (d) => (d.hits.length ? d.hits.map((h) => `${h.file}:${h.line}:${h.col} ${h.text}`) : [`ok ${d.rule}`]);

/** Gradle tasks for the targets of `check`: docs, a module's short name, or a raw task. */
export function checkTasks(targets, modules) {
  if (!targets.length) return ['check'];
  return targets.flatMap((t) => {
    if (t === 'docs') return ['docsLint', 'docsBuild'];
    if (t.startsWith(':') || /^[a-z][A-Za-z]*$/.test(t) && !modules.some((m) => m.path.endsWith(`:${t}`))) return [t];
    const found = modules.filter((m) => m.path.endsWith(`:${t}`) || m.dir === t || m.dir.endsWith(`/${t}`));
    if (found.length === 1) return [found[0].check];
    if (found.length > 1) throw new UsageError(`check ${t}: ambiguous, one of ${found.map((m) => m.path).join(' ')}`);
    return [t];
  });
}

function checkCommand(args) {
  const { modules } = modulesCommand();
  const tasks = checkTasks(args._, modules);
  const logDir = join(ROOT, 'build/agent-cli');
  mkdirSync(logDir, { recursive: true });
  const log = join(logDir, 'check.log');
  const fd = openSync(log, 'w');
  const started = Date.now();
  const result = spawnSync(join(ROOT, 'gradlew'), [...tasks, '--console=plain', ...(args['--'] ?? [])], { cwd: ROOT, stdio: ['ignore', fd, fd] });
  closeSync(fd);
  if (result.error) throw new Unavailable(`gradlew: ${result.error.message}`);
  const summary = summarizeGradle(readFileSync(log, 'utf8'));
  return { tasks, exit: result.status, seconds: Math.round((Date.now() - started) / 1000), log: relative(ROOT, log), ...summary, failed: result.status !== 0 };
}

function checkText(d) {
  return [
    `${d.result} exit=${d.exit} ${d.duration} tasks=${d.tasks.join(' ')} log=${d.log}`,
    ...d.failedTasks.map((t) => `failed ${t}`),
    ...d.errors.map((e) => `| ${e}`),
  ];
}

// ---------------------------------------------------------------- transcripts (stats)

/** Claude Code transcript folders of this checkout and its worktrees. */
function transcriptDirs(extra = []) {
  const base = join(process.env.CLAUDE_CONFIG_DIR ?? join(homedir(), '.claude'), 'projects');
  const prefix = ROOT.replace(/[^A-Za-z0-9]/g, '-');
  const own = existsSync(base) ? readdirSync(base).filter((d) => d === prefix || d.startsWith(`${prefix}-`)).map((d) => join(base, d)) : [];
  return [...own, ...extra.map((d) => resolve(d))];
}

function jsonlFiles(dir, out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory() && entry.name !== 'memory') jsonlFiles(path, out);
    else if (entry.name.endsWith('.jsonl')) out.push(path);
  }
  return out;
}

/** Every Bash command of the transcripts: session, time, command, result size. */
export function bashCalls(files, since) {
  const calls = [];
  for (const file of files) {
    const session = file.match(/([0-9a-f]{8})-[0-9a-f-]{27}/)?.[1] ?? file;
    const uses = new Map();
    const sizes = new Map();
    for (const line of readFileSync(file, 'utf8').split('\n')) {
      if (!line.includes('"tool_')) continue;
      let record;
      try {
        record = JSON.parse(line);
      } catch {
        continue;
      }
      const content = record.message?.content;
      if (!Array.isArray(content)) continue;
      for (const block of content) {
        if (block.type === 'tool_use' && block.name === 'Bash') uses.set(block.id, { command: String(block.input?.command ?? ''), time: record.timestamp ?? '' });
        if (block.type === 'tool_result') sizes.set(block.tool_use_id, JSON.stringify(block.content ?? '').length);
      }
    }
    for (const [id, use] of uses) {
      if (since && use.time && use.time < since) continue;
      calls.push({ session, time: use.time, command: use.command, bytes: sizes.get(id) ?? 0 });
    }
  }
  return calls;
}

/** Commands that do something rather than look something up, or shell syntax: never candidates. */
const NOT_A_LOOKUP = /^(cd|echo|printf|done|do|then|fi|else|for|while|until|if|break|exit|sleep|mkdir|rm|mv|cp|touch|chmod|cat >|cat >>|sed -i|python3? -|node -e|git (add|commit|push|switch|checkout|merge|rebase|stash|mv|rm|reset|restore|pull|fetch|tag|cherry-pick)|gh (pr (create|merge|edit|comment|close|ready|update-branch)|issue (create|edit|comment|close)|api -X|workflow run|project item)|[)}\]"'`]|assert|EOF|#)/;

/** The kind of a normalized command: its first words, which group its variants. */
function commandKind(command) {
  const t = command.split(' ');
  if (t[0] === 'gh' || t[0] === './gradlew') return t.filter((w) => !w.startsWith('-')).slice(0, 3).join(' ');
  return t.slice(0, 2).join(' ');
}

function statsCommand(args) {
  const dirs = transcriptDirs(args.dir ? [].concat(args.dir) : []);
  if (!dirs.length) throw new Unavailable('no Claude Code transcript for this checkout');
  const calls = bashCalls(dirs.flatMap((d) => jsonlFiles(d)), args.since);
  const minSessions = Number(args.min ?? 3);
  const perCommand = REGISTRY.filter((e) => e.match).map((e) => ({ command: e.name, rawCalls: 0, rawSessions: new Set(), rawBytes: 0, cliCalls: 0 }));
  const byName = new Map(perCommand.map((p) => [p.command, p]));
  const kinds = new Map();
  for (const call of calls) {
    const cli = call.command.match(/agent-cli(?:\.mjs)?\s+([a-z]+)/)?.[1];
    if (cli && byName.has(cli)) {
      byName.get(cli).cliCalls++;
      continue;
    }
    const covered = coveringCommands(call.command);
    for (const { command } of covered) {
      const p = byName.get(command);
      p.rawCalls++;
      p.rawSessions.add(call.session);
      // A compound call's output is shared between the lookups it made.
      p.rawBytes += call.bytes / covered.length;
    }
    if (covered.length) continue;
    for (const segment of commandSegments(call.command)) {
      if (NOT_A_LOOKUP.test(segment)) continue;
      const kind = commandKind(segment);
      const k = kinds.get(kind) ?? { kind, sessions: new Set(), calls: 0, example: segment.slice(0, 100) };
      k.sessions.add(call.session);
      k.calls++;
      kinds.set(kind, k);
    }
  }
  return {
    sessions: new Set(calls.map((c) => c.session)).size,
    bashCalls: calls.length,
    since: args.since ?? null,
    commands: perCommand.map((p) => ({
      command: p.command,
      rawCalls: p.rawCalls,
      rawSessions: p.rawSessions.size,
      rawTokensPerCall: p.rawCalls ? Math.round(p.rawBytes / p.rawCalls / 4) : 0,
      cliCalls: p.cliCalls,
    })),
    candidates: [...kinds.values()]
      .filter((k) => k.sessions.size >= minSessions)
      .sort((a, b) => b.sessions.size - a.sessions.size || b.calls - a.calls)
      .slice(0, Number(args.limit ?? 15))
      .map((k) => ({ kind: k.kind, sessions: k.sessions.size, calls: k.calls, example: k.example })),
  };
}

function statsText(d) {
  return [
    `sessions ${d.sessions} bash ${d.bashCalls}${d.since ? ` since ${d.since}` : ''}`,
    'command raw-calls raw-sessions tokens/raw-call cli-calls',
    ...d.commands.map((c) => `${c.command} ${c.rawCalls} ${c.rawSessions} ${c.rawTokensPerCall} ${c.cliCalls}`),
    'uncovered sessions calls kind | example',
    ...d.candidates.map((c) => `? ${c.sessions} ${c.calls} ${c.kind} | ${c.example}`),
  ];
}

// ---------------------------------------------------------------- hook, docs, selftest

/** PostToolUse hook: when a Bash command did what a subcommand does, say so. Never fails. */
function hookCommand() {
  try {
    const input = JSON.parse(readFileSync(0, 'utf8') || '{}');
    if (input.tool_name !== 'Bash') return null;
    const covered = coveringCommand(String(input.tool_input?.command ?? ''));
    if (!covered) return null;
    const entry = REGISTRY.find((e) => e.name === covered.command);
    return {
      hookSpecificOutput: {
        hookEventName: 'PostToolUse',
        additionalContext: `bin/agent-cli ${entry.name} covers \`${covered.segment.slice(0, 80)}\` (${entry.summary}); prefer it next time, \`bin/agent-cli ${entry.name} --help\` for options.`,
      },
    };
  } catch {
    return null;
  }
}

export function renderDocs(registry = REGISTRY) {
  const out = [
    '# Agent CLI',
    '',
    '<!-- Generated by `bin/agent-cli docs --write` from the registry in bin/agent-cli.mjs; edit the registry, not this page. -->',
    '',
    '`bin/agent-cli` answers the lookups agents repeat on this repository: the state of the branch, its pull request and CI, issues and epics, the modules, the documentation, the decisions, and a terse `./gradlew` run. It needs Node 22 or later and, for the GitHub commands, an authenticated `gh`. `AGENTS.md` sends agents to it first.',
    '',
    'Every command prints terse lines for an agent, or JSON with `--json`. Exit codes: 0 done, 1 the answer is a failure (a red check, a lint hit), 2 wrong usage, 3 stale, 4 unavailable (no `gh`, no pull request).',
    '',
    '## Keeping it current',
    '',
    '- **Read at run time.** No answer is stored: each command reads the file or calls the tool that owns the fact.',
    '- **Stale fails loudly.** A source that no longer parses gives exit 3 and `stale: <source>: <what>`, never an empty or partial answer.',
    '- **Checked with the build.** `./gradlew check` runs `agentCliTest`: the unit tests, then `selftest`, which runs every offline command against the repository and checks that this page matches the registry. A change that moves a file the CLI reads fails in its own pull request.',
    '- **Usage feeds back.** A `PostToolUse` hook in `.claude/settings.json` runs `agent-cli hook`: when an agent types a raw command a subcommand covers, the hook tells it so. `agent-cli stats` reads the Claude Code transcripts of this checkout and lists, per command, the raw calls still made and the calls through the CLI, plus the repeated lookups no command covers yet: add those, delete the commands nobody calls.',
    '',
    'To add a command, add an entry to `REGISTRY` in `bin/agent-cli.mjs` (with its `sources` and `match`), a test in `bin/agent-cli.test.mjs`, then run `bin/agent-cli docs --write`.',
    '',
    '## Commands',
    '',
    '| Command | What it answers |',
    '| --- | --- |',
    ...registry.map((e) => `| [\`${e.name}\`](#${e.name}) | ${e.summary} |`),
    '',
  ];
  for (const e of registry) {
    out.push(`### ${e.name}`, '', e.summary.charAt(0).toUpperCase() + e.summary.slice(1) + '.', '', '```text', `bin/agent-cli ${e.usage}`, ...(e.options ?? []).map(([flag, text]) => `  ${flag.padEnd(16)} ${text}`), '```', '');
    if (e.sources?.length) out.push(`Reads: ${e.sources.map((s) => `\`${s}\``).join(', ')}.`, '');
    if (e.replaces?.length) out.push(`Replaces: ${e.replaces.map((s) => `\`${s}\``).join(', ')}.`, '');
  }
  return `${out.join('\n').trimEnd()}\n`;
}

function docsCommand(args) {
  const page = renderDocs();
  if (args.write) {
    writeFileSync(join(ROOT, DOC_PAGE), page);
    return { written: DOC_PAGE };
  }
  if (args.check) {
    const current = existsSync(join(ROOT, DOC_PAGE)) ? readFileSync(join(ROOT, DOC_PAGE), 'utf8') : '';
    return { page: DOC_PAGE, upToDate: current === page, failed: current !== page };
  }
  return { markdown: page };
}

const docsText = (d) => (d.markdown ? [d.markdown.trimEnd()] : d.written ? [`wrote ${d.written}`] : [d.upToDate ? `ok ${d.page}` : `outdated ${d.page}: run bin/agent-cli docs --write`]);

function selftestCommand() {
  const results = [];
  const record = (name, fn) => {
    try {
      const detail = fn();
      results.push({ name, ok: true, detail: detail ?? '' });
    } catch (e) {
      results.push({ name, ok: false, detail: e.message });
    }
  };
  for (const e of REGISTRY) {
    record(`registry ${e.name}`, () => {
      if (!e.summary || !e.usage || !e.run) throw new Error('needs summary, usage and run');
      for (const s of e.sources ?? []) if (!existsSync(join(ROOT, s))) throw new Stale(s, `declared by ${e.name}, missing`);
    });
  }
  const git = existsSync(join(ROOT, '.git'));
  for (const e of REGISTRY.filter((x) => x.selftest)) {
    if (e.name === 'status' && !git) continue;
    record(`run ${e.name}`, () => {
      const data = e.run({ _: e.selftest === true ? [] : e.selftest });
      if (data == null || (Array.isArray(data) && !data.length)) throw new Error('empty answer');
      return 'ok';
    });
  }
  record('docs page', () => {
    if (docsCommand({ check: true }).failed) throw new Error(`${DOC_PAGE} differs from the registry: run bin/agent-cli docs --write`);
  });
  record('settings hook', () => {
    const settings = JSON.parse(read('.claude/settings.json'));
    const hooks = JSON.stringify(settings.hooks?.PostToolUse ?? []);
    if (!hooks.includes('agent-cli.mjs') || !hooks.includes(' hook')) throw new Stale('.claude/settings.json', 'no PostToolUse hook running agent-cli hook');
  });
  const failed = results.filter((r) => !r.ok);
  return { passed: results.length - failed.length, failedCount: failed.length, results, failed: failed.length > 0 };
}

const selftestText = (d) => [`selftest ${d.failed ? 'fail' : 'pass'} ${d.passed}/${d.results.length}`, ...d.results.filter((r) => !r.ok).map((r) => `fail ${r.name}: ${r.detail}`)];

// ---------------------------------------------------------------- registry

const JSON_OPTION = ['--json', 'machine-readable output'];

/**
 * Every command. `match` recognises, in a normalized raw command, the lookup
 * the command replaces: the hook and `stats` use it. `selftest` runs the
 * command offline during `selftest` (true, or the positional arguments).
 */
export const REGISTRY = [
  {
    name: 'status',
    summary: 'branch, upstream, ahead and behind the base branch, dirty files, commits and diff size since the base',
    usage: 'status [--json]',
    sources: [],
    replaces: ['git status', 'git log base..HEAD', 'git diff --stat base...HEAD', 'git branch', 'git rev-parse --abbrev-ref HEAD'],
    match: /^git (status|log\b.*\.\.|diff --(stat|shortstat|name-only)|branch( -a| -vv| --show-current)?$|rev-parse --abbrev-ref)/,
    selftest: true,
    run: statusCommand,
    text: statusText,
  },
  {
    name: 'pr',
    summary: 'pull request state, mergeability, review and its checks, failing ones by name',
    usage: 'pr [<number>] [--json]',
    sources: [],
    replaces: ['gh pr view', 'gh pr checks', 'gh pr status'],
    match: /^gh pr (view|checks|status)\b/,
    run: prCommand,
    text: prText,
  },
  {
    name: 'prs',
    summary: 'open pull requests, one line each: branches, mergeability, checks as pass/fail/pending',
    usage: 'prs [--all] [--limit 30] [--json]',
    options: [['--all', 'closed and merged ones too'], ['--limit <n>', 'at most (30)'], JSON_OPTION],
    sources: [],
    replaces: ['gh pr list --state open'],
    match: /^gh pr list\b/,
    run: prsCommand,
    text: prsText,
  },
  {
    name: 'ci',
    summary: 'failed CI runs of the pull request, a run or the branch: failed jobs, steps and the log lines that say why',
    usage: 'ci [<pr> | <run-id>] [--lines <n>] [--json]',
    options: [['--lines <n>', 'error lines kept per run (40)'], JSON_OPTION],
    sources: [],
    replaces: ['gh run list', 'gh run view --log-failed | grep error'],
    match: /^gh run (view|list)\b/,
    run: ciCommand,
    text: ciText,
  },
  {
    name: 'wait',
    summary: 'blocks until the pull request checks settle, or until it is merged; exit 1 if a check fails',
    usage: 'wait [<pr>] [--merged] [--timeout 30m] [--interval 20s] [--json]',
    options: [['--merged', 'wait for the pull request to be merged or closed'], ['--timeout <d>', 'give up after (30m)'], ['--interval <d>', 'poll every (20s)'], JSON_OPTION],
    sources: [],
    replaces: ['until gh pr view ... = MERGED; do sleep; done', 'gh run watch', 'gh pr checks --watch'],
    match: /^(until|while)\b.*\bgh (pr|run)\b|^gh run watch\b|^gh pr checks\b.*--watch/,
    run: waitCommand,
    text: waitText,
  },
  {
    name: 'issue',
    summary: 'an issue: state, labels, milestone, parent and sub-issues, body',
    usage: 'issue <number> [--no-body] [--json]',
    options: [['--no-body', 'leave the body out'], JSON_OPTION],
    sources: [],
    replaces: ['gh issue view <n> --json title,body,labels'],
    match: /^gh issue view\b/,
    run: issueCommand,
    text: issueText,
  },
  {
    name: 'issues',
    summary: 'open issues, one line each, with labels and milestone',
    usage: 'issues [--all] [--label <name>] [--limit 30] [--json]',
    options: [['--all', 'closed ones too'], ['--label <name>', 'only this label'], ['--limit <n>', 'at most (30)'], JSON_OPTION],
    sources: [],
    replaces: ['gh issue list'],
    match: /^gh issue list\b/,
    run: issuesCommand,
    text: issuesText,
  },
  {
    name: 'epic',
    summary: "an epic's sub-issues with their state and open pull requests, how many are done and the next one",
    usage: 'epic <number> [--json]',
    sources: [],
    replaces: ['gh api repos/.../issues/<n>/sub_issues'],
    match: /^gh api \S*sub_issues/,
    run: epicCommand,
    text: epicText,
  },
  {
    name: 'labels',
    summary: 'the repository labels and what each is for',
    usage: 'labels [--json]',
    sources: [],
    replaces: ['gh label list'],
    match: /^gh label list\b/,
    run: labelsCommand,
    text: labelsText,
  },
  {
    name: 'repo',
    summary: 'repository settings: default branch, merge methods, rulesets and their required checks, Pages, variable and secret names',
    usage: 'repo [--json]',
    sources: [],
    replaces: ['gh repo view --json ...', 'gh api repos/.../rulesets', 'gh variable list', 'gh secret list'],
    match: /^gh (repo view\b|api repos\/\S+\/(rulesets|pages|environments)\b(?!.*-X)|variable list|secret list)/,
    run: repoCommand,
    text: repoText,
  },
  {
    name: 'modules',
    summary: 'Gradle projects with their folder and plugins, and the product folders that are not Gradle projects',
    usage: 'modules [--json]',
    sources: ['settings.gradle.kts'],
    replaces: ['cat settings.gradle.kts', 'find . -name build.gradle.kts', 'ls apps'],
    match: /^(cat|head\b.*|sed -n \S+) settings\.gradle\.kts|^find \. .*-name ['"]?\*?\.?build\.gradle\.kts|^ls (-\S+ )*apps(\/\S*)?( apps\S*)*$/,
    selftest: true,
    run: modulesCommand,
    text: modulesText,
  },
  {
    name: 'doc',
    summary: 'which page to read: the "Before you" table of AGENTS.md, the best pages for a topic, or every page with its title',
    usage: 'doc [<topic words>] [--all] [--limit 5] [--json]',
    options: [['--all', 'every page under docs/ with its title and length'], ['--limit <n>', 'matches shown for a topic (5)'], JSON_OPTION],
    sources: ['AGENTS.md', 'docs'],
    replaces: ["grep -n '' README.md AGENTS.md mkdocs.yml", 'wc -l docs/**/*.md', 'ls docs'],
    match: /^(\/usr\/bin\/)?grep -n '' .*(README|AGENTS|mkdocs)|^wc -l .*\bdocs\b|^ls (-\S+ )*docs(\/[\w/-]*)?( docs\S*)*$/,
    selftest: true,
    run: docCommand,
    text: docText,
  },
  {
    name: 'adr',
    summary: 'the decision records with status, the next free number, or those matching a topic',
    usage: 'adr [<topic words>] [--json]',
    sources: [ADR_DIR, 'docs/architecture/decisions.md'],
    replaces: ['ls docs/architecture/decisions | tail', 'head docs/architecture/decisions/<n>-*.md'],
    match: /^ls (-\S+ )*docs\/architecture\/decisions\/?(\s|$)|^head\b.*docs\/architecture\/decisions\//,
    selftest: true,
    run: adrCommand,
    text: adrText,
  },
  {
    name: 'workflows',
    summary: 'GitHub Actions workflows with their name, triggers and jobs',
    usage: 'workflows [--json]',
    sources: ['.github/workflows'],
    replaces: ['ls .github/workflows'],
    match: /^ls (-\S+ )*\.github(\/workflows)?\/?(\s|$)/,
    selftest: true,
    run: workflowsCommand,
    text: workflowsText,
  },
  {
    name: 'lint',
    summary: 'em dashes in tracked text files, which the writing rules forbid; exit 1 on a hit',
    usage: 'lint [--json]',
    sources: [],
    replaces: ["grep -rn $'\\u2014' README.md AGENTS.md docs"],
    match: /^(\/usr\/bin\/)?(git )?grep\b.*(\u2014|\\u2014)/,
    run: lintCommand,
    text: lintText,
  },
  {
    name: 'check',
    summary: 'runs Gradle tasks with the full log in build/agent-cli/check.log and prints the result, failed tasks and first errors',
    usage: 'check [docs | <module> | <task>...] [-- <gradle args>] [--json]',
    options: [['docs', 'docsLint and docsBuild'], ['<module>', "a module's short name, e.g. api or bruno: its check task"], ['<task>', 'any Gradle task, passed as is'], JSON_OPTION],
    sources: ['gradlew', 'settings.gradle.kts'],
    replaces: ['./gradlew check > log; tail log', './gradlew docsLint docsBuild 2>&1 | grep error'],
    match: /^\.\/gradlew\b.*\b(check|test|docsLint|docsBuild|integrationTest|functionalTest)\b/,
    run: checkCommand,
    text: checkText,
  },
  {
    name: 'stats',
    summary: 'from the Claude Code transcripts of this checkout: raw calls each command would have answered, calls through the CLI, and repeated lookups no command covers',
    usage: 'stats [--since <ISO date>] [--min 3] [--limit 15] [--dir <transcript folder>]... [--json]',
    options: [['--since <date>', 'only calls after this ISO date'], ['--min <n>', 'sessions a lookup must recur in to be a candidate (3)'], ['--dir <path>', 'another transcript folder, repeatable'], JSON_OPTION],
    sources: [],
    run: statsCommand,
    text: statsText,
  },
  {
    name: 'hook',
    summary: 'Claude Code PostToolUse hook: reads the tool call on stdin and, when a command covers it, says which',
    usage: 'hook < tool-call.json',
    sources: ['.claude/settings.json'],
    run: hookCommand,
    text: () => [],
  },
  {
    name: 'docs',
    summary: 'the reference page of this CLI, generated from its registry',
    usage: 'docs [--write | --check] [--json]',
    options: [['--write', `writes ${DOC_PAGE}`], ['--check', 'exit 1 when the page differs from the registry'], JSON_OPTION],
    sources: [DOC_PAGE],
    run: docsCommand,
    text: docsText,
  },
  {
    name: 'selftest',
    summary: 'checks the CLI against the repository: declared sources exist, offline commands answer, the page is current, the hook is wired',
    usage: 'selftest [--json]',
    sources: [],
    run: selftestCommand,
    text: selftestText,
  },
];

// ---------------------------------------------------------------- entry point

/** argv to { _: positionals, flag: value | true, '--': rest }. */
export function parseArgs(argv, booleans = []) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--') {
      args['--'] = argv.slice(i + 1);
      break;
    }
    if (a.startsWith('--')) {
      const [key, inline] = a.slice(2).split(/=(.*)/s);
      const value = inline ?? (booleans.includes(key) || argv[i + 1] === undefined || argv[i + 1].startsWith('--') ? true : argv[++i]);
      args[key] = args[key] === undefined ? value : [].concat(args[key], value);
    } else {
      args._.push(a);
    }
  }
  return args;
}

const BOOLEANS = ['json', 'help', 'all', 'merged', 'no-body', 'write', 'check'];

function help(entry) {
  if (entry) {
    return [
      `usage: bin/agent-cli ${entry.usage}`,
      entry.summary,
      ...(entry.options ?? [JSON_OPTION]).map(([flag, text]) => `  ${flag.padEnd(16)} ${text}`),
      ...(entry.sources?.length ? [`reads: ${entry.sources.join(', ')}`] : []),
      ...(entry.replaces?.length ? [`replaces: ${entry.replaces.join(' ; ')}`] : []),
    ].join('\n');
  }
  const width = Math.max(...REGISTRY.map((e) => e.name.length));
  return [
    'usage: bin/agent-cli <command> [args] [--json]   (bin/agent-cli <command> --help)',
    'exit: 0 ok, 1 failure answer, 2 usage, 3 stale, 4 unavailable',
    ...REGISTRY.map((e) => `${e.name.padEnd(width)}  ${e.summary}`),
  ].join('\n');
}

export function main(argv) {
  const [name, ...rest] = argv;
  if (!name || name === 'help' || name === '--help' || name === '-h') {
    const entry = REGISTRY.find((e) => e.name === rest[0]);
    process.stdout.write(`${help(entry)}\n`);
    return EXIT.ok;
  }
  const entry = REGISTRY.find((e) => e.name === name);
  if (!entry) {
    process.stderr.write(`unknown command ${name}\n${help()}\n`);
    return EXIT.usage;
  }
  const args = parseArgs(rest, BOOLEANS);
  if (args.help) {
    process.stdout.write(`${help(entry)}\n`);
    return EXIT.ok;
  }
  try {
    const data = entry.run(args);
    if (name === 'hook') {
      if (data) process.stdout.write(`${JSON.stringify(data)}\n`);
      return EXIT.ok;
    }
    const output = args.json ? JSON.stringify(data, null, 1) : entry.text(data).join('\n');
    if (output) process.stdout.write(`${output}\n`);
    return data?.failed ? EXIT.failed : EXIT.ok;
  } catch (e) {
    const code = e instanceof Stale ? EXIT.stale : e instanceof UsageError ? EXIT.usage : e instanceof Unavailable ? EXIT.unavailable : EXIT.failed;
    if (args.json) process.stdout.write(`${JSON.stringify({ error: e.message, exit: code })}\n`);
    process.stderr.write(`${e instanceof Stale || e instanceof Unavailable || e instanceof UsageError ? '' : 'error: '}${e.message}\n`);
    return code;
  }
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  process.exitCode = main(process.argv.slice(2));
}
