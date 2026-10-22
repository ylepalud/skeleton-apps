import type { Note, NotePage, NoteRequest } from '../api/generated';
import type { NotesClient, Outcome, Problem } from './notes-client';

/** An in-memory stand-in for the API, newest notes first, that can be told to fail. */
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

  async update(noteId: string, request: NoteRequest): Promise<Outcome<Note>> {
    return this.answer(() => {
      const note = { ...this.find(noteId), ...request };
      this.notes = [note, ...this.notes.filter((n) => n.id !== noteId)];
      return note;
    });
  }

  async remove(noteId: string): Promise<Outcome<void>> {
    return this.answer(() => {
      this.find(noteId);
      this.notes = this.notes.filter((n) => n.id !== noteId);
    });
  }

  private answer<T>(operation: () => T): Outcome<T> {
    return this.failWith ? { ok: false, problem: this.failWith } : { ok: true, data: operation() };
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
    return { id: `id-${++this.sequence}`, title, content, createdAt: now, updatedAt: now };
  }
}
