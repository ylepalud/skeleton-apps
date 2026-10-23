import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { unzipSync } from 'fflate';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { createArchive, initRepository } from '../src/archive.js';

describe('initRepository and createArchive', () => {
  /** @type {string} */
  let dir;

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'installer-test-'));
  });

  afterEach(() => {
    rmSync(dir, { recursive: true, force: true });
  });

  it('commits everything once on develop and zips the repository without ignored files', () => {
    const project = join(dir, 'demo');
    mkdirSync(join(project, 'build'), { recursive: true });
    writeFileSync(join(project, '.gitignore'), 'build/\n');
    writeFileSync(join(project, 'gradlew'), '#!/bin/sh\n', { mode: 0o755 });
    writeFileSync(join(project, 'build', 'out.txt'), 'ignored');

    initRepository(project);
    const zipPath = join(dir, 'demo.zip');
    createArchive(project, zipPath, 'demo');

    const log = execFileSync('git', ['-C', project, 'log', '--format=%s', 'develop'], { encoding: 'utf8' });
    expect(log.trim()).toBe('Init');
    const zip = unzipSync(readFileSync(zipPath));
    expect(Object.keys(zip)).toContain('demo/gradlew');
    expect(Object.keys(zip)).toContain('demo/.git/HEAD');
    expect(Object.keys(zip)).not.toContain('demo/build/out.txt');
  });
});
