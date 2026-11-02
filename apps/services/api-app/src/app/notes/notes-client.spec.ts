import { etag, problemFrom } from './notes-client';

describe('problemFrom', () => {
  it('keeps the detail and hint of an API problem', () => {
    expect(problemFrom({ type: 'urn:problem:note_not_found', title: 'Not Found', status: 404, detail: 'Note not found: 1', hint: 'Check the id.' })).toEqual({
      message: 'Note not found: 1',
      hint: 'Check the id.',
    });
  });

  it('keeps the error code, so that a screen can react to a stale version', () => {
    expect(problemFrom({ type: 'urn:problem:version_mismatch', title: 'Precondition Failed', status: 412, detail: 'Note 1 is at version 3, not 2', errorCode: 'VERSION_MISMATCH' }).code).toBe('VERSION_MISMATCH');
  });

  it('sends versions as the API writes its ETags', () => {
    expect(etag(3)).toBe('"3"');
  });

  it('gives a generic message when the error is not a problem', () => {
    expect(problemFrom(undefined).message).toBe('Something went wrong. Please try again.');
    expect(problemFrom('Bad Gateway').message).toBe('Something went wrong. Please try again.');
  });
});
