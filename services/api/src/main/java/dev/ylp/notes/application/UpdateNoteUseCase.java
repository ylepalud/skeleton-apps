package dev.ylp.notes.application;

import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteErrorReason;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Title;
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
   * Edits and stores the note, then returns it. Raises {@link NoteErrorReason#NOTE_NOT_FOUND} if it
   * does not exist or belongs to another user.
   */
  @Transactional
  public Note execute(UpdateNoteCommand command) {
    Note note =
        noteRepository
            .findByIdAndOwner(command.noteId(), command.owner())
            .orElseThrow(() -> NoteErrorReason.noteNotFound(command.noteId()));
    note.edit(command.title(), command.content(), clock);
    noteRepository.save(note);
    return note;
  }

  /** The note to edit, who edits it, and its new title and content. */
  public record UpdateNoteCommand(OwnerId owner, NoteId noteId, Title title, Content content) {}
}
