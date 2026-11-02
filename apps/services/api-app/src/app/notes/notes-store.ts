import { computed, inject, Injectable, signal } from '@angular/core';
import type { Note, NoteRequest } from '../api/generated';
import { NotesClient, Problem, VERSION_MISMATCH } from './notes-client';

/** Number of notes shown per page. */
export const PAGE_SIZE = 10;

/** What the user reads when a note changed elsewhere between reading and saving it. */
export const CHANGED_ELSEWHERE = 'This note was changed elsewhere. It now shows the latest version.';

/** State of the notes screen: the current page of notes, and what is in progress or failed. */
@Injectable()
export class NotesStore {
  private readonly client = inject(NotesClient);

  private readonly state = signal({
    notes: [] as readonly Note[],
    page: 0,
    totalItems: 0,
    hasNext: false,
    loading: false,
    problem: undefined as Problem | undefined,
  });

  readonly notes = computed(() => this.state().notes);
  readonly page = computed(() => this.state().page);
  readonly totalItems = computed(() => this.state().totalItems);
  readonly hasNext = computed(() => this.state().hasNext);
  readonly hasPrevious = computed(() => this.state().page > 0);
  readonly loading = computed(() => this.state().loading);
  readonly problem = computed(() => this.state().problem);

  /** Shows the given page of notes. */
  async load(page: number): Promise<void> {
    this.patch({ loading: true, problem: undefined });
    const outcome = await this.client.list(page, PAGE_SIZE);
    if (outcome.ok) {
      const { items, page: current, totalItems, hasNext } = outcome.data;
      this.patch({ notes: items, page: current, totalItems, hasNext, loading: false });
    } else {
      this.patch({ loading: false, problem: outcome.problem });
    }
  }

  /**
   * Writes a new note, or replaces `note` as it was read; returns whether it succeeded. A note
   * changed elsewhere meanwhile is not overwritten: the page reloads to show its latest version.
   */
  async save(note: Note | undefined, request: NoteRequest): Promise<boolean> {
    const outcome = note === undefined ? await this.client.create(request) : await this.client.update(note.id, note.version, request);
    return this.reloadOr(outcome.ok ? undefined : outcome.problem, note === undefined ? 0 : this.page());
  }

  /** Deletes `note` as it was read; returns whether it succeeded. A note changed meanwhile is kept. */
  async remove(note: Note): Promise<boolean> {
    const outcome = await this.client.remove(note.id, note.version);
    // Deleting the last note of a page shows the previous one.
    const page = outcome.ok && this.notes().length === 1 && this.page() > 0 ? this.page() - 1 : this.page();
    return this.reloadOr(outcome.ok ? undefined : outcome.problem, page);
  }

  // The policy of notes for a concurrent change: refuse it, and show the latest version.
  private async reloadOr(problem: Problem | undefined, page: number): Promise<boolean> {
    if (problem?.code === VERSION_MISMATCH) {
      await this.load(page);
      this.patch({ problem: { message: CHANGED_ELSEWHERE, code: VERSION_MISMATCH } });
      return false;
    }
    if (problem) {
      this.patch({ problem });
      return false;
    }
    await this.load(page);
    return true;
  }

  private patch(change: Partial<ReturnType<typeof this.state>>): void {
    this.state.update((current) => ({ ...current, ...change }));
  }
}
