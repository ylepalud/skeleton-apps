import { TestBed } from '@angular/core/testing';
import { FakeNotesClient } from './notes-client.fake';
import { NotesClient } from './notes-client';
import { NotesStore, PAGE_SIZE } from './notes-store';

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

    await store.save(note.id, { title: 'Renamed', content: 'Body' });

    expect(store.notes()[0]).toMatchObject({ id: note.id, title: 'Renamed', content: 'Body' });
  });

  it('deleting the only note of a page goes back to the previous page', async () => {
    setUp(PAGE_SIZE + 1);
    await store.load(1);
    const [last] = store.notes();

    expect(await store.remove(last.id)).toBe(true);

    expect(store.page()).toBe(0);
    expect(store.totalItems()).toBe(PAGE_SIZE);
  });

  it('keeps the problem when a call fails', async () => {
    setUp(1);
    client.failWith = { message: 'Note not found: 1', hint: 'Check the id.' };

    expect(await store.save('1', { title: 'x', content: '' })).toBe(false);
    expect(store.problem()).toEqual({ message: 'Note not found: 1', hint: 'Check the id.' });

    await store.load(0);
    expect(store.loading()).toBe(false);
    expect(store.problem()?.message).toBe('Note not found: 1');
  });
});
