import { describe, expect, it } from 'vitest';
import { composeMajor, javaMajor } from '../src/prerequisites.js';

describe('javaMajor', () => {
  it('reads a modern version', () => {
    expect(javaMajor('openjdk version "25.0.1" 2025-10-21 LTS')).toBe(25);
  });

  it('reads a version without minor part', () => {
    expect(javaMajor('openjdk version "17" 2021-09-14')).toBe(17);
  });

  it('reads the old 1.x scheme', () => {
    expect(javaMajor('java version "1.8.0_402"')).toBe(8);
  });

  it('finds nothing in unrelated output', () => {
    expect(javaMajor('command not found')).toBeUndefined();
  });
});

describe('composeMajor', () => {
  it.each([
    ['Docker Compose version v2.29.7', 2],
    ['Docker Compose version v5.3.1', 5],
    ['Docker Compose version 2.40.0', 2],
  ])('reads %s', (output, major) => {
    expect(composeMajor(output)).toBe(major);
  });

  it('finds nothing in unrelated output', () => {
    expect(composeMajor("docker: 'compose' is not a docker command.")).toBeUndefined();
  });
});
