package dev.ylp.notes.domain;

import java.time.Instant;
import java.util.UUID;

/** Builds a valid note by default; each test overrides only what it is about. */
public final class NoteTestDataBuilder {

  // Whole seconds: PostgreSQL stores microseconds, so a round trip keeps them intact.
  public static final Instant CREATED_AT = Instant.parse("2026-10-01T08:00:00Z");

  private NoteId id = new NoteId(UUID.randomUUID());
  private OwnerId owner = new OwnerId("owner-" + UUID.randomUUID());
  private Title title = new Title("Groceries");
  private Content content = new Content("Milk, eggs");
  private Instant createdAt = CREATED_AT;
  private Instant updatedAt = CREATED_AT;

  private NoteTestDataBuilder() {}

  /** Starts a valid note. */
  public static NoteTestDataBuilder aNote() {
    return new NoteTestDataBuilder();
  }

  /** Sets the id. */
  public NoteTestDataBuilder withId(NoteId id) {
    this.id = id;
    return this;
  }

  /** Sets the owner. */
  public NoteTestDataBuilder withOwner(OwnerId owner) {
    this.owner = owner;
    return this;
  }

  /** Sets the title. */
  public NoteTestDataBuilder withTitle(String title) {
    this.title = new Title(title);
    return this;
  }

  /** Sets the last update time. */
  public NoteTestDataBuilder withUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /** Builds the note. */
  public Note build() {
    return Note.restore(id, owner, title, content, createdAt, updatedAt);
  }
}
