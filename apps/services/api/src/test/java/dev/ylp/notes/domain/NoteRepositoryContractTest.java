package dev.ylp.notes.domain;

import static dev.ylp.notes.domain.NoteTestDataBuilder.CREATED_AT;
import static dev.ylp.notes.domain.NoteTestDataBuilder.aNote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ylp.shared.concurrency.VersionConflictReason;
import dev.ylp.shared.error.business.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The behaviour every {@link NoteRepository} adapter must honour, run against each adapter. */
public abstract class NoteRepositoryContractTest {

  /** Returns the adapter under test. */
  protected abstract NoteRepository repository();

  @Test
  @DisplayName("A saved note is found by its id and owner, unchanged")
  void savedNoteIsFoundByItsIdAndOwnerUnchanged() {
    // Given
    Note note = aNote().build();
    repository().save(note);

    // When
    var found = repository().findByIdAndOwner(note.id(), note.owner());

    // Then
    assertThat(found).hasValueSatisfying(stored -> assertSameState(stored, note));
  }

  @Test
  @DisplayName("A note is invisible to any other owner")
  void noteIsInvisibleToAnyOtherOwner() {
    // Given
    Note note = aNote().build();
    repository().save(note);

    // When
    var found = repository().findByIdAndOwner(note.id(), new OwnerId("someone-else"));

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("An unknown id is not found")
  void unknownIdIsNotFound() {
    // When
    var found = repository().findByIdAndOwner(new NoteId(UUID.randomUUID()), new OwnerId("anyone"));

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("An owner's notes are paged, most recently updated first")
  void ownersNotesArePagedMostRecentlyUpdatedFirst() {
    // Given
    OwnerId owner = new OwnerId("owner-" + UUID.randomUUID());
    Note oldest = aNote().withOwner(owner).withUpdatedAt(CREATED_AT.plusSeconds(1)).build();
    Note newest = aNote().withOwner(owner).withUpdatedAt(CREATED_AT.plusSeconds(3)).build();
    Note middle = aNote().withOwner(owner).withUpdatedAt(CREATED_AT.plusSeconds(2)).build();
    repository().save(oldest);
    repository().save(newest);
    repository().save(middle);
    repository().save(aNote().build());

    // When
    Page<Note> first = repository().findByOwner(owner, new PageRequest(0, 2));
    Page<Note> second = repository().findByOwner(owner, new PageRequest(1, 2));

    // Then
    assertThat(first.items()).extracting(Note::id).containsExactly(newest.id(), middle.id());
    assertThat(first.totalItems()).isEqualTo(3);
    assertThat(first.hasNext()).isTrue();
    assertThat(second.items()).extracting(Note::id).containsExactly(oldest.id());
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  @DisplayName("Saving an edited note stores its new state")
  void savingAnEditedNoteStoresItsNewState() {
    // Given
    Note note = aNote().build();
    repository().save(note);
    Clock later = Clock.fixed(CREATED_AT.plus(Duration.ofHours(1)), ZoneOffset.UTC);
    note.edit(new Title("Shopping"), new Content("Bread"), later);

    // When
    repository().save(note);

    // Then
    assertThat(repository().findByIdAndOwner(note.id(), note.owner()))
        .hasValueSatisfying(stored -> assertSameState(stored, note));
  }

  @Test
  @DisplayName("A deleted note is no longer found")
  void deletedNoteIsNoLongerFound() {
    // Given
    Note note = aNote().build();
    repository().save(note);

    // When
    repository().delete(note);

    // Then
    assertThat(repository().findByIdAndOwner(note.id(), note.owner())).isEmpty();
  }

  @Test
  @DisplayName("A note is stored at version 0, and each stored change adds one")
  void noteIsStoredAtVersion0AndEachStoredChangeAddsOne() {
    // Given
    Note note = aNote().build();

    // When
    Note created = repository().save(note);
    created.edit(new Title("Shopping"), new Content("Bread"), Clock.systemUTC());
    Note edited = repository().save(created);

    // Then
    assertThat(created.version()).isZero();
    assertThat(edited.version()).isEqualTo(1);
    assertThat(repository().findByIdAndOwner(note.id(), note.owner()))
        .hasValueSatisfying(stored -> assertThat(stored.version()).isEqualTo(1));
  }

  @Test
  @DisplayName("Saving a note read before a change stored since fails and keeps that change")
  void savingNoteReadBeforeChangeStoredSinceFailsAndKeepsThatChange() {
    // Given
    Note note = repository().save(aNote().build());
    Note first = repository().findByIdAndOwner(note.id(), note.owner()).orElseThrow();
    Note second = repository().findByIdAndOwner(note.id(), note.owner()).orElseThrow();
    first.edit(new Title("First"), new Content(""), Clock.systemUTC());
    repository().save(first);
    second.edit(new Title("Second"), new Content(""), Clock.systemUTC());

    // When / Then
    assertThatThrownBy(() -> repository().save(second))
        .isInstanceOfSatisfying(BusinessException.class, NoteRepositoryContractTest::assertStale);
    assertThat(repository().findByIdAndOwner(note.id(), note.owner()))
        .hasValueSatisfying(stored -> assertThat(stored.title()).isEqualTo(new Title("First")));
  }

  @Test
  @DisplayName("Deleting a note read before a change stored since fails and keeps it")
  void deletingNoteReadBeforeChangeStoredSinceFailsAndKeepsIt() {
    // Given
    Note note = repository().save(aNote().build());
    Note stale = repository().findByIdAndOwner(note.id(), note.owner()).orElseThrow();
    note.edit(new Title("Changed"), new Content(""), Clock.systemUTC());
    repository().save(note);

    // When / Then
    assertThatThrownBy(() -> repository().delete(stale))
        .isInstanceOfSatisfying(BusinessException.class, NoteRepositoryContractTest::assertStale);
    assertThat(repository().findByIdAndOwner(note.id(), note.owner())).isPresent();
  }

  private static void assertStale(BusinessException exception) {
    assertThat(exception.reason()).isEqualTo(VersionConflictReason.VERSION_MISMATCH);
  }

  // An entity's equals() compares ids only; a round trip must keep every field.
  private static void assertSameState(Note actual, Note expected) {
    assertThat(actual.id()).isEqualTo(expected.id());
    assertThat(actual.owner()).isEqualTo(expected.owner());
    assertThat(actual.title()).isEqualTo(expected.title());
    assertThat(actual.content()).isEqualTo(expected.content());
    assertThat(actual.createdAt()).isEqualTo(expected.createdAt());
    assertThat(actual.updatedAt()).isEqualTo(expected.updatedAt());
  }
}
