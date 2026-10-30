import { computed, inject, Injectable, signal } from '@angular/core';
import type { Note, NoteRequest } from '../api/generated';
import { NotesClient, Problem } from './notes-client';

/** Number of notes shown per page. */
export const PAGE_SIZE = 10;

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

  /** Writes a new note, or replaces an existing one; returns whether it succeeded. */
  async save(noteId: string | undefined, request: NoteRequest): Promise<boolean> {
    const outcome = noteId === undefined ? await this.client.create(request) : await this.client.update(noteId, request);
    return this.reloadOr(outcome.ok ? undefined : outcome.problem, noteId === undefined ? 0 : this.page());
  }

  /** Deletes a note; returns whether it succeeded. */
  async remove(noteId: string): Promise<boolean> {
    const outcome = await this.client.remove(noteId);
    // Deleting the last note of a page shows the previous one.
    const page = this.notes().length === 1 && this.page() > 0 ? this.page() - 1 : this.page();
    return this.reloadOr(outcome.ok ? undefined : outcome.problem, page);
  }

  private async reloadOr(problem: Problem | undefined, page: number): Promise<boolean> {
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
