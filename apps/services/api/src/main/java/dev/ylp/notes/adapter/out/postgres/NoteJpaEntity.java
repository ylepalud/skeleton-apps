package dev.ylp.notes.adapter.out.postgres;

import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Title;
import dev.ylp.shared.concurrency.ExpectedVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Persistence model of a {@link Note}, mapped by hand to and from the domain. */
@Entity
@Table(schema = "notes", name = "notes")
// JPA instantiates the entity through the no-argument constructor before filling its fields.
@SuppressWarnings("NullAway.Init")
class NoteJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String ownerId;

  @Column(nullable = false, length = Title.MAX_LENGTH)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  // Null until stored: Spring Data then inserts rather than merges. Hibernate checks it in every
  // update and delete, and increments it on update.
  @Version private @Nullable Long version;

  protected NoteJpaEntity() {}

  private NoteJpaEntity(
      UUID id, String ownerId, String title, String content, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.title = title;
    this.content = content;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  static NoteJpaEntity fromDomain(Note note) {
    return new NoteJpaEntity(
        note.id().value(),
        note.owner().value(),
        note.title().value(),
        note.content().value(),
        note.createdAt(),
        note.updatedAt());
  }

  /**
   * Takes the changes of a note read at this entity's version. Raises {@code VERSION_MISMATCH} if
   * the note was read at another version.
   */
  NoteJpaEntity update(Note note) {
    requireVersion(note);
    this.title = note.title().value();
    this.content = note.content().value();
    this.updatedAt = note.updatedAt();
    return this;
  }

  /** Raises {@code VERSION_MISMATCH} if the note was read at another version than this one. */
  void requireVersion(Note note) {
    ExpectedVersion.of(note.version()).check(storedVersion(), "Note " + id);
  }

  Note toDomain() {
    return Note.restore(
        new NoteId(id),
        new OwnerId(ownerId),
        new Title(title),
        new Content(content),
        createdAt,
        updatedAt,
        storedVersion());
  }

  private long storedVersion() {
    return version == null ? 0 : version;
  }
}
