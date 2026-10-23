import { describe, expect, it } from 'vitest';
import { javaMajor } from '../src/prerequisites.js';

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
