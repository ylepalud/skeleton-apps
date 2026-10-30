package dev.ylp.notes.adapter.out.postgres;

import dev.ylp.notes.domain.Content;
import dev.ylp.notes.domain.Note;
import dev.ylp.notes.domain.NoteId;
import dev.ylp.notes.domain.OwnerId;
import dev.ylp.notes.domain.Title;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

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

  Note toDomain() {
    return Note.restore(
        new NoteId(id),
        new OwnerId(ownerId),
        new Title(title),
        new Content(content),
        createdAt,
        updatedAt);
  }
}
