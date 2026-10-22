package dev.ylp.notes.application;

import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import org.springframework.transaction.annotation.Transactional;

/** Reads one note of its owner. */
public class GetNoteUseCase {

  private final NoteRepository noteRepository;

  /** Creates the use case. */
  public GetNoteUseCase(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  /**
   * Returns the note, or raises {@link NoteErrorReason#NOTE_NOT_FOUND} if it does not exist or
   * belongs to another user.
   */
  @Transactional(readOnly = true)
  public Note execute(GetNoteCommand command) {
    return noteRepository
        .findByIdAndOwner(command.noteId(), command.owner())
        .orElseThrow(() -> NoteErrorReason.noteNotFound(command.noteId()));
  }

  /** The note to read, and who asks for it. */
  public record GetNoteCommand(OwnerId owner, NoteId noteId) {}
}
