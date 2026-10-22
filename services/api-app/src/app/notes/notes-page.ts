import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import type { Note } from '../api/generated';
import { NotesStore } from './notes-store';

/** Limits of the contract, checked here first so the user gets immediate feedback. */
export const TITLE_MAX_LENGTH = 200;
export const CONTENT_MAX_LENGTH = 20_000;

/** The user's notes: a page of them, a form to write or edit one, and deletion. */
@Component({
  selector: 'app-notes-page',
  imports: [ReactiveFormsModule],
  providers: [NotesStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notes-page.html',
})
export class NotesPage implements OnInit {
  protected readonly store = inject(NotesStore);
  protected readonly titleMaxLength = TITLE_MAX_LENGTH;
  protected readonly contentMaxLength = CONTENT_MAX_LENGTH;

  /** The note being edited; none while writing a new one. */
  protected readonly editing = signal<Note | undefined>(undefined);
  /** The note awaiting deletion confirmation. */
  protected readonly confirmingDelete = signal<string | undefined>(undefined);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(TITLE_MAX_LENGTH), Validators.pattern(/\S/)]],
    content: ['', [Validators.maxLength(CONTENT_MAX_LENGTH)]],
  });

  ngOnInit(): void {
    void this.store.load(0);
  }

  protected edit(note: Note): void {
    this.editing.set(note);
    this.form.setValue({ title: note.title, content: note.content });
  }

  protected cancelEdit(): void {
    this.editing.set(undefined);
    this.form.reset();
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (await this.store.save(this.editing()?.id, this.form.getRawValue())) {
      this.cancelEdit();
    }
  }

  protected async confirmDelete(noteId: string): Promise<void> {
    this.confirmingDelete.set(undefined);
    if (await this.store.remove(noteId) && this.editing()?.id === noteId) {
      this.cancelEdit();
    }
  }
}
