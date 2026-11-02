import { TestBed } from '@angular/core/testing';
import { FakeNotesClient } from './notes-client.fake';
import { NotesClient, VERSION_MISMATCH } from './notes-client';
import { CHANGED_ELSEWHERE, NotesStore, PAGE_SIZE } from './notes-store';

describe('NotesStore', () => {
  let client: FakeNotesClient;
  let store: NotesStore;

  function setUp(existing: number): void {
    client = new FakeNotesClient().withNotes(existing);
    TestBed.configureTestingModule({ providers: [NotesStore, { provide: NotesClient, useValue: client }] });
    store = TestBed.inject(NotesStore);
  }

  it('loads a page of notes and knows whether others follow', async () => {
    setUp(PAGE_SIZE + 1);

    await store.load(0);

    expect(store.notes()).toHaveLength(PAGE_SIZE);
    expect(store.totalItems()).toBe(PAGE_SIZE + 1);
    expect(store.hasNext()).toBe(true);
    expect(store.hasPrevious()).toBe(false);
  });

  it('creates a note and shows the first page again', async () => {
    setUp(0);

    const saved = await store.save(undefined, { title: 'Ideas', content: '' });

    expect(saved).toBe(true);
    expect(store.notes().map((n) => n.title)).toEqual(['Ideas']);
  });

  it('updates a note in place', async () => {
    setUp(1);
    await store.load(0);
    const [note] = store.notes();

    await store.save(note, { title: 'Renamed', content: 'Body' });

    expect(store.notes()[0]).toMatchObject({ id: note.id, title: 'Renamed', content: 'Body', version: 1 });
  });

  it('refuses to overwrite a note changed elsewhere, and shows its latest version', async () => {
    setUp(1);
    await store.load(0);
    const [read] = store.notes();
    client.changeElsewhere(read.id, { title: 'From the phone', content: '' });

    expect(await store.save(read, { title: 'From the browser', content: '' })).toBe(false);

    expect(store.notes()[0]).toMatchObject({ title: 'From the phone', version: 1 });
    expect(store.problem()).toEqual({ message: CHANGED_ELSEWHERE, code: VERSION_MISMATCH });
  });

  it('keeps a note changed elsewhere instead of deleting it', async () => {
    setUp(1);
    await store.load(0);
    const [read] = store.notes();
    client.changeElsewhere(read.id, { title: 'From the phone', content: '' });

    expect(await store.remove(read)).toBe(false);

    expect(store.notes()).toHaveLength(1);
    expect(store.problem()?.message).toBe(CHANGED_ELSEWHERE);
  });

  it('deleting the only note of a page goes back to the previous page', async () => {
    setUp(PAGE_SIZE + 1);
    await store.load(1);
    const [last] = store.notes();

    expect(await store.remove(last)).toBe(true);

    expect(store.page()).toBe(0);
    expect(store.totalItems()).toBe(PAGE_SIZE);
  });

  it('keeps the problem when a call fails', async () => {
    setUp(1);
    client.failWith = { message: 'Note not found: 1', hint: 'Check the id.' };

    expect(await store.save(undefined, { title: 'x', content: '' })).toBe(false);
    expect(store.problem()).toEqual({ message: 'Note not found: 1', hint: 'Check the id.' });

    await store.load(0);
    expect(store.loading()).toBe(false);
    expect(store.problem()?.message).toBe('Note not found: 1');
  });
});
