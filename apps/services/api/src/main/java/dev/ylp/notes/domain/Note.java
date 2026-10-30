package dev.ylp.notes.domain;

import dev.ylp.shared.id.UUIDGenerator;
import java.time.Clock;
import java.time.Instant;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

/** A note written by a user. Aggregate root: it is only ever read or changed as a whole. */
@AggregateRoot
public final class Note {

  @Identity private final NoteId id;
  private final OwnerId owner;
  private Title title;
  private Content content;
  private final Instant createdAt;
  private Instant updatedAt;

  private Note(
      NoteId id,
      OwnerId owner,
      Title title,
      Content content,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.owner = owner;
    this.title = title;
    this.content = content;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Writes a new note for this owner, with a freshly generated id. */
  public static Note create(
      UUIDGenerator uuidGenerator, OwnerId owner, Title title, Content content, Clock clock) {
    Instant now = clock.instant();
    return new Note(new NoteId(uuidGenerator.generate()), owner, title, content, now, now);
  }

  /** Rebuilds a stored note exactly as it was saved. Reserved to persistence adapters. */
  public static Note restore(
      NoteId id,
      OwnerId owner,
      Title title,
      Content content,
      Instant createdAt,
      Instant updatedAt) {
    return new Note(id, owner, title, content, createdAt, updatedAt);
  }

  /** Replaces the title and content, and records when the note was last changed. */
  public void edit(Title title, Content content, Clock clock) {
    this.title = title;
    this.content = content;
    this.updatedAt = clock.instant();
  }

  /** Returns the identity of this note. */
  public NoteId id() {
    return id;
  }

  /** Returns the user owning this note. */
  public OwnerId owner() {
    return owner;
  }

  /** Returns the current title. */
  public Title title() {
    return title;
  }

  /** Returns the current content. */
  public Content content() {
    return content;
  }

  /** Returns when this note was written. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns when this note was last changed. */
  public Instant updatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Note note && id.equals(note.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
