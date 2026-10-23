// What the installer needs, and what the optional build check needs. Detected, never configured.
import { spawnSync } from 'node:child_process';

/**
 * @typedef {object} Prerequisite
 * @property {string} name
 * @property {boolean} ok
 * @property {string} detail version found, or why it is missing
 * @property {boolean} required whether the installer cannot run without it
 */

/**
 * @param {string} command
 * @param {string[]} args
 * @returns {string | undefined} stdout and stderr together (`java -version` prints to stderr), or nothing if the command failed
 */
function run(command, args) {
  const result = spawnSync(command, args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  return result.status === 0 ? `${result.stdout}${result.stderr}` : undefined;
}

/**
 * The Java major version in `java -version` output.
 * @param {string} output
 * @returns {number | undefined}
 */
export function javaMajor(output) {
  const match = /version "(\d+)(?:\.(\d+))?/.exec(output);
  if (!match) {
    return undefined;
  }
  // Java 8 and earlier report "1.8".
  return match[1] === '1' ? Number(match[2]) : Number(match[1]);
}

/**
 * The Docker Compose major version in `docker compose version` output.
 * @param {string} output
 * @returns {number | undefined}
 */
export function composeMajor(output) {
  const match = /version v?(\d+)\./.exec(output);
  return match ? Number(match[1]) : undefined;
}

/** @returns {Prerequisite[]} */
export function checkPrerequisites() {
  const git = run('git', ['--version']);
  const java = run('java', ['-version']);
  const major = java ? javaMajor(java) : undefined;
  const compose = run('docker', ['compose', 'version']);
  const composeVersion = compose ? composeMajor(compose) : undefined;
  return [
    { name: 'git', ok: git !== undefined, detail: git?.trim() ?? 'not found', required: true },
    {
      name: 'Java 17+',
      ok: major !== undefined && major >= 17,
      detail: major === undefined ? 'not found (only needed to verify the build)' : `Java ${major}`,
      required: false,
    },
    {
      name: 'Docker Compose v2+',
      ok: composeVersion !== undefined && composeVersion >= 2,
      detail: compose?.trim() ?? 'not found (only needed to verify the build)',
      required: false,
    },
  ];
}
