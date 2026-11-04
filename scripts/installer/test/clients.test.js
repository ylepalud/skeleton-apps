import { describe, expect, it } from 'vitest';
import { holds, isLeftOut, parseClients, stripClientBlocks, validateClients } from '../src/clients.js';

/** @param {...import('../src/clients.js').Client} clients */
const only = (...clients) => new Set(clients);

describe('holds', () => {
  it('holds for a kept client, and native for the CLI or mobile', () => {
    expect(holds('web', only('web'))).toBe(true);
    expect(holds('cli', only('web'))).toBe(false);
    expect(holds('native', only('cli'))).toBe(true);
    expect(holds('native', only('mobile'))).toBe(true);
    expect(holds('native', only('web'))).toBe(false);
  });

  it('negates with an exclamation mark', () => {
    expect(holds('!web', only('cli'))).toBe(true);
    expect(holds('!web', only('web'))).toBe(false);
  });

  it('refuses an unknown name', () => {
    expect(() => holds('desktop', only('web'))).toThrow('Unknown client marker "desktop"');
  });
});

describe('the selection', () => {
  it('is read from a comma-separated list', () => {
    expect(parseClients('web, cli,,')).toEqual(only('web', 'cli'));
  });

  it('needs at least one known client', () => {
    expect(validateClients(only('web'))).toBeUndefined();
    expect(validateClients(new Set())).toBe('Choose at least one client.');
    expect(validateClients(new Set(['web', 'desktop']))).toBe('Unknown client: desktop. Choose among web, cli, mobile.');
  });
});

describe('isLeftOut', () => {
  it('leaves out the folders of the clients not kept, and shared without CLI nor mobile', () => {
    expect(isLeftOut('apps/clients/android/build.gradle.kts', only('web', 'cli'))).toBe(true);
    expect(isLeftOut('apps/clients/cli/build.gradle.kts', only('web', 'cli'))).toBe(false);
    expect(isLeftOut('apps/clients/shared/build.gradle.kts', only('cli'))).toBe(false);
    expect(isLeftOut('apps/clients/shared/build.gradle.kts', only('web'))).toBe(true);
    expect(isLeftOut('apps/clients/shared/src/iosMain/kotlin/A.kt', only('cli'))).toBe(true);
    expect(isLeftOut('apps/services/api-app/package.json', only('mobile'))).toBe(true);
    expect(isLeftOut('apps/services/api/build.gradle.kts', only('mobile'))).toBe(false);
  });
});

describe('stripClientBlocks', () => {
  const text = [
    'all',
    '# client:web:start',
    'web',
    '# client:web:end',
    '// client:!web:start',
    '// no web',
    '// client:!web:end',
    '<!-- client:native:start -->',
    'native',
    '<!-- client:mobile:start -->',
    'mobile',
    '<!-- client:mobile:end -->',
    '<!-- client:native:end -->',
    'end',
  ].join('\n');

  it('keeps the blocks of the kept clients, without their markers', () => {
    expect(stripClientBlocks(text, only('web', 'cli'))).toBe(['all', 'web', 'native', 'end'].join('\n'));
  });

  it('drops the others, nested ones included', () => {
    expect(stripClientBlocks(text, only('mobile'))).toBe(['all', 'no web', 'native', 'mobile', 'end'].join('\n'));
    expect(stripClientBlocks(text, only('web'))).toBe(['all', 'web', 'end'].join('\n'));
  });

  it('uncomments a kept negated block, which the template keeps commented out', () => {
    const yaml = ['    ports:', '      # client:!web:start', '      # - "8088:8080"', '      # client:!web:end', '      - "8091:8081"'].join('\n');
    expect(stripClientBlocks(yaml, only('cli'))).toBe(['    ports:', '      - "8088:8080"', '      - "8091:8081"'].join('\n'));
    expect(stripClientBlocks(yaml, only('web'))).toBe(['    ports:', '      - "8091:8081"'].join('\n'));
  });

  it('refuses a block left open or closed twice', () => {
    expect(() => stripClientBlocks('# client:web:start\nweb', only('web'), 'a.yml')).toThrow('a.yml:1: client:web:start without its end');
    expect(() => stripClientBlocks('# client:web:end', only('web'), 'a.yml')).toThrow('a.yml:1: client:web:end without its start');
    expect(() => stripClientBlocks('# client:web:start\n# client:cli:end', only('web'))).toThrow('client:cli:end without its start');
  });

  it('keeps or drops a single marked line, without its marker', () => {
    const lines = ['| a | b <!-- client:mobile --> |', '  - use-the-cli.md  # client:cli', 'Plain.', 'With web. // client:!web'].join('\n');
    expect(stripClientBlocks(lines, only('mobile'))).toBe(['| a | b |', 'Plain.', 'With web.'].join('\n'));
    expect(stripClientBlocks(lines, only('web', 'cli'))).toBe(['  - use-the-cli.md', 'Plain.'].join('\n'));
  });

  it('leaves text without markers untouched', () => {
    expect(stripClientBlocks('a client: b', only('web'))).toBe('a client: b');
  });
});
