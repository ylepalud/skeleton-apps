// What the installer needs, and what the optional build check needs. Detected, never configured.
import { execFileSync } from 'node:child_process';

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
 * @returns {string | undefined} the combined output, or nothing if the command failed
 */
function run(command, args) {
  try {
    return execFileSync(command, args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  } catch (error) {
    // `java -version` prints to stderr; a failure leaves nothing usable.
    const stderr = /** @type {{ stderr?: string }} */ (error).stderr;
    return typeof stderr === 'string' && stderr.length > 0 ? stderr : undefined;
  }
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

/** @returns {Prerequisite[]} */
export function checkPrerequisites() {
  const git = run('git', ['--version']);
  const java = run('java', ['-version']);
  const major = java ? javaMajor(java) : undefined;
  const compose = run('docker', ['compose', 'version']);
  return [
    { name: 'git', ok: git !== undefined, detail: git?.trim() ?? 'not found', required: true },
    {
      name: 'Java 17+',
      ok: major !== undefined && major >= 17,
      detail: major === undefined ? 'not found (only needed to verify the build)' : `Java ${major}`,
      required: false,
    },
    {
      name: 'Docker Compose v2',
      ok: compose !== undefined && /v?2\./.test(compose),
      detail: compose?.trim() ?? 'not found (only needed to verify the build)',
      required: false,
    },
  ];
}
