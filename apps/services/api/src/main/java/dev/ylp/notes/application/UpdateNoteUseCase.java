package dev.ylp.notes.application;

import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Title;
import dev.ylp.shared.concurrency.ExpectedVersion;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

/** Replaces the title and content of one note of its owner. */
public class UpdateNoteUseCase {

  private final NoteRepository noteRepository;
  private final Clock clock;

  /** Creates the use case. */
  public UpdateNoteUseCase(NoteRepository noteRepository, Clock clock) {
    this.noteRepository = noteRepository;
    this.clock = clock;
  }

  /**
   * Edits and stores the note, then returns it at its new version. Raises {@link
   * NoteErrorReason#NOTE_NOT_FOUND} if it does not exist or belongs to another user.
   *
   * <p>Conflict policy: a note changed since the caller read it is not overwritten. The change is
   * refused with {@code VERSION_MISMATCH}, and the caller shows the latest version to the user, who
   * applies the change again.
   */
  @Transactional
  public Note execute(UpdateNoteCommand command) {
    Note note =
        noteRepository
            .findByIdAndOwner(command.noteId(), command.owner())
            .orElseThrow(() -> NoteErrorReason.noteNotFound(command.noteId()));
    note.requireVersion(command.expectedVersion());
    note.edit(command.title(), command.content(), clock);
    return noteRepository.save(note);
  }

  /** The note to edit, the version the change is based on, who edits it, and its new content. */
  public record UpdateNoteCommand(
      OwnerId owner,
      NoteId noteId,
      ExpectedVersion expectedVersion,
      Title title,
      Content content) {}
}
