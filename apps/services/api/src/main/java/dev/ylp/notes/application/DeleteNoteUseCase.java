package dev.ylp.notes.application;

import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.shared.concurrency.ExpectedVersion;
import org.springframework.transaction.annotation.Transactional;

/** Deletes one note of its owner. */
public class DeleteNoteUseCase {

  private final NoteRepository noteRepository;

  /** Creates the use case. */
  public DeleteNoteUseCase(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  /**
   * Deletes the note. Raises {@link NoteErrorReason#NOTE_NOT_FOUND} if it does not exist or belongs
   * to another user.
   *
   * <p>Conflict policy: a note changed since the caller read it is not deleted. The deletion is
   * refused with {@code VERSION_MISMATCH}, so that the user sees the change before deciding again.
   */
  @Transactional
  public void execute(DeleteNoteCommand command) {
    Note note =
        noteRepository
            .findByIdAndOwner(command.noteId(), command.owner())
            .orElseThrow(() -> NoteErrorReason.noteNotFound(command.noteId()));
    note.requireVersion(command.expectedVersion());
    noteRepository.delete(note);
  }

  /** The note to delete, the version the deletion is based on, and who deletes it. */
  public record DeleteNoteCommand(OwnerId owner, NoteId noteId, ExpectedVersion expectedVersion) {}
}
