import { Injectable } from '@angular/core';
import { createNote, deleteNote, listNotes, updateNote } from '../api/generated';
import type { Note, NotePage, NoteRequest, Problem as ProblemBody } from '../api/generated';

/** The outcome of a call: its data, or the problem the API answered with. */
export type Outcome<T> = { readonly ok: true; readonly data: T } | { readonly ok: false; readonly problem: Problem };

/** What the user is told about a failed call, and the API's code for it when it gave one. */
export interface Problem {
  readonly message: string;
  readonly hint?: string;
  readonly code?: string;
}

/** The code of a change refused because the resource changed since it was read. */
export const VERSION_MISMATCH = 'VERSION_MISMATCH';

/** The notes operations of the generated client, as a seam that tests replace. */
@Injectable({ providedIn: 'root' })
export class NotesClient {
  async list(page: number, size: number): Promise<Outcome<NotePage>> {
    return outcome(await listNotes({ query: { page, size } }));
  }

  async create(request: NoteRequest): Promise<Outcome<Note>> {
    return outcome(await createNote({ body: request }));
  }

  /** Replaces a note, as read at `version`: a note changed since is refused. */
  async update(noteId: string, version: number, request: NoteRequest): Promise<Outcome<Note>> {
    return outcome(await updateNote({ path: { noteId }, headers: { 'If-Match': etag(version) }, body: request }));
  }

  /** Deletes a note, as read at `version`: a note changed since is kept. */
  async remove(noteId: string, version: number): Promise<Outcome<void>> {
    const result = await deleteNote({ path: { noteId }, headers: { 'If-Match': etag(version) } });
    return result.error === undefined
      ? { ok: true, data: undefined }
      : { ok: false, problem: problemFrom(result.error) };
  }
}

/** The ETag of a version, as the API sends it. */
export function etag(version: number): string {
  return `"${version}"`;
}

function outcome<T>(result: { data?: T; error?: unknown }): Outcome<T> {
  if (result.error === undefined && result.data !== undefined) {
    return { ok: true, data: result.data };
  }
  return { ok: false, problem: problemFrom(result.error) };
}

/** Turns an API error body into a message; anything else gets a generic one. */
export function problemFrom(error: unknown): Problem {
  const problem = error as Partial<ProblemBody> | null | undefined;
  if (problem && typeof problem.detail === 'string') {
    return { message: problem.detail, hint: problem.hint, code: problem.errorCode };
  }
  return { message: 'Something went wrong. Please try again.' };
}
