#!/usr/bin/env node
// Creates a new project from the skeleton-apps template:
//   npx github:ylepalud/skeleton-apps#<ref> <name> [options]
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { parseArgs } from 'node:util';
import * as p from '@clack/prompts';
import {
  defaultClientId,
  defaultOwner,
  defaultPackage,
  validateClientId,
  validateName,
  validateOwner,
  validatePackage,
} from './answers.js';
import { createArchive, initRepository } from './archive.js';
import { readTree, writeTree } from './files.js';
import { checkPrerequisites } from './prerequisites.js';
import { TEMPLATE_REPOSITORY, fetchTemplate, resolveRef } from './template.js';
import { PROVENANCE_PATH, provenancePage, transformTree } from './transform.js';

const BANNER = String.raw`
     _        _      _
 ___| | _____| | ___| |_ ___  _ __         __ _ _ __  _ __  ___
/ __| |/ / _ \ |/ _ \ __/ _ \| '_ \ _____ / _' | '_ \| '_ \/ __|
\__ \   <  __/ |  __/ || (_) | | | |_____| (_| | |_) | |_) \__ \
|___/_|\_\___|_|\___|\__\___/|_| |_|      \__,_| .__/| .__/|___/
                                               |_|   |_|
`;

const STEPS = [
  'Check prerequisites',
  'Choose the project',
  'Fetch the template',
  'Generate the project',
  'Verify the build',
  'Review',
  'Create the Git repository',
  'Create the archive',
];

/** @type {('todo' | 'current' | 'done' | 'skipped')[]} */
const status = STEPS.map(() => 'todo');

/** The step list: [x] done, [>] current, [-] skipped, [ ] to do. */
function board() {
  const marks = { todo: '[ ]', current: '[>]', done: '[x]', skipped: '[-]' };
  return STEPS.map((step, index) => `${marks[status[index]]} ${index + 1}. ${step}`).join('\n');
}

/** @param {number} index */
function start(index) {
  status[index] = 'current';
  p.note(board(), `Step ${index + 1} of ${STEPS.length}`);
}

/**
 * @param {number} index
 * @param {'done' | 'skipped'} [outcome]
 */
function finish(index, outcome = 'done') {
  status[index] = outcome;
}

/** Ends the run with a message, after cleaning up. */
class Stop extends Error {}

/**
 * Stops on a cancelled prompt.
 * @template T
 * @param {T} value
 * @returns {Exclude<T, symbol>}
 */
function answered(value) {
  if (p.isCancel(value)) {
    throw new Stop('Cancelled, nothing was written.');
  }
  return /** @type {Exclude<T, symbol>} */ (value);
}

/**
 * @param {string} key
 * @returns {string | undefined}
 */
function gitConfig(key) {
  try {
    return execFileSync('git', ['config', '--get', key], { encoding: 'utf8' }).trim();
  } catch {
    return undefined;
  }
}

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    name: { type: 'string' },
    package: { type: 'string' },
    'client-id': { type: 'string' },
    owner: { type: 'string' },
    dest: { type: 'string', default: '.' },
    ref: { type: 'string' },
    source: { type: 'string', default: TEMPLATE_REPOSITORY },
    check: { type: 'boolean', default: false },
    yes: { type: 'boolean', default: false },
    help: { type: 'boolean', default: false },
  },
});

if (values.help) {
  console.log(`Usage: npx github:ylepalud/skeleton-apps#<ref> [name] [options]

Options:
  --name <name>          project name, kebab-case (or the first argument)
  --package <package>    base Java package (default dev.<owner>.<name>)
  --client-id <id>       OIDC client id of the web app (default <name>-web)
  --owner <owner>        GitHub owner of the new repository, lower case
  --dest <folder>        where to write <name>.zip (default .)
  --ref <ref>            template tag or branch (default: latest release, else develop)
  --source <url|path>    template repository (default ${TEMPLATE_REPOSITORY})
  --check                run ./gradlew check on the generated project
  --yes                  accept the defaults and the given options, ask nothing`);
  process.exit(0);
}

async function main() {
  const interactive = !values.yes;
  console.log(BANNER);
  p.intro('Create a new project from the skeleton-apps template');

  // 1. Prerequisites
  start(0);
  const prerequisites = checkPrerequisites();
  for (const item of prerequisites) {
    const line = `${item.name}: ${item.detail}`;
    if (item.ok) {
      p.log.success(line);
    } else if (item.required) {
      p.log.error(line);
    } else {
      p.log.warn(line);
    }
  }
  if (prerequisites.some((item) => item.required && !item.ok)) {
    throw new Stop('A required tool is missing.');
  }
  const canCheck = prerequisites.every((item) => item.ok);
  finish(0);

  // 2. Answers
  start(1);
  /**
   * @param {string} message
   * @param {string | undefined} given
   * @param {string} fallback
   * @param {(value: string) => string | undefined} validate
   * @returns {Promise<string>}
   */
  async function ask(message, given, fallback, validate) {
    const value = given ?? fallback;
    if (!interactive) {
      const error = validate(value);
      if (error) {
        throw new Stop(`${message} "${value}": ${error}`);
      }
      return value;
    }
    return answered(
      await p.text({
        message,
        initialValue: value,
        validate: (input) => validate(input ?? ''),
      }),
    );
  }
  const name = await ask('Project name', values.name ?? positionals[0], '', validateName);
  const owner = await ask(
    'GitHub owner of the new repository',
    values.owner,
    defaultOwner(gitConfig('github.user'), gitConfig('user.name')),
    validateOwner,
  );
  const packageName = await ask('Base Java package', values.package, defaultPackage(owner, name), validatePackage);
  const clientId = await ask('OIDC client id of the web app', values['client-id'], defaultClientId(name), validateClientId);
  const destination = resolve(
    await ask('Folder for the zip', values.dest, '.', (value) => (existsSync(value) ? undefined : 'This folder does not exist.')),
  );
  const zipPath = join(destination, `${name}.zip`);
  if (existsSync(zipPath)) {
    throw new Stop(`${zipPath} already exists.`);
  }
  finish(1);

  const workspace = mkdtempSync(join(tmpdir(), 'skeleton-apps-'));
  try {
    // 3. Template
    start(2);
    const spinner = p.spinner();
    spinner.start('Resolving the template version');
    const templateDir = join(workspace, 'template');
    let ref;
    try {
      ref = resolveRef(values.source, values.ref);
      spinner.message(`Fetching ${ref}`);
      mkdirSync(templateDir);
      fetchTemplate(values.source, ref, templateDir);
    } catch (error) {
      spinner.error('Could not fetch the template');
      throw error;
    }
    spinner.stop(`Template ${ref} fetched`);
    finish(2);

    // 4. Generate
    start(3);
    const answers = { name, owner, packageName, clientId };
    const template = readTree(templateDir);
    const generated = transformTree(template, answers);
    generated.set(PROVENANCE_PATH, { content: new TextEncoder().encode(provenancePage(ref)), mode: 0o644 });
    const projectDir = join(workspace, name);
    writeTree(projectDir, generated);
    p.log.success(`${generated.size} files generated (${template.size - generated.size} template-only files left out)`);
    finish(3);

    // 5. Verify
    start(4);
    let verified = 'skipped';
    const wantCheck = interactive
      ? canCheck && answered(await p.confirm({ message: 'Run ./gradlew check now? (several minutes)', initialValue: false }))
      : values.check;
    if (wantCheck) {
      if (!canCheck) {
        throw new Stop('The check needs Java 17+ and Docker Compose v2+.');
      }
      execFileSync('./gradlew', ['check'], { cwd: projectDir, stdio: 'inherit' });
      verified = 'passed';
      finish(4);
    } else {
      finish(4, 'skipped');
    }

    // 6. Review
    start(5);
    p.note(
      [
        `Project          ${name}`,
        `GitHub           ${owner}/${name}`,
        `Base package     ${packageName}`,
        `OIDC client id   ${clientId}`,
        `Template         ${ref}`,
        `Files            ${generated.size}`,
        `Build check      ${verified}`,
        `Archive          ${zipPath}`,
      ].join('\n'),
      'Review',
    );
    if (interactive && !answered(await p.confirm({ message: 'Create the repository and the archive?' }))) {
      throw new Stop('Cancelled, nothing was written.');
    }
    finish(5);

    // 7. Git
    start(6);
    initRepository(projectDir);
    p.log.success('Repository created on develop, with one commit: Init');
    finish(6);

    // 8. Zip
    start(7);
    const count = createArchive(projectDir, zipPath, name);
    finish(7);
    p.note(board(), 'Done');
    p.outro(`${zipPath} (${count} files). Unzip it, push it to github.com/${owner}/${name}, and apply the GitHub settings of docs/operations/ci-cd.md.`);
  } finally {
    try {
      rmSync(workspace, { recursive: true, force: true, maxRetries: 5, retryDelay: 200 });
    } catch {
      // The archive is already written: a leftover temporary folder is not a failure.
      p.log.warn(`Could not remove the temporary folder ${workspace}`);
    }
  }
}

try {
  await main();
} catch (error) {
  if (error instanceof Stop) {
    p.cancel(error.message);
  } else {
    // A failed command: show what it said rather than a stack trace.
    const stderr = /** @type {{ stderr?: unknown }} */ (error).stderr;
    const detail = typeof stderr === 'string' || stderr instanceof Uint8Array ? String(stderr).trim() : '';
    p.cancel(detail || (error instanceof Error ? error.message : String(error)));
  }
  process.exitCode = 1;
}
