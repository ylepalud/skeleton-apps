import { problemFrom } from './notes-client';

describe('problemFrom', () => {
  it('keeps the detail and hint of an API problem', () => {
    expect(problemFrom({ type: 'urn:problem:note_not_found', title: 'Not Found', status: 404, detail: 'Note not found: 1', hint: 'Check the id.' })).toEqual({
      message: 'Note not found: 1',
      hint: 'Check the id.',
    });
  });

  it('gives a generic message when the error is not a problem', () => {
    expect(problemFrom(undefined).message).toBe('Something went wrong. Please try again.');
    expect(problemFrom('Bad Gateway').message).toBe('Something went wrong. Please try again.');
  });
});
