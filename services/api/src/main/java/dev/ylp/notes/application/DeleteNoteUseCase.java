package dev.ylp.notes.application;

import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
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
   */
  @Transactional
  public void execute(DeleteNoteCommand command) {
    Note note =
        noteRepository
            .findByIdAndOwner(command.noteId(), command.owner())
            .orElseThrow(() -> NoteErrorReason.noteNotFound(command.noteId()));
    noteRepository.delete(note);
  }

  /** The note to delete, and who deletes it. */
  public record DeleteNoteCommand(OwnerId owner, NoteId noteId) {}
}
