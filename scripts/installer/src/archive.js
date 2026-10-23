// Turns the generated folder into a Git repository with a single commit, then a zip.
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { zipSync } from 'fflate';
import { readTree } from './files.js';

/**
 * @param {string} directory
 * @param {string[]} args
 */
function git(directory, args) {
  return execFileSync('git', ['-C', directory, ...args], { encoding: 'utf8', stdio: 'pipe' }).trim();
}

/**
 * `git init` on `develop` and one `Init` commit of every file.
 * Uses the caller's git identity, or a neutral one when none is configured.
 * @param {string} directory
 */
export function initRepository(directory) {
  git(directory, ['init', '--quiet', '--initial-branch', 'develop']);
  git(directory, ['add', '--all']);
  /** @param {string} key */
  const config = (key) => {
    try {
      return git(directory, ['config', key]);
    } catch {
      return '';
    }
  };
  const name = config('user.name') || 'Project installer';
  const email = config('user.email') || 'installer@example.invalid';
  // No automatic gc or maintenance: a detached one would still write into .git while it is zipped and removed.
  git(directory, [
    '-c', `user.name=${name}`,
    '-c', `user.email=${email}`,
    '-c', 'gc.auto=0',
    '-c', 'maintenance.auto=false',
    'commit', '--quiet', '-m', 'Init',
  ]);
}

/**
 * Zips the committed files and the `.git` folder: build output and dependencies stay out.
 * @param {string} directory a repository created by initRepository
 * @param {string} zipPath
 * @param {string} rootFolder name of the folder inside the zip
 * @returns {number} the number of files in the archive
 */
export function createArchive(directory, zipPath, rootFolder) {
  const tracked = new Set(git(directory, ['ls-files', '-z']).split('\0').filter(Boolean));
  /** @type {Record<string, [Uint8Array, import('fflate').ZipOptions]>} */
  const entries = {};
  for (const [path, file] of readTree(directory)) {
    if (tracked.has(path) || path.startsWith('.git/')) {
      // os 3 (unix) with the mode in the high bits keeps gradlew executable once unzipped.
      entries[`${rootFolder}/${path}`] = [file.content, { os: 3, attrs: (0o100000 | file.mode) << 16 }];
    }
  }
  writeFileSync(zipPath, zipSync(entries, { level: 6 }));
  return Object.keys(entries).length;
}
