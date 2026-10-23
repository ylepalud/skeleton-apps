import { describe, expect, it } from 'vitest';
import {
  defaultClientId,
  defaultOwner,
  defaultPackage,
  validateClientId,
  validateName,
  validateOwner,
  validatePackage,
} from '../src/answers.js';

describe('validateName', () => {
  it.each(['demo', 'demo-app', 'a1-b2'])('accepts %s', (name) => {
    expect(validateName(name)).toBeUndefined();
  });

  it.each(['', 'Demo', 'demo_app', 'demo--app', '-demo', 'demo-', '1demo', 'a'.repeat(51)])('rejects "%s"', (name) => {
    expect(validateName(name)).toBeDefined();
  });
});

describe('validatePackage', () => {
  it.each(['dev.example', 'dev.example.demo', 'io.my_org.v2'])('accepts %s', (value) => {
    expect(validatePackage(value)).toBeUndefined();
  });

  it.each(['dev', 'Dev.example', 'dev..example', 'dev.example.', 'dev.1example', 'dev-example.demo'])(
    'rejects "%s"',
    (value) => {
      expect(validatePackage(value)).toBeDefined();
    },
  );

  it('rejects a Java keyword', () => {
    expect(validatePackage('dev.example.new')).toContain('"new"');
  });
});

describe('validateClientId', () => {
  it('accepts letters, digits, dots, dashes and underscores', () => {
    expect(validateClientId('demo-app_web.1')).toBeUndefined();
  });

  it.each(['', '-web', 'demo web'])('rejects "%s"', (value) => {
    expect(validateClientId(value)).toBeDefined();
  });
});

describe('validateOwner', () => {
  it.each(['example', 'my-org', 'a1'])('accepts %s', (value) => {
    expect(validateOwner(value)).toBeUndefined();
  });

  it.each(['', 'Example', 'my--org', '-org', 'org-', 'a'.repeat(40)])('rejects "%s"', (value) => {
    expect(validateOwner(value)).toBeDefined();
  });
});

describe('defaults', () => {
  it('derives the package from the owner and the name without dashes', () => {
    expect(defaultPackage('example', 'demo-app')).toBe('dev.example.demoapp');
  });

  it('keeps the derived package valid', () => {
    expect(defaultPackage('my-org', '1st-try')).toBe('dev.myorg.x1sttry');
    expect(defaultPackage('example', 'new')).toBe('dev.example.new_');
    expect(validatePackage(defaultPackage('my-org', 'new'))).toBeUndefined();
  });

  it('suffixes the client id with -web', () => {
    expect(defaultClientId('demo-app')).toBe('demo-app-web');
  });

  it('prefers github.user, then user.name, for the owner', () => {
    expect(defaultOwner('Example', 'Someone Else')).toBe('example');
    expect(defaultOwner(undefined, 'Jane Doe')).toBe('janedoe');
    expect(defaultOwner(undefined, undefined)).toBe('');
    expect(defaultOwner(undefined, '---')).toBe('');
  });
});
