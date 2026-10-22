package dev.ylp.notes.domain;

import static dev.ylp.notes.domain.NoteTestDataBuilder.CREATED_AT;
import static dev.ylp.notes.domain.NoteTestDataBuilder.aNote;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteTest {

  private static final UUID GENERATED = UUID.fromString("7f3c1a2e-9b4d-4c6e-8a1f-2d3e4f5a6b7c");

  @Test
  @DisplayName("A new note gets a generated id and is created and updated now")
  void newNoteGetsGeneratedIdAndIsCreatedAndUpdatedNow() {
    // Given
    OwnerId owner = new OwnerId("ada");
    Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);

    // When
    Note note = Note.create(() -> GENERATED, owner, new Title("Ideas"), new Content(""), clock);

    // Then
    assertThat(note.id()).isEqualTo(new NoteId(GENERATED));
    assertThat(note.owner()).isEqualTo(owner);
    assertThat(note.title()).isEqualTo(new Title("Ideas"));
    assertThat(note.content()).isEqualTo(new Content(""));
    assertThat(note.createdAt()).isEqualTo(CREATED_AT);
    assertThat(note.updatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  @DisplayName("Editing a note replaces its text and moves only its update time")
  void editingNoteReplacesItsTextAndMovesOnlyItsUpdateTime() {
    // Given
    Note note = aNote().build();
    Instant later = CREATED_AT.plusSeconds(60);

    // When
    note.edit(new Title("Shopping"), new Content("Bread"), Clock.fixed(later, ZoneOffset.UTC));

    // Then
    assertThat(note.title()).isEqualTo(new Title("Shopping"));
    assertThat(note.content()).isEqualTo(new Content("Bread"));
    assertThat(note.updatedAt()).isEqualTo(later);
    assertThat(note.createdAt()).isEqualTo(CREATED_AT);
  }

  @Test
  @DisplayName("Two notes with the same id are equal, whatever their content")
  void twoNotesWithSameIdAreEqualWhateverTheirContent() {
    // Given
    NoteId id = new NoteId(UUID.randomUUID());
    Note first = aNote().withId(id).withTitle("First").build();
    Note second = aNote().withId(id).withTitle("Second").build();

    // When / Then
    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    assertThat(first).isNotEqualTo(aNote().withTitle("First").build());
  }
}
