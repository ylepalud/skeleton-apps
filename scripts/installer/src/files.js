// Reads and writes file trees, keeping the executable bit (gradlew must stay runnable).
import { mkdirSync, readdirSync, readFileSync, statSync, writeFileSync, chmodSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';

/**
 * @typedef {object} TreeFile
 * @property {Uint8Array} content
 * @property {number} mode unix permission bits
 */

/**
 * Every file under `root`, by relative path with forward slashes.
 * @param {string} root
 * @returns {Map<string, TreeFile>}
 */
export function readTree(root) {
  /** @type {Map<string, TreeFile>} */
  const files = new Map();
  /** @param {string} dir */
  const walk = (dir) => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const path = join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(path);
      } else if (entry.isFile()) {
        files.set(relative(root, path).split(sep).join('/'), {
          content: readFileSync(path),
          mode: statSync(path).mode & 0o777,
        });
      }
    }
  };
  walk(root);
  return files;
}

/**
 * Writes a tree under `root`.
 * @param {string} root
 * @param {Map<string, TreeFile>} files
 */
export function writeTree(root, files) {
  for (const [path, file] of files) {
    const target = join(root, ...path.split('/'));
    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(target, file.content);
    chmodSync(target, file.mode);
  }
}
