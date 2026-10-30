import { Injectable } from '@angular/core';
import { createNote, deleteNote, listNotes, updateNote } from '../api/generated';
import type { Note, NotePage, NoteRequest, Problem as ProblemBody } from '../api/generated';

/** The outcome of a call: its data, or the problem the API answered with. */
export type Outcome<T> = { readonly ok: true; readonly data: T } | { readonly ok: false; readonly problem: Problem };

/** What the user is told about a failed call. */
export interface Problem {
  readonly message: string;
  readonly hint?: string;
}

/** The notes operations of the generated client, as a seam that tests replace. */
@Injectable({ providedIn: 'root' })
export class NotesClient {
  async list(page: number, size: number): Promise<Outcome<NotePage>> {
    return outcome(await listNotes({ query: { page, size } }));
  }

  async create(request: NoteRequest): Promise<Outcome<Note>> {
    return outcome(await createNote({ body: request }));
  }

  async update(noteId: string, request: NoteRequest): Promise<Outcome<Note>> {
    return outcome(await updateNote({ path: { noteId }, body: request }));
  }

  async remove(noteId: string): Promise<Outcome<void>> {
    const result = await deleteNote({ path: { noteId } });
    return result.error === undefined
      ? { ok: true, data: undefined }
      : { ok: false, problem: problemFrom(result.error) };
  }
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
    return { message: problem.detail, hint: problem.hint };
  }
  return { message: 'Something went wrong. Please try again.' };
}
