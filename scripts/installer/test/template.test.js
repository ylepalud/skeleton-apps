import { describe, expect, it } from 'vitest';
import { latestTag } from '../src/template.js';

describe('latestTag', () => {
  it('picks the highest release by number, not by text', () => {
    const output = [
      'aaa\trefs/tags/v0.9.0',
      'bbb\trefs/tags/v0.10.0',
      'ccc\trefs/tags/v0.10.0^{}',
      'ddd\trefs/tags/v0.2.11',
    ].join('\n');
    expect(latestTag(output)).toBe('v0.10.0');
  });

  it('ignores tags that are not releases', () => {
    expect(latestTag('aaa\trefs/tags/v1.0.0-rc.1\nbbb\trefs/tags/nightly\nccc\trefs/tags/v0.1.0')).toBe('v0.1.0');
  });

  it('finds nothing without a release', () => {
    expect(latestTag('')).toBeUndefined();
  });
});
