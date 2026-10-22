package dev.ylp.notes.application;

import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteRepository;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Page;
import dev.ylp.notes.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Lists the notes of their owner, most recently updated first. */
public class ListNotesUseCase {

  private final NoteRepository noteRepository;

  /** Creates the use case. */
  public ListNotesUseCase(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  /** Returns the requested page of the owner's notes. */
  @Transactional(readOnly = true)
  public Page<Note> execute(ListNotesCommand command) {
    return noteRepository.findByOwner(command.owner(), command.request());
  }

  /** Whose notes to list, and which page. */
  public record ListNotesCommand(OwnerId owner, PageRequest request) {}
}
