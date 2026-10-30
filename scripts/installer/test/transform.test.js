import { describe, expect, it } from 'vitest';
import {
  PROVENANCE_PATH,
  isBinary,
  isExcluded,
  provenancePage,
  transformPath,
  transformText,
  transformTree,
} from '../src/transform.js';

const answers = { name: 'demo-app', owner: 'example', packageName: 'dev.example.demo', clientId: 'demo-app-web' };
const encoder = new TextEncoder();
const decoder = new TextDecoder();

describe('isExcluded', () => {
  it.each([
    '.git/HEAD',
    'scripts/installer/src/cli.js',
    'package.json',
    'package-lock.json',
    'node_modules/fflate/package.json',
    '.github/workflows/installer.yml',
    'CHANGELOG.md',
    'docs/template/start-a-new-project.md',
    'docs/template/README.md',
  ])('leaves out %s', (path) => {
    expect(isExcluded(path)).toBe(true);
  });

  it.each(['apps/services/api-app/package.json', 'apps/bruno/package-lock.json', '.github/workflows/ci.yml', 'docs/CHANGELOG.md'])(
    'keeps %s',
    (path) => {
      expect(isExcluded(path)).toBe(false);
    },
  );
});

describe('isBinary', () => {
  it('detects a NUL byte', () => {
    expect(isBinary(new Uint8Array([0x50, 0x4b, 0x00, 0x03]))).toBe(true);
  });

  it('treats text as text', () => {
    expect(isBinary(encoder.encode('plain text'))).toBe(false);
  });
});

describe('transformPath', () => {
  it('moves the package folders', () => {
    expect(transformPath('apps/services/api/src/main/java/dev/ylp/notes/Note.java', answers)).toBe(
      'apps/services/api/src/main/java/dev/example/demo/notes/Note.java',
    );
    expect(transformPath('build-logic/src/main/kotlin/dev/ylp/buildlogic/Docker.kt', answers)).toBe(
      'build-logic/src/main/kotlin/dev/example/demo/buildlogic/Docker.kt',
    );
  });

  it('leaves other paths alone', () => {
    expect(transformPath('apps/services/api-app/src/main.ts', answers)).toBe('apps/services/api-app/src/main.ts');
    expect(transformPath('docs/dev/ylpx/file.md', answers)).toBe('docs/dev/ylpx/file.md');
  });
});

describe('transformText', () => {
  it('renames the GitHub repository before the bare name', () => {
    expect(transformText('README.md', 'ghcr.io/ylepalud/skeleton-apps/api and skeleton-apps', answers)).toBe(
      'ghcr.io/example/demo-app/api and demo-app',
    );
  });

  it('renames the package and its path', () => {
    const text = 'package dev.ylp.notes;\ngroup = "dev.ylp"\nsrc/main/java/dev/ylp/Application.java';
    expect(transformText('Note.java', text, answers)).toBe(
      'package dev.example.demo.notes;\ngroup = "dev.example.demo"\nsrc/main/java/dev/example/demo/Application.java',
    );
  });

  it('leaves look-alike packages alone', () => {
    expect(transformText('a.txt', 'dev.ylpx and mydev.ylp', answers)).toBe('dev.ylpx and mydev.ylp');
  });

  it('changes the client id of the web app, not the web module', () => {
    expect(transformText('apps/services/api-app/public/config.json', '{ "clientId": "api-app" }', answers)).toBe(
      '{ "clientId": "demo-app-web" }',
    );
    expect(
      transformText('compose.yaml', 'image: skeleton-apps/api-app:local\n  OIDC_CLIENT_ID: api-app', answers),
    ).toBe('image: demo-app/api-app:local\n  OIDC_CLIENT_ID: demo-app-web');
    expect(transformText('settings.gradle.kts', 'include(":apps:services:api-app")', answers)).toBe(
      'include(":apps:services:api-app")',
    );
  });

  it('drops template-only blocks and template issue references from Markdown', () => {
    const text = [
      'Intro.',
      '<!-- template-only:start -->',
      '## Start a new project',
      '<!-- template-only:end -->',
      'A possible evolution (issue #20). Installed app (it must be, issue #11): yes.',
      'Checks `check`<!-- template-only:start -->, plus `installer`<!-- template-only:end --> required.',
    ].join('\n');
    expect(transformText('README.md', text, answers)).toBe(
      'Intro.\nA possible evolution. Installed app (it must be): yes.\nChecks `check` required.',
    );
    expect(transformText('notes.yaml', 'see (issue #20)', answers)).toBe('see (issue #20)');
  });

  it('leaves no double blank line where a template-only block was', () => {
    const text = ['Intro.', '', '<!-- template-only:start -->', '## Start', '', '<!-- template-only:end -->', '', 'Next.'].join('\n');
    expect(transformText('README.md', text, answers)).toBe('Intro.\n\nNext.');
  });

  it('resets the release history', () => {
    expect(JSON.parse(transformText('.release-please-manifest.json', '{ ".": "0.4.2" }', answers))).toEqual({
      '.': '0.0.0',
    });
  });
});

describe('transformTree', () => {
  it('excludes, moves and rewrites, keeping binaries and modes', () => {
    const jar = new Uint8Array([0x50, 0x4b, 0x00, 0x64, 0x65, 0x76, 0x2e, 0x79, 0x6c, 0x70]);
    const files = new Map([
      ['package.json', { content: encoder.encode('{}'), mode: 0o644 }],
      ['gradlew', { content: encoder.encode('#!/bin/sh\n# skeleton-apps'), mode: 0o755 }],
      ['gradle/wrapper/gradle-wrapper.jar', { content: jar, mode: 0o644 }],
      ['apps/services/api/src/main/java/dev/ylp/App.java', { content: encoder.encode('package dev.ylp;'), mode: 0o644 }],
    ]);

    const result = transformTree(files, answers);

    expect([...result.keys()].sort()).toEqual([
      'apps/services/api/src/main/java/dev/example/demo/App.java',
      'gradle/wrapper/gradle-wrapper.jar',
      'gradlew',
    ]);
    expect(decoder.decode(result.get('gradlew')?.content)).toBe('#!/bin/sh\n# demo-app');
    expect(result.get('gradlew')?.mode).toBe(0o755);
    expect(result.get('gradle/wrapper/gradle-wrapper.jar')?.content).toBe(jar);
    expect(decoder.decode(result.get('apps/services/api/src/main/java/dev/example/demo/App.java')?.content)).toBe(
      'package dev.example.demo;',
    );
  });
});

describe('provenancePage', () => {
  it('names the template version, under the excluded template section', () => {
    expect(isExcluded(PROVENANCE_PATH)).toBe(true);
    expect(provenancePage('v0.2.0')).toContain('generated from [skeleton-apps](https://github.com/ylepalud/skeleton-apps) at `v0.2.0`');
  });
});
