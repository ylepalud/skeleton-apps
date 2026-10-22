package dev.ylp.notes.application;

import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Title;
import dev.ylp.shared.id.UUIDGenerator;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

/** Writes a new note for its owner. */
public class CreateNoteUseCase {

  private final NoteRepository noteRepository;
  private final UUIDGenerator uuidGenerator;
  private final Clock clock;

  /** Creates the use case. */
  public CreateNoteUseCase(
      NoteRepository noteRepository, UUIDGenerator uuidGenerator, Clock clock) {
    this.noteRepository = noteRepository;
    this.uuidGenerator = uuidGenerator;
    this.clock = clock;
  }

  /** Creates and stores the note, then returns it. */
  @Transactional
  public Note execute(CreateNoteCommand command) {
    Note note =
        Note.create(uuidGenerator, command.owner(), command.title(), command.content(), clock);
    noteRepository.save(note);
    return note;
  }

  /** The note to write. */
  public record CreateNoteCommand(OwnerId owner, Title title, Content content) {}
}
