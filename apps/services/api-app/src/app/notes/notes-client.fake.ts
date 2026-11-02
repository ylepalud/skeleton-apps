import type { Note, NotePage, NoteRequest } from '../api/generated';
import { type NotesClient, type Outcome, type Problem, VERSION_MISMATCH } from './notes-client';

/**
 * An in-memory stand-in for the API, newest notes first, that can be told to fail. Like the API, it
 * versions the notes and refuses a change based on an older version.
 */
export class FakeNotesClient implements Pick<NotesClient, 'list' | 'create' | 'update' | 'remove'> {
  notes: Note[] = [];
  failWith: Problem | undefined;
  private sequence = 0;

  withNotes(count: number): this {
    for (let i = 1; i <= count; i++) {
      this.notes.unshift(this.note(`Note ${i}`, `Content ${i}`));
    }
    return this;
  }

  async list(page: number, size: number): Promise<Outcome<NotePage>> {
    return this.answer(() => {
      const items = this.notes.slice(page * size, page * size + size);
      return { items, page, size, totalItems: this.notes.length, hasNext: (page + 1) * size < this.notes.length };
    });
  }

  async create(request: NoteRequest): Promise<Outcome<Note>> {
    return this.answer(() => {
      const note = this.note(request.title, request.content);
      this.notes.unshift(note);
      return note;
    });
  }

  async update(noteId: string, version: number, request: NoteRequest): Promise<Outcome<Note>> {
    return this.answer(() => this.replace(noteId, request), this.staleUnless(noteId, version));
  }

  async remove(noteId: string, version: number): Promise<Outcome<void>> {
    return this.answer(() => {
      this.find(noteId);
      this.notes = this.notes.filter((n) => n.id !== noteId);
    }, this.staleUnless(noteId, version));
  }

  /** Changes a note as another device would, so that the versions read before are stale. */
  changeElsewhere(noteId: string, request: NoteRequest): void {
    this.replace(noteId, request);
  }

  private replace(noteId: string, request: NoteRequest): Note {
    const current = this.find(noteId);
    const note = { ...current, ...request, version: current.version + 1 };
    this.notes = [note, ...this.notes.filter((n) => n.id !== noteId)];
    return note;
  }

  private staleUnless(noteId: string, version: number): Problem | undefined {
    const current = this.notes.find((n) => n.id === noteId);
    return current && current.version !== version
      ? { message: `Note ${noteId} is at version ${current.version}, not ${version}`, code: VERSION_MISMATCH }
      : undefined;
  }

  private answer<T>(operation: () => T, refusal?: Problem): Outcome<T> {
    const problem = this.failWith ?? refusal;
    return problem ? { ok: false, problem } : { ok: true, data: operation() };
  }

  private find(noteId: string): Note {
    const note = this.notes.find((n) => n.id === noteId);
    if (!note) {
      throw new Error(`Unknown note ${noteId}`);
    }
    return note;
  }

  private note(title: string, content: string): Note {
    const now = new Date(Date.UTC(2026, 9, 22, 8, 0, this.sequence)).toISOString();
    return { id: `id-${++this.sequence}`, title, content, createdAt: now, updatedAt: now, version: 0 };
  }
}
