package dev.ylp.notes.domain;

import java.util.Optional;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/** Stores notes. Every read is scoped to an owner: a note is never visible to another user. */
@SecondaryPort
public interface NoteRepository {

  /** Returns the note with this id if it belongs to this owner. */
  Optional<Note> findByIdAndOwner(NoteId id, OwnerId owner);

  /** Returns a page of this owner's notes, most recently updated first. */
  Page<Note> findByOwner(OwnerId owner, PageRequest request);

  /**
   * Inserts or updates the note, and returns it as stored, at its new version. Raises {@code
   * VERSION_MISMATCH} if the stored note changed since this one was read.
   */
  Note save(Note note);

  /**
   * Deletes the note. Raises {@code VERSION_MISMATCH} if the stored note changed since this one was
   * read.
   */
  void delete(Note note);
}
