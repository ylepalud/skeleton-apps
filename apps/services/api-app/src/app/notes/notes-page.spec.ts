import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { FakeNotesClient } from './notes-client.fake';
import { NotesClient } from './notes-client';
import { NotesPage, TITLE_MAX_LENGTH } from './notes-page';

describe('NotesPage', () => {
  async function renderPage(client: FakeNotesClient): Promise<void> {
    await render(NotesPage, { providers: [{ provide: NotesClient, useValue: client }] });
    await screen.findByRole('heading', { name: /your notes/i });
  }

  it('lists the notes of the first page', async () => {
    await renderPage(new FakeNotesClient().withNotes(2));

    expect(await screen.findByRole('heading', { name: 'Note 2' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Note 1' })).toBeInTheDocument();
  });

  it('adds a note', async () => {
    const user = userEvent.setup();
    await renderPage(new FakeNotesClient());

    await user.type(screen.getByLabelText('Title'), 'Groceries');
    await user.type(screen.getByLabelText('Content'), 'Milk');
    await user.click(screen.getByRole('button', { name: 'Add note' }));

    expect(await screen.findByRole('heading', { name: 'Groceries' })).toBeInTheDocument();
  });

  it('refuses a blank title without calling the API', async () => {
    const user = userEvent.setup();
    const client = new FakeNotesClient();
    await renderPage(client);

    await user.type(screen.getByLabelText('Title'), '   ');
    await user.click(screen.getByRole('button', { name: 'Add note' }));

    expect(screen.getByText(/a title is required/i)).toBeInTheDocument();
    expect(client.notes).toHaveLength(0);
  });

  it('caps the title at the contract limit', async () => {
    await renderPage(new FakeNotesClient());

    expect(screen.getByLabelText('Title')).toHaveAttribute('maxlength', String(TITLE_MAX_LENGTH));
  });

  it('edits a note', async () => {
    const user = userEvent.setup();
    await renderPage(new FakeNotesClient().withNotes(1));

    await user.click(await screen.findByRole('button', { name: 'Edit' }));
    await user.clear(screen.getByLabelText('Title'));
    await user.type(screen.getByLabelText('Title'), 'Renamed');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('heading', { name: 'Renamed' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'New note' })).toBeInTheDocument();
  });

  it('asks for confirmation before deleting, and can keep the note', async () => {
    const user = userEvent.setup();
    const client = new FakeNotesClient().withNotes(1);
    await renderPage(client);

    await user.click(await screen.findByRole('button', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Keep' }));
    expect(client.notes).toHaveLength(1);

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));
    expect(await screen.findByText('No notes yet.')).toBeInTheDocument();
  });

  it('shows the problem and its hint when the API refuses', async () => {
    const user = userEvent.setup();
    const client = new FakeNotesClient();
    await renderPage(client);
    client.failWith = { message: 'A title must be non-blank', hint: 'Check the submitted value.' };

    await user.type(screen.getByLabelText('Title'), 'Valid');
    await user.click(screen.getByRole('button', { name: 'Add note' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('A title must be non-blank');
    expect(alert).toHaveTextContent('Check the submitted value.');
  });

  it('pages through the notes', async () => {
    const user = userEvent.setup();
    await renderPage(new FakeNotesClient().withNotes(11));

    await user.click(await screen.findByRole('button', { name: 'Next' }));

    expect(await screen.findByRole('heading', { name: 'Note 1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });
});
