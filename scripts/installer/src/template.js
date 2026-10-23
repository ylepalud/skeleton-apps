// Fetches the template at a given version.
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';

/** Where the template lives. */
export const TEMPLATE_REPOSITORY = 'https://github.com/ylepalud/skeleton-apps.git';

/**
 * The newest release tag (vX.Y.Z) in `git ls-remote --tags` output, if any.
 * @param {string} lsRemote
 * @returns {string | undefined}
 */
export function latestTag(lsRemote) {
  const tags = lsRemote
    .split('\n')
    .map((line) => line.split('\t')[1] ?? '')
    .map((ref) => /^refs\/tags\/(v(\d+)\.(\d+)\.(\d+))$/.exec(ref))
    .filter((match) => match !== null)
    .map((match) => ({ tag: match[1], version: [Number(match[2]), Number(match[3]), Number(match[4])] }));
  tags.sort((a, b) => a.version[0] - b.version[0] || a.version[1] - b.version[1] || a.version[2] - b.version[2]);
  return tags.at(-1)?.tag;
}

/**
 * The version to fetch: the given one, else the newest release, else develop.
 * @param {string} source repository URL or local path
 * @param {string | undefined} ref
 */
export function resolveRef(source, ref) {
  if (ref) {
    return ref;
  }
  if (isLocal(source)) {
    return 'HEAD';
  }
  const output = execFileSync('git', ['ls-remote', '--tags', source], { encoding: 'utf8' });
  return latestTag(output) ?? 'develop';
}

/** @param {string} source */
function isLocal(source) {
  return existsSync(source);
}

/**
 * Copies the template's committed files at `ref` into `destination`.
 * A local source is read from its commits, so uncommitted changes never leak in.
 * @param {string} source repository URL or local path
 * @param {string} ref
 * @param {string} destination an empty folder
 */
export function fetchTemplate(source, ref, destination) {
  if (isLocal(source)) {
    const archive = execFileSync('git', ['-C', resolve(source), 'archive', '--format=tar', ref], {
      maxBuffer: 1024 * 1024 * 1024,
    });
    execFileSync('tar', ['-x', '-C', destination], { input: archive });
    return;
  }
  execFileSync('git', ['clone', '--quiet', '--depth', '1', '--branch', ref, source, destination], {
    stdio: 'pipe',
  });
}
